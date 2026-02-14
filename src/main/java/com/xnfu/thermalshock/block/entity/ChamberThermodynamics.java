package com.xnfu.thermalshock.block.entity;

import com.xnfu.thermalshock.api.IThermalHandler;
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

public class ChamberThermodynamics {

    // 运行时状态
    private int heatStored = 0;
    private int maxHeatCapacity = 10000;

    // 缓存值
    private int cachedHighTemp = 0;
    private int cachedLowTemp = 0;
    private int cachedNetInput = 0; // 净输入速率
    private int cachedDelta = 0;

    /**
     * 重建热力缓存 (环境变动时调用)
     * 调用前会自动结算之前的热量
     */
    public void recalculate(Level level, ChamberStructure structure) {
        if (level == null || !structure.isFormed()) {
            resetCache();
            return;
        }

        int sumHigh = 0;
        int sumLow = 0;

        for (BlockPos portPos : structure.getPorts()) {
            for (Direction dir : Direction.values()) {
                BlockPos targetPos = portPos.relative(dir);
                if (structure.contains(targetPos)) continue;

                if (!level.isLoaded(targetPos)) continue;

                // A. 优先尝试 Capability
                IThermalHandler thermal = level.getCapability(IThermalHandler.INTERFACE, targetPos, dir.getOpposite());
                if (thermal != null) {
                    int rate = thermal.getThermalRate();
                    if (rate > 0) sumHigh += rate;
                    else sumLow += rate;
                    continue;
                }

                BlockState targetState = level.getBlockState(targetPos);
                var holder = BuiltInRegistries.BLOCK.wrapAsHolder(targetState.getBlock());

                // B. DataMap 热源
                HeatSourceData heatData = holder.getData(ThermalShockDataMaps.HEAT_SOURCE_PROPERTY);
                if (heatData != null) {
                    sumHigh += Math.abs(heatData.heatPerTick());
                    continue;
                }

                // C. DataMap 冷源
                ColdSourceData coldData = holder.getData(ThermalShockDataMaps.COLD_SOURCE_PROPERTY);
                if (coldData != null) {
                    sumLow -= Math.abs(coldData.coolingPerTick());
                    continue;
                }
            }
        }

        this.cachedHighTemp = Math.min(sumHigh, structure.getMaxHeatRate());
        this.cachedLowTemp = Math.max(sumLow, -structure.getMaxColdRate());
        this.cachedNetInput = this.cachedHighTemp + this.cachedLowTemp;
        this.cachedDelta = this.cachedHighTemp - this.cachedLowTemp;
    }

    private void resetCache() {
        this.cachedHighTemp = 0;
        this.cachedLowTemp = 0;
        this.cachedDelta = 0;
        this.cachedNetInput = 0;
    }

    public void consumeHeat(Level level, int amount) {
        this.heatStored = Math.max(0, this.heatStored - amount);
    }

    public void setMaxHeatCapacity(int capacity) {
        this.maxHeatCapacity = capacity;
        if (this.heatStored > this.maxHeatCapacity) {
            this.heatStored = this.maxHeatCapacity;
        }
    }

    public void clearHeat() {
        this.heatStored = 0;
    }

    public void save(CompoundTag tag) {
        tag.putInt("HeatStored", heatStored);
    }

    public void load(CompoundTag tag) {
        this.heatStored = tag.getInt("HeatStored");
    }

    public int getHeatStored(Level level) {
        return heatStored;
    }

    public void addHeat(int amount) {
        this.heatStored += amount;
        if (this.heatStored > maxHeatCapacity) this.heatStored = maxHeatCapacity;
        if (this.heatStored < 0) this.heatStored = 0;
    }

    public int getCurrentHeat() {
        return heatStored;
    }

    public int getMaxHeatCapacity() { return maxHeatCapacity; }
    public int getCurrentHighTemp() { return cachedHighTemp; }
    public int getCurrentLowTemp() { return cachedLowTemp; }
    public int getDeltaT() { return cachedDelta; }
    public int getLastInputRate() { return cachedNetInput; }

    /**
     * [客户端专用] 同步环境温度，用于 Jade 显示
     */
    public void setClientTemps(int high, int low) {
        this.cachedHighTemp = high;
        this.cachedLowTemp = low;
        this.cachedDelta = high - low;
        this.cachedNetInput = high + low;
    }
    
    // --- 兼容性方法 (防止残留调用报错) ---
    public int getHeatStoredRaw() { return getCurrentHeat(); }
    public int getCurrentDelta() { return getDeltaT(); }
}
