package com.xnfu.thermalshock.block.entity;

import com.xnfu.thermalshock.api.IThermalHandler;
import com.xnfu.thermalshock.client.gui.ThermalConverterMenu;
import com.xnfu.thermalshock.data.ColdSourceData;
import com.xnfu.thermalshock.data.HeatSourceData;
import com.xnfu.thermalshock.recipe.ConverterRecipeInput;
import com.xnfu.thermalshock.recipe.ThermalConverterRecipe;
import com.xnfu.thermalshock.registries.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
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

public class ThermalConverterBlockEntity extends BlockEntity implements MenuProvider, IThermalHandler {

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

        @Override
        public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
            super.deserializeNBT(provider, nbt);
            // [Fix] 存档迁移：如果读取旧存档导致 slot 变少，强制扩容到 7
            if (this.stacks.size() < 7) {
                NonNullList<ItemStack> oldStacks = this.stacks;
                this.stacks = NonNullList.withSize(7, ItemStack.EMPTY);
                for (int i = 0; i < oldStacks.size(); i++) {
                    this.stacks.set(i, oldStacks.get(i));
                }
                // 强制标记需要保存，确保下次存盘写入 Size: 7
                setChanged();
            }
        }
    };

    // === Fluid ===
    // 0: Input Tank, 1: Output Tank
    private final FluidTank inputTank = new FluidTank(64000) { // 稍微加大一点缓存
        @Override protected void onContentsChanged() { setChanged(); markRecipeDirty(); }
    };
    private final FluidTank outputTank = new FluidTank(64000) {
        @Override protected void onContentsChanged() { 
            setChanged(); 
            // 输出变化时也需要唤醒，以检查是否腾出了空间
            wakeUp(); 
        }
    };

    // 包装器：用于 Capability 暴露
    private final IFluidHandler fluidHandlerWrapper = new IFluidHandler() {
        @Override public int getTanks() { return 2; }
        @Override public @NotNull FluidStack getFluidInTank(int tank) { return tank == 0 ? inputTank.getFluid() : outputTank.getFluid(); }
        @Override public int getTankCapacity(int tank) { return 64000; }
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

    @Override
    public int getThermalRate() {
        return 0; // 转换器目前不直接作为热源提供热量
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
        
        int upgradeCount = be.getUpgradeCount();
        int speedMultiplier = 1 << upgradeCount; // 2^N (1, 2, 4, 8, 16)
        int maxBatch = (upgradeCount == 4) ? 4 : 1;

        // 4.1 检查热量条件
        if (!be.checkHeatCondition(recipe)) {
            // 热量不足 -> 进度回退并休眠
            if (be.processTime > 0) be.processTime = Math.max(0, be.processTime - 2 * speedMultiplier);
            be.isSleeping = true;
            return;
        }

        // 4.2 检查输出空间 (至少能放下一份)
        // 实际执行时我们会尝试放入 maxBatch 份，这里先做最基本的检查防止空转
        if (!be.checkOutputSpace(recipe, 1)) {
            be.isSleeping = true;
            return;
        }

        // 5. 执行加工
        be.isSleeping = false;
        be.totalProcessTime = recipe.getProcessTime();
        be.processTime += speedMultiplier;

        if (be.processTime >= be.totalProcessTime) {
            // 计算实际可执行的批次 (受限于原料和空间)
            int actualBatch = be.calculateMaxBatch(recipe, maxBatch);
            
            if (actualBatch > 0) {
                be.finishProcess(recipe, actualBatch);
            }
            
            be.processTime = 0;
            // 完成后重新检查
            be.markRecipeDirty();
        }
    }

    // =========================================================
    // 2. 辅助逻辑
    // =========================================================

    private int getUpgradeCount() {
        int count = 0;
        for (int i = 3; i <= 6; i++) {
            if (itemHandler.getStackInSlot(i).is(ThermalShockItems.OVERCLOCK_UPGRADE.get())) {
                count++;
            }
        }
        return count;
    }

    private int calculateMaxBatch(ThermalConverterRecipe recipe, int limit) {
        int maxPossible = limit;

        // 1. 检查原料限制
        if (!recipe.getItemInputs().isEmpty()) {
            var req = recipe.getItemInputs().get(0);
            ItemStack inStack = itemHandler.getStackInSlot(0);
            if (inStack.isEmpty() || !req.ingredient().test(inStack)) return 0;
            int itemLimit = inStack.getCount() / req.count();
            maxPossible = Math.min(maxPossible, itemLimit);
        }
        if (!recipe.getFluidInputs().isEmpty()) {
            var req = recipe.getFluidInputs().get(0);
            int fluidLimit = inputTank.getFluidAmount() / req.fluid().getAmount();
            maxPossible = Math.min(maxPossible, fluidLimit);
        }

        // 2. 检查输出空间限制 (模拟)
        // 简单起见，逐步递减检查，找到最大的可行值
        for (int i = maxPossible; i > 0; i--) {
            if (checkOutputSpace(recipe, i)) {
                return i;
            }
        }
        return 0;
    }

    private void recalculateHeat() {
        int heat = 0;
        for (Direction dir : Direction.values()) {
            BlockPos target = worldPosition.relative(dir);
            BlockState s = level.getBlockState(target);

            // A. 优先通过 Capability 获取热量 (解耦)
            IThermalHandler thermal = level.getCapability(IThermalHandler.INTERFACE, target, dir.getOpposite());
            if (thermal != null) {
                heat += thermal.getThermalRate();
                continue;
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

    private boolean checkOutputSpace(ThermalConverterRecipe recipe, int multiplier) {
        // 模拟插入物品
        if (!recipe.getItemOutputs().isEmpty()) {
            ItemStack out1 = recipe.getItemOutputs().get(0).stack().copy();
            out1.setCount(out1.getCount() * multiplier);
            if (!itemHandler.insertItem(1, out1, true).isEmpty()) return false;
        }
        if (recipe.getItemOutputs().size() > 1) {
            ItemStack out2 = recipe.getItemOutputs().get(1).stack().copy();
            out2.setCount(out2.getCount() * multiplier);
            if (!itemHandler.insertItem(2, out2, true).isEmpty()) return false;
        }
        // 模拟注入流体
        if (!recipe.getFluidOutputs().isEmpty()) {
            FluidStack outF = recipe.getFluidOutputs().get(0).fluid().copy();
            outF.setAmount(outF.getAmount() * multiplier);
            if (outputTank.fill(outF, IFluidHandler.FluidAction.SIMULATE) < outF.getAmount()) return false;
        }
        return true;
    }

    private void finishProcess(ThermalConverterRecipe recipe, int multiplier) {
        // 1. 消耗输入 (multiplier 次)
        // 注意：概率判定通常对每次操作独立。为了性能和批处理的“爽感”，这里我们假设批处理时必定成功消耗
        // 或者，我们可以做一个循环来精确模拟概率
        
        for (int i = 0; i < multiplier; i++) {
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
    }

    // =========================================================
    // 3. 事件 API (供外部调用)
    // =========================================================

    public void wakeUp() {
        this.isSleeping = false;
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
    
    public int getCachedHeatInput() {
        return cachedHeatInput;
    }

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