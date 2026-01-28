package com.xnfu.thermalshock.block.entity;

import com.xnfu.thermalshock.block.ThermalSourceBlock;
import com.xnfu.thermalshock.recipe.ThermalFuelRecipe;
import com.xnfu.thermalshock.registries.ThermalShockBlockEntities;
import com.xnfu.thermalshock.registries.ThermalShockBlocks;
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
    private int fuelHeatOutput = 0; // 燃料提供的热值

    // 能量相关
    private long energyStored = 0;
    private long maxEnergyStored = 10000;
    private int targetElectricHeat = 0; // 玩家设定的目标电子热值
    private int electricHeatOutput = 0; // 实际产生的电子热值

    // 总输出
    private int totalHeatOutput = 0;
    private int lastHeatOutput = 0;

    // 物品处理
    private final ItemStackHandler itemHandler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            // 简单校验，允许服务端后续精确判断
            return true;
        }
    };

    // 能量处理 (Capability)
    private final IEnergyStorage energyStorage = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            // 动态上限
            long capacity = Math.max(1000, (long) targetElectricHeat * 100);
            long space = capacity - energyStored;
            int accepted = (int) Math.min(maxReceive, Math.min(space, Integer.MAX_VALUE));

            if (!simulate && accepted > 0) {
                energyStored += accepted;
                setChanged();
            }
            return Math.max(0, accepted);
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) { return 0; }

        @Override
        public int getEnergyStored() { return (int) Math.min(energyStored, Integer.MAX_VALUE); }

        @Override
        public int getMaxEnergyStored() { return (int) Math.min(Math.max(1000, (long) targetElectricHeat * 100), Integer.MAX_VALUE); }

        @Override
        public boolean canExtract() { return false; }

        @Override
        public boolean canReceive() { return true; }
    };

    // 数据同步 (大小 7: 燃烧2 + 热值2 + 能量2(Low/High) + 目标1)
    private final ContainerData data = new SimpleContainerData(7) {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> burnTime;
                case 1 -> maxBurnTime;
                case 2 -> totalHeatOutput;
                case 3 -> (int) (energyStored & 0xFFFF_FFFFL); // Energy Low
                case 4 -> (int) (energyStored >>> 32);         // Energy High
                case 5 -> targetElectricHeat;
                case 6 -> (int) (Math.max(1000, (long) targetElectricHeat * 100) & 0xFFFF_FFFFL); // Max Energy Low (简化同步)
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
        boolean wasBurning = be.burnTime > 0;

        // === 1. 燃料逻辑 ===
        if (be.burnTime > 0) {
            be.burnTime--;
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

        // === 2. 能量逻辑 (非线性消耗) ===
        be.electricHeatOutput = 0;
        if (be.targetElectricHeat > 0) {
            // 基础转换: 10 FE = 1 H
            // 非线性惩罚: (Heat^2) / 50
            // 总消耗 = (Target * 10) + (Target * Target / 50)
            long cost = (long) be.targetElectricHeat * 10 + ((long) be.targetElectricHeat * be.targetElectricHeat / 50);

            if (be.energyStored >= cost) {
                be.energyStored -= cost;
                // 注意：如果是 Freezer，产生的“电子热值”应该是负数
                be.electricHeatOutput = isHeater ? be.targetElectricHeat : -be.targetElectricHeat;
                dirty = true;
            }
        }

        // === 3. 总热值计算与状态更新 ===
        be.totalHeatOutput = be.fuelHeatOutput + be.electricHeatOutput;

        boolean isBurning = be.totalHeatOutput != 0; // 只要有输出就算工作

        if (wasBurning != isBurning) {
            level.setBlock(pos, state.setValue(ThermalSourceBlock.LIT, isBurning), 3);
            dirty = true;
        } else if (be.totalHeatOutput != be.lastHeatOutput) {
            // 数值变化触发更新
            level.updateNeighborsAt(pos, state.getBlock());
            be.lastHeatOutput = be.totalHeatOutput;
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
        this.targetElectricHeat = Math.max(0, heat); // 只能设置绝对值
        this.setChanged();
    }

    public int getCurrentHeatOutput() {
        return totalHeatOutput;
    }

    private boolean isHeater() {
        return this.getBlockState().is(ThermalShockBlocks.THERMAL_HEATER.get());
    }

    public IEnergyStorage getEnergyStorage() { return energyStorage; }
    public ItemStackHandler getItemHandler() { return itemHandler; }

    // --- GUI ---
    @Override
    public Component getDisplayName() {
        return isHeater()
                ? Component.translatable("block.thermalshock.thermal_heater")
                : Component.translatable("block.thermalshock.thermal_freezer");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new com.xnfu.thermalshock.client.gui.ThermalSourceMenu(id, inventory, this, this.data);
    }

    // --- NBT ---
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

    // ... UpdatePacket methods same as before ...
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