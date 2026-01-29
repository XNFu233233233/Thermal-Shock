package com.xnfu.thermalshock.block.entity;

import com.xnfu.thermalshock.block.ThermalSourceBlock;
import com.xnfu.thermalshock.data.ColdSourceData;
import com.xnfu.thermalshock.data.HeatSourceData;
import com.xnfu.thermalshock.recipe.ConverterRecipeInput;
import com.xnfu.thermalshock.recipe.ThermalConverterRecipe;
import com.xnfu.thermalshock.registries.ThermalShockBlockEntities;
import com.xnfu.thermalshock.registries.ThermalShockDataMaps;
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
    private final ItemStackHandler itemHandler = new ItemStackHandler(3) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            wakeUp();
        }
    };

    // === Fluid ===
    // 0: Input Tank, 1: Output Tank
    private final FluidTank inputTank = new FluidTank(64000) {
        @Override protected void onContentsChanged() { setChanged(); wakeUp(); }
    };
    private final FluidTank outputTank = new FluidTank(64000) {
        @Override protected void onContentsChanged() { setChanged(); wakeUp(); }
    };

    // 包装器，用于 Capability 暴露 (Input tank 只进，Output tank 只出)
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
    private boolean isSleeping = true;

    // === Heat Cache ===
    private int cachedHeatInput = 0;
    private boolean heatCacheDirty = true;

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

    public static void tick(Level level, BlockPos pos, BlockState state, ThermalConverterBlockEntity be) {
        if (level.isClientSide) return;

        // 1. 热量缓存更新 (事件驱动)
        if (be.heatCacheDirty) {
            be.recalculateHeat();
        }

        // 2. 休眠检查
        if (be.isSleeping) {
            // 如果有热量输入，可能满足“纯热量配方”，尝试唤醒一次检查
            if (be.cachedHeatInput != 0 && be.processTime == 0) {
                be.isSleeping = false;
            } else {
                return;
            }
        }

        // 3. 配方处理逻辑
        boolean isWorking = false;
        ThermalConverterRecipe recipe = be.findRecipe();

        if (recipe != null && be.canProcess(recipe)) {
            isWorking = true;
            be.totalProcessTime = recipe.getProcessTime();
            be.processTime++;

            if (be.processTime >= be.totalProcessTime) {
                be.finishProcess(recipe);
                be.processTime = 0;
            }
        } else {
            be.processTime = 0;
        }

        // 4. 自动休眠
        if (!isWorking && be.processTime == 0) {
            be.isSleeping = true;
        }
    }

    private void recalculateHeat() {
        int heat = 0;
        for (Direction dir : Direction.values()) {
            BlockPos target = worldPosition.relative(dir);
            BlockState s = level.getBlockState(target);

            // A. 主动热源/冷源
            if (s.getBlock() instanceof ThermalSourceBlock && s.getValue(ThermalSourceBlock.LIT)) {
                BlockEntity neighborBe = level.getBlockEntity(target);
                if (neighborBe instanceof ThermalSourceBlockEntity source) {
                    heat += source.getCurrentHeatOutput();
                }
            }
            // B. 被动热源 (DataMap)
            else {
                var holder = s.getBlockHolder();
                HeatSourceData hData = holder.getData(ThermalShockDataMaps.HEAT_SOURCE_PROPERTY);
                if (hData != null) heat += hData.heatPerTick();

                ColdSourceData cData = holder.getData(ThermalShockDataMaps.COLD_SOURCE_PROPERTY);
                if (cData != null) heat -= cData.coolingPerTick(); // 冷源减热
            }
        }
        this.cachedHeatInput = heat;
        this.heatCacheDirty = false;
        // 热量变化可能满足配方条件，唤醒
        wakeUp();
    }

    private ThermalConverterRecipe findRecipe() {
        ConverterRecipeInput input = new ConverterRecipeInput(
                itemHandler.getStackInSlot(0),
                inputTank.getFluid()
        );
        Optional<RecipeHolder<ThermalConverterRecipe>> opt = level.getRecipeManager()
                .getRecipeFor(ThermalShockRecipes.CONVERTER_TYPE.get(), input, level);
        return opt.map(RecipeHolder::value).orElse(null);
    }

    private boolean canProcess(ThermalConverterRecipe recipe) {
        // 1. 温度检查
        // 熔融: InputHeat >= Min (e.g. 1000 >= 500)
        if (recipe.getMinHeat() != Integer.MIN_VALUE && cachedHeatInput < recipe.getMinHeat()) return false;
        // 冷冻: InputHeat <= Max (e.g. -200 <= -100)
        if (recipe.getMaxHeat() != Integer.MAX_VALUE && cachedHeatInput > recipe.getMaxHeat()) return false;

        // 2. 输出空间检查 (模拟产出)
        // 简单起见，只检查是否有空位或物品相同且未满堆叠
        if (!recipe.getItemOutputs().isEmpty()) {
            ItemStack out1 = recipe.getItemOutputs().get(0).stack();
            if (!itemHandler.insertItem(1, out1, true).isEmpty()) return false;
        }
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
            // [修复] .fluid()
            if (level.random.nextFloat() < inRule.consumeChance()) {
                inputTank.drain(inRule.fluid().getAmount(), IFluidHandler.FluidAction.EXECUTE);
            }
        }

        // 2. 产生输出
        if (!recipe.getItemOutputs().isEmpty()) {
            var outRule = recipe.getItemOutputs().get(0);
            // [修复] .chance() 和 .stack()
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

    // --- API & Utility ---
    public void wakeUp() {
        this.isSleeping = false;
    }

    public void markHeatCacheDirty() {
        this.heatCacheDirty = true;
        // 不立即 wakeUp，等到 tick 检查时发现 dirty 再重算，如果 processTime > 0 自然会继续跑
    }

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
        return new com.xnfu.thermalshock.client.gui.ThermalConverterMenu(id, inventory, this, this.data);
    }

    // --- NBT ---
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
        heatCacheDirty = true; // 载入时强制刷新一次热量
    }

    // ... UpdatePacket methods (Standard Boilerplate) ...
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider r) { return saveWithoutMetadata(r); }
    @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider r) { loadAdditional(pkt.getTag(), r); }
}