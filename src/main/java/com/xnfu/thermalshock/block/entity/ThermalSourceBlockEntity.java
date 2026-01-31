package com.xnfu.thermalshock.block.entity;

import com.xnfu.thermalshock.block.ThermalSourceBlock;
import com.xnfu.thermalshock.client.gui.ThermalSourceMenu;
import com.xnfu.thermalshock.recipe.ThermalFuelRecipe;
import com.xnfu.thermalshock.registries.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class ThermalSourceBlockEntity extends BlockEntity implements MenuProvider {

    // 燃烧相关
    private int burnTime;
    private int maxBurnTime;
    private int fuelHeatOutput = 0;

    // 能量相关
    private long energyStored = 0;
    // 动态上限：根据目标热量动态调整缓存大小，防止小目标存太多电，或者大目标存不够
    private int targetElectricHeat = 0;
    private int electricHeatOutput = 0;

    // 总输出
    private int totalHeatOutput = 0;
    private int lastHeatOutput = 0; // 用于检测变化

    // 物品处理
    private final ItemStackHandler itemHandler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return true;
        }
    };

    // 能量处理 (Capability)
    private final IEnergyStorage energyStorage = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            // 缓存上限至少 10,000，或者 目标热量 * 100 (保证至少能维持10tick满载)
            long capacity = Math.max(10000, (long) targetElectricHeat * 100);
            long space = capacity - energyStored;
            int accepted = (int) Math.min(maxReceive, Math.min(space, Integer.MAX_VALUE));

            if (!simulate && accepted > 0) {
                energyStored += accepted;
                setChanged();
            }
            return Math.max(0, accepted);
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            return (int) Math.min(energyStored, Integer.MAX_VALUE);
        }

        @Override
        public int getMaxEnergyStored() {
            return (int) Math.min(Math.max(10000, (long) targetElectricHeat * 100), Integer.MAX_VALUE);
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return true;
        }
    };

    // 数据同步 (修复了之前 Size=7 导致的崩溃，现在是 8)
    private final ContainerData data = new SimpleContainerData(8) {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> burnTime;
                case 1 -> maxBurnTime;
                case 2 -> totalHeatOutput;
                case 3 -> (int) (energyStored & 0xFFFF_FFFFL); // Energy Low
                case 4 -> (int) (energyStored >>> 32);         // Energy High
                case 5 -> targetElectricHeat;
                case 6 -> (int) (Math.max(10000, (long) targetElectricHeat * 100) & 0xFFFF_FFFFL); // Max Energy Low
                case 7 -> electricHeatOutput * 10; // 实时能耗 (FE/t)
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> burnTime = value;
                case 1 -> maxBurnTime = value;
                case 2 -> totalHeatOutput = value;
                case 3 -> energyStored = (energyStored & 0xFFFF_FFFF_0000_0000L) | (value & 0xFFFF_FFFFL);
                case 4 -> energyStored = ((long) value << 32) | (energyStored & 0xFFFF_FFFFL);
                case 5 -> targetElectricHeat = value;
            }
        }
    };

    public ThermalSourceBlockEntity(BlockPos pos, BlockState blockState) {
        super(ThermalShockBlockEntities.THERMAL_SOURCE_BE.get(), pos, blockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ThermalSourceBlockEntity be) {
        if (level.isClientSide) return;

        boolean isHeater = be.isHeater();
        boolean dirty = false;
        boolean wasBurning = be.totalHeatOutput != 0;

        // === 1. 燃料逻辑 ===
        if (be.burnTime > 0) {
            be.burnTime--;
            // C. 特殊硬编码 (水) - 也可以写进 DataMap，但为了保险保留硬编码
            if (state.getFluidState().is(FluidTags.WATER)) {
                // 水大概提供微弱冷却? 这里暂设为 0 或者 -5
                // 如果 DataMap 没覆盖到，可以在这里补充
            }
            dirty = true;
        } else {
            if (be.fuelHeatOutput != 0) {
                be.fuelHeatOutput = 0;
                dirty = true;
            }
        }

        if (be.burnTime <= 0) {
            ItemStack stack = be.itemHandler.getStackInSlot(0);
            if (!stack.isEmpty()) {
                ThermalFuelRecipe recipe = be.findRecipe(level, stack);
                if (recipe != null) {
                    if ((isHeater && recipe.getHeatRate() > 0) || (!isHeater && recipe.getHeatRate() < 0)) {
                        be.burnTime = recipe.getBurnTime();
                        be.maxBurnTime = recipe.getBurnTime();
                        be.fuelHeatOutput = recipe.getHeatRate();
                        stack.shrink(1);
                        dirty = true;
                    }
                }
            }
        }

        // === 2. 能量逻辑 ===
        be.electricHeatOutput = 0;
        if (be.targetElectricHeat > 0 && be.energyStored >= 10) {
            long maxAffordableHeat = be.energyStored / 10;
            int actualOutput = (int) Math.min(be.targetElectricHeat, maxAffordableHeat);

            if (actualOutput > 0) {
                long cost = actualOutput * 10L;
                be.energyStored -= cost;
                be.electricHeatOutput = isHeater ? actualOutput : -actualOutput;
                dirty = true;
            }
        }

        // === 3. 状态更新与通知 ===
        be.totalHeatOutput = be.fuelHeatOutput + be.electricHeatOutput;
        boolean isBurning = be.totalHeatOutput != 0;

        // 3.1 视觉更新
        if (wasBurning != isBurning) {
            level.setBlock(pos, state.setValue(ThermalSourceBlock.LIT, isBurning), 3);
            dirty = true;
        }

        // 3.2 数值通知 (核心修复)
        // 只要数值变化，强制通知邻居。这会触发 SimulationPortBlock.neighborChanged
        if (be.totalHeatOutput != be.lastHeatOutput) {
            be.lastHeatOutput = be.totalHeatOutput; // 先更新缓存
            // 通知邻居方块 (Flag 3 = Update Client + Block Update)
            // 必须通知周围方块，触发 Port 的 neighborChanged -> Controller.onEnvironmentUpdate
            level.updateNeighborsAt(pos, state.getBlock()); 
            // 同时也更新自身状态，确保视觉同步
            level.sendBlockUpdated(pos, state, state, 3);
            dirty = true;
        }

        if (dirty) {
            be.setChanged();
        }
    }

    private ThermalFuelRecipe findRecipe(Level level, ItemStack stack) {
        RecipeManager rm = level.getRecipeManager();
        Optional<RecipeHolder<ThermalFuelRecipe>> recipe = rm.getRecipeFor(
                ThermalShockRecipes.THERMAL_FUEL_TYPE.get(),
                new SingleRecipeInput(stack),
                level
        );
        return recipe.map(RecipeHolder::value).orElse(null);
    }

    public void setTargetElectricHeat(int heat) {
        // 仅在数值改变时触发保存
        if (this.targetElectricHeat != Math.max(0, heat)) {
            this.targetElectricHeat = Math.max(0, heat);
            this.setChanged();
            // 注意：这里不需要手动 updateNeighborsAt，
            // 因为 target 改变会导致下一 tick 的 electricHeatOutput 改变，
            // 从而在 tick() 里触发 updateNeighborsAt。
        }
    }

    public int getCurrentHeatOutput() {
        return totalHeatOutput;
    }
    
    public int getBurnTime() {
        return burnTime;
    }
    
    public int getElectricHeatOutput() {
        return electricHeatOutput;
    }

    private boolean isHeater() {
        return this.getBlockState().is(ThermalShockBlocks.THERMAL_HEATER.get());
    }

    public IEnergyStorage getEnergyStorage() {
        return energyStorage;
    }

    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    @Override
    public Component getDisplayName() {
        return isHeater()
                ? Component.translatable("block.thermalshock.thermal_heater")
                : Component.translatable("block.thermalshock.thermal_freezer");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ThermalSourceMenu(id, inventory, this, this.data);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider r) {
        super.saveAdditional(tag, r);
        tag.put("Inventory", itemHandler.serializeNBT(r));
        tag.putInt("BurnTime", burnTime);
        tag.putInt("MaxBurnTime", maxBurnTime);
        tag.putInt("FuelHeat", fuelHeatOutput);
        tag.putLong("Energy", energyStored);
        tag.putInt("TargetElec", targetElectricHeat);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider r) {
        super.loadAdditional(tag, r);
        itemHandler.deserializeNBT(r, tag.getCompound("Inventory"));
        burnTime = tag.getInt("BurnTime");
        maxBurnTime = tag.getInt("MaxBurnTime");
        fuelHeatOutput = tag.getInt("FuelHeat");
        energyStored = tag.getLong("Energy");
        targetElectricHeat = tag.getInt("TargetElec");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider r) {
        CompoundTag t = new CompoundTag();
        saveAdditional(t, r);
        return t;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider r) {
        loadAdditional(pkt.getTag(), r);
    }
}