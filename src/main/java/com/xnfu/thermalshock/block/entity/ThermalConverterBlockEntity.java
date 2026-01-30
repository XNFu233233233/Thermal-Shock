package com.xnfu.thermalshock.block.entity;

import com.xnfu.thermalshock.client.gui.ThermalConverterMenu;
import com.xnfu.thermalshock.data.ColdSourceData;
import com.xnfu.thermalshock.data.HeatSourceData;
import com.xnfu.thermalshock.recipe.ConverterRecipeInput;
import com.xnfu.thermalshock.recipe.ThermalConverterRecipe;
import com.xnfu.thermalshock.registries.ThermalShockBlockEntities;
import com.xnfu.thermalshock.registries.ThermalShockDataMaps;
import com.xnfu.thermalshock.registries.ThermalShockItems;
import com.xnfu.thermalshock.registries.ThermalShockRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class ThermalConverterBlockEntity extends BlockEntity implements MenuProvider {

    // === Inventory ===
    // 0: Input, 1: Output Main, 2: Output Scrap
    // 3, 4, 5, 6: Overclock Upgrades
    private final ItemStackHandler itemHandler = new ItemStackHandler(7) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            markRecipeDirty(); // 库存变动 -> 可能改变配方匹配结果
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (slot >= 3) {
                return stack.is(ThermalShockItems.OVERCLOCK_UPGRADE.get());
            }
            return super.isItemValid(slot, stack);
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot >= 3 ? 1 : 64;
        }
    };

    // === Fluid ===
    // 0: Input Tank, 1: Output Tank
    private final FluidTank inputTank = new FluidTank(4000) { // 稍微加大一点缓存
        @Override protected void onContentsChanged() { setChanged(); markRecipeDirty(); }
    };
    private final FluidTank outputTank = new FluidTank(4000) {
        @Override protected void onContentsChanged() { setChanged(); markRecipeDirty(); }
    };

    // 包装器：用于 Capability 暴露
    private final IFluidHandler fluidHandlerWrapper = new IFluidHandler() {
        @Override public int getTanks() { return 2; }
        @Override public @NotNull FluidStack getFluidInTank(int tank) { return tank == 0 ? inputTank.getFluid() : outputTank.getFluid(); }
        @Override public int getTankCapacity(int tank) { return 4000; }
        @Override public boolean isFluidValid(int tank, @NotNull FluidStack stack) { return tank == 0 && inputTank.isFluidValid(stack); }
        @Override public int fill(FluidStack resource, FluidAction action) { return inputTank.fill(resource, action); }
        @Override public @NotNull FluidStack drain(FluidStack resource, FluidAction action) { return outputTank.drain(resource, action); }
        @Override public @NotNull FluidStack drain(int maxDrain, FluidAction action) { return outputTank.drain(maxDrain, action); }
    };

    // === Logic State ===
    private int processTime = 0;
    private int totalProcessTime = 0;
    private boolean isSleeping = true; // 默认休眠

    // === Caches ===
    private int cachedHeatInput = 0;
    private boolean heatDirty = true;   // 热量是否需要重算
    private boolean recipeDirty = true; // 配方是否需要重查
    private RecipeHolder<ThermalConverterRecipe> cachedRecipe = null;

    // === GUI Sync ===
    private final ContainerData data = new ContainerData() {
        @Override public int get(int index) {
            return switch (index) {
                case 0 -> processTime;
                case 1 -> totalProcessTime;
                case 2 -> cachedHeatInput;
                default -> 0;
            };
        }
        @Override public void set(int index, int value) {
            if (index == 0) processTime = value;
            else if (index == 1) totalProcessTime = value;
            else if (index == 2) cachedHeatInput = value;
        }
        @Override public int getCount() { return 3; }
    };

    public ThermalConverterBlockEntity(BlockPos pos, BlockState blockState) {
        super(ThermalShockBlockEntities.THERMAL_CONVERTER_BE.get(), pos, blockState);
    }

    // =========================================================
    // 1. 核心循环 (Tick Logic)
    // =========================================================

    public static void tick(Level level, BlockPos pos, BlockState state, ThermalConverterBlockEntity be) {
        if (level.isClientSide) return;

        // 1. 如果正在休眠且没有脏标记，直接跳过 (极致性能)
        if (be.isSleeping && !be.heatDirty && !be.recipeDirty) {
            return;
        }

        // 2. 处理热量更新 (被动触发)
        if (be.heatDirty) {
            be.recalculateHeat(); // 算完后 heatDirty = false
        }

        // 3. 处理配方缓存 (被动触发)
        if (be.recipeDirty) {
            be.updateCachedRecipe(); // 算完后 recipeDirty = false
        }

        // 安全检查：如果没有配方（可能是更新后变null，也可能是本来就是null但被热量更新唤醒），直接睡去
        if (be.cachedRecipe == null) {
            be.isSleeping = true;
            be.processTime = 0;
            return;
        }

        // 4. 运行检查
        ThermalConverterRecipe recipe = be.cachedRecipe.value();

        // 4.1 检查热量条件
        if (!be.checkHeatCondition(recipe)) {
            // 热量不足 -> 进度回退并休眠 (等待 neighborChanged 唤醒)
            if (be.processTime > 0) be.processTime = Math.max(0, be.processTime - 2);
            be.isSleeping = true;
            return;
        }

        // 4.2 检查输出空间 (模拟)
        if (!be.checkOutputSpace(recipe)) {
            // 空间不足 -> 休眠 (等待库存变动唤醒)
            be.isSleeping = true;
            return;
        }

        // 5. 执行加工
        be.isSleeping = false; // 正在工作，标记为醒着
        be.totalProcessTime = recipe.getProcessTime();
        be.processTime++;

        if (be.processTime >= be.totalProcessTime) {
            be.finishProcess(recipe);
            be.processTime = 0;
            // 完成一次后，需要重新检查输入是否足够 (可能刚才把最后一份原料用完了)
            be.markRecipeDirty();
        }
    }

    // =========================================================
    // 2. 辅助逻辑
    // =========================================================

    private void recalculateHeat() {
        int heat = 0;
        for (Direction dir : Direction.values()) {
            BlockPos target = worldPosition.relative(dir);
            BlockState s = level.getBlockState(target);

            // A. 主动热源/冷源 (机器)
            // 优先检查 BE，因为机器可能有开关状态
            BlockEntity neighborBe = level.getBlockEntity(target);
            if (neighborBe instanceof ThermalSourceBlockEntity source) {
                // 只有开启状态才有热量 (getCurrentHeatOutput 内部已经处理了逻辑)
                heat += source.getCurrentHeatOutput();
                continue; // 既然是机器，就不查 DataMap 了，避免重复
            }

            // B. 被动热源 (DataMap)
            var holder = s.getBlockHolder();
            HeatSourceData hData = holder.getData(ThermalShockDataMaps.HEAT_SOURCE_PROPERTY);
            if (hData != null) {
                heat += hData.heatPerTick();
                continue;
            }

            ColdSourceData cData = holder.getData(ThermalShockDataMaps.COLD_SOURCE_PROPERTY);
            if (cData != null) {
                heat -= cData.coolingPerTick(); // 冷源减热 (coolingPerTick 是正数)
                continue;
            }

            // C. 特殊硬编码 (水) - 也可以写进 DataMap，但为了保险保留硬编码
            if (s.getFluidState().is(net.minecraft.tags.FluidTags.WATER)) {
                // 水大概提供微弱冷却? 这里暂设为 0 或者 -5
                // 如果 DataMap 没覆盖到，可以在这里补充
            }
        }
        this.cachedHeatInput = heat;
        this.heatDirty = false;
    }

    private void updateCachedRecipe() {
        ConverterRecipeInput input = new ConverterRecipeInput(
                itemHandler.getStackInSlot(0),
                inputTank.getFluid()
        );

        // 如果输入为空，直接清空配方
        if (input.item().isEmpty() && input.fluid().isEmpty()) {
            this.cachedRecipe = null;
            this.recipeDirty = false;
            return;
        }

        // 只有当 当前缓存的配方 不再匹配时，才去查表
        if (this.cachedRecipe != null && this.cachedRecipe.value().matches(input, level)) {
            this.recipeDirty = false;
            return;
        }

        Optional<RecipeHolder<ThermalConverterRecipe>> opt = level.getRecipeManager()
                .getRecipeFor(ThermalShockRecipes.CONVERTER_TYPE.get(), input, level);

        this.cachedRecipe = opt.orElse(null);
        this.recipeDirty = false;
    }

    private boolean checkHeatCondition(ThermalConverterRecipe recipe) {
        // 熔融: InputHeat >= Min (e.g. 1000 >= 500)
        if (recipe.getMinHeat() != Integer.MIN_VALUE && cachedHeatInput < recipe.getMinHeat()) return false;
        // 冷冻: InputHeat <= Max (e.g. -200 <= -100)
        if (recipe.getMaxHeat() != Integer.MAX_VALUE && cachedHeatInput > recipe.getMaxHeat()) return false;
        return true;
    }

    private boolean checkOutputSpace(ThermalConverterRecipe recipe) {
        // 模拟插入物品
        if (!recipe.getItemOutputs().isEmpty()) {
            ItemStack out1 = recipe.getItemOutputs().get(0).stack();
            if (!itemHandler.insertItem(1, out1, true).isEmpty()) return false;
        }
        if (recipe.getItemOutputs().size() > 1) {
            ItemStack out2 = recipe.getItemOutputs().get(1).stack();
            if (!itemHandler.insertItem(2, out2, true).isEmpty()) return false;
        }
        // 模拟注入流体
        if (!recipe.getFluidOutputs().isEmpty()) {
            FluidStack outF = recipe.getFluidOutputs().get(0).fluid();
            if (outputTank.fill(outF, IFluidHandler.FluidAction.SIMULATE) < outF.getAmount()) return false;
        }
        return true;
    }

    private void finishProcess(ThermalConverterRecipe recipe) {
        // 1. 消耗输入
        if (!recipe.getItemInputs().isEmpty()) {
            var inRule = recipe.getItemInputs().get(0);
            if (level.random.nextFloat() < inRule.consumeChance()) {
                itemHandler.extractItem(0, inRule.count(), false);
            }
        }
        if (!recipe.getFluidInputs().isEmpty()) {
            var inRule = recipe.getFluidInputs().get(0);
            if (level.random.nextFloat() < inRule.consumeChance()) {
                inputTank.drain(inRule.fluid().getAmount(), IFluidHandler.FluidAction.EXECUTE);
            }
        }

        // 2. 产生输出
        if (!recipe.getItemOutputs().isEmpty()) {
            var outRule = recipe.getItemOutputs().get(0);
            if (level.random.nextFloat() < outRule.chance()) {
                itemHandler.insertItem(1, outRule.stack().copy(), false);
            }
        }
        if (recipe.getItemOutputs().size() > 1) {
            var outRule = recipe.getItemOutputs().get(1);
            if (level.random.nextFloat() < outRule.chance()) {
                itemHandler.insertItem(2, outRule.stack().copy(), false);
            }
        }
        if (!recipe.getFluidOutputs().isEmpty()) {
            var outRule = recipe.getFluidOutputs().get(0);
            if (level.random.nextFloat() < outRule.chance()) {
                outputTank.fill(outRule.fluid().copy(), IFluidHandler.FluidAction.EXECUTE);
            }
        }
    }

    // =========================================================
    // 3. 事件 API (供外部调用)
    // =========================================================

    public void wakeUp() {
        if (this.isSleeping) {
            this.isSleeping = false;
            // 不调用 setChanged()，因为 wakeUp 通常伴随着其他数据变化已经 setChanged 了
        }
    }

    public void markHeatDirty() {
        this.heatDirty = true;
        wakeUp(); // 热量变化可能满足配方条件，唤醒检查
    }

    public void markRecipeDirty() {
        this.recipeDirty = true;
        wakeUp(); // 库存变化必须唤醒
    }

    // =========================================================
    // 4. 标准 Getters & NBT
    // =========================================================

    public IItemHandler getItemHandler() { return itemHandler; }
    public IFluidHandler getFluidHandler() { return fluidHandlerWrapper; }
    public FluidTank getInputTank() { return inputTank; }
    public FluidTank getOutputTank() { return outputTank; }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.thermalshock.thermal_converter");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ThermalConverterMenu(id, inventory, this, this.data);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider r) {
        super.saveAdditional(tag, r);
        tag.put("Inventory", itemHandler.serializeNBT(r));
        tag.put("InputTank", inputTank.writeToNBT(r, new CompoundTag()));
        tag.put("OutputTank", outputTank.writeToNBT(r, new CompoundTag()));
        tag.putInt("ProcessTime", processTime);
        tag.putInt("TotalTime", totalProcessTime);
        tag.putInt("HeatCache", cachedHeatInput);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider r) {
        super.loadAdditional(tag, r);
        itemHandler.deserializeNBT(r, tag.getCompound("Inventory"));
        inputTank.readFromNBT(r, tag.getCompound("InputTank"));
        outputTank.readFromNBT(r, tag.getCompound("OutputTank"));
        processTime = tag.getInt("ProcessTime");
        totalProcessTime = tag.getInt("TotalTime");
        cachedHeatInput = tag.getInt("HeatCache");

        // 载入时强制标记脏，以便下一 tick 刷新状态
        this.heatDirty = true;
        this.recipeDirty = true;
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider r) { return saveWithoutMetadata(r); }
    @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider r) { loadAdditional(pkt.getTag(), r); }
}