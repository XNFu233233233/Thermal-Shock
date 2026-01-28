package com.xnfu.thermalshock.block.entity;

import com.xnfu.thermalshock.block.ThermalSourceBlock;
import com.xnfu.thermalshock.data.ColdSourceData;
import com.xnfu.thermalshock.data.HeatSourceData;
import com.xnfu.thermalshock.registries.ThermalShockDataMaps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

import java.util.ArrayList;
import java.util.List;

public class ChamberThermodynamics {

    // 运行时状态
    private int heatStored = 0;
    private int maxHeatCapacity = 10000;

    // [核心优化] 预计算的缓存值
    private int cachedHighTemp = 0;
    private int cachedLowTemp = 0;
    private int cachedNetInput = 0; // 净输入 (Sum All)
    private int cachedDelta = 0;    // 温差 (High - Low)

    /**
     * 重建热力缓存。
     */
    /**
     * 重建热力缓存。
     */
    public void rebuildCache(Level level, ChamberStructure structure) {
        if (level == null || !structure.isFormed()) {
            resetCache();
            return;
        }

        int sumHigh = 0; // 总是 >= 0
        int sumLow = 0;  // 总是 <= 0

        for (BlockPos portPos : structure.getPorts()) {
            for (Direction dir : Direction.values()) {
                BlockPos targetPos = portPos.relative(dir);
                if (structure.contains(targetPos)) continue;

                BlockState targetState = level.getBlockState(targetPos);

                // === [修改] 动态热源检测：直接读取 BE ===
                if (targetState.getBlock() instanceof ThermalSourceBlock) {
                    if (targetState.getValue(ThermalSourceBlock.LIT)) {
                        BlockEntity be = level.getBlockEntity(targetPos);
                        if (be instanceof ThermalSourceBlockEntity sourceBe) {
                            int heat = sourceBe.getCurrentHeatOutput();
                            if (heat > 0) {
                                sumHigh += heat;
                            } else {
                                // 注意：Freezer 产出的是负数，sumLow 需要负数累加
                                sumLow += heat;
                            }
                        }
                    }
                    continue;
                }
                // ==========================

                var holder = BuiltInRegistries.BLOCK.wrapAsHolder(targetState.getBlock());

                // 1. DataMap 热源判定 (强制为正)
                HeatSourceData heatData = holder.getData(ThermalShockDataMaps.HEAT_SOURCE_PROPERTY);
                if (heatData != null) {
                    sumHigh += Math.abs(heatData.heatPerTick());
                    continue;
                }

                // 2. DataMap 冷源判定 (强制为负)
                ColdSourceData coldData = holder.getData(ThermalShockDataMaps.COLD_SOURCE_PROPERTY);
                if (coldData != null) {
                    sumLow -= Math.abs(coldData.coolingPerTick());
                    continue;
                }

                // 3. 水判定
                net.minecraft.world.level.material.FluidState fluidState = targetState.getFluidState();
                if (fluidState.is(FluidTags.WATER) && fluidState.isSource()) {
                    sumLow = 0; // 水 = 0度
                }
            }
        }

        this.cachedHighTemp = sumHigh;
        this.cachedLowTemp = sumLow;

        // 过热模式：代数和 (High + Low) -> (100 + (-10) = 90)
        this.cachedNetInput = this.cachedHighTemp + this.cachedLowTemp;

        // 热冲击模式：差值 (High - Low) -> (100 - (-10) = 110)
        this.cachedDelta = this.cachedHighTemp - this.cachedLowTemp;
    }

    private void resetCache() {
        this.cachedHighTemp = 0;
        this.cachedLowTemp = 0;
        this.cachedDelta = 0;
        this.cachedNetInput = 0;
    }

    /**
     * 每 Tick 调用。纯数值加减。
     */
    public void tick(MachineMode mode) {
        if (mode == MachineMode.OVERHEATING) {
            // 过热模式：积累热量 (使用代数和)
            if (this.cachedNetInput > 0) {
                this.heatStored = Math.min(maxHeatCapacity, this.heatStored + this.cachedNetInput);
            } else if (this.cachedNetInput < 0) {
                // 如果净输入是负的，自然冷却
                this.heatStored = Math.max(0, this.heatStored + this.cachedNetInput);
            }
        } else {
            // 热冲击模式：不存储热量
            this.heatStored = 0;
        }
    }

    public void consumeHeat(int amount) {
        this.heatStored = Math.max(0, this.heatStored - amount);
    }

    public void setMaxHeatCapacity(int capacity) {
        this.maxHeatCapacity = capacity;
    }

    // --- NBT ---
    public void save(CompoundTag tag) {
        tag.putInt("HeatStored", heatStored);
    }

    public void load(CompoundTag tag) {
        this.heatStored = tag.getInt("HeatStored");
    }

    // --- Getters ---
    public int getHeatStored() {
        return heatStored;
    }

    public int getMaxHeatCapacity() {
        return maxHeatCapacity;
    }

    public int getCurrentHighTemp() {
        return cachedHighTemp;
    }

    public int getCurrentLowTemp() {
        return cachedLowTemp;
    }

    public int getCurrentDelta() {
        return cachedDelta;
    }

    public int getLastInputRate() {
        return cachedNetInput;
    }

    public void setHeatStored(int amount) {
        this.heatStored = amount;
    }
}