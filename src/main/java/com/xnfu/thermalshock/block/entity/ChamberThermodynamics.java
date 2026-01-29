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

public class ChamberThermodynamics {

    // 运行时状态
    private long lastGameTime = -1; // 上次结算的时间戳
    private double heatStored = 0;   // 改为 double 以支持精确计算，存取时转 int
    private int maxHeatCapacity = 10000;

    // 缓存值
    private int cachedHighTemp = 0;
    private int cachedLowTemp = 0;
    private int cachedNetInput = 0; // 净输入速率
    private int cachedDelta = 0;

    /**
     * [重构] 惰性更新：根据当前时间结算热量
     * 在每次 getHeat, insertHeat, 或 GUI 同步前调用
     */
    public void updateLazy(Level level) {
        if (level.isClientSide) return;

        long currentTime = level.getGameTime();
        if (lastGameTime == -1) {
            lastGameTime = currentTime;
            return;
        }

        long dt = currentTime - lastGameTime;
        if (dt > 0) {
            // 只在过热模式(需要存热)且速率不为0时计算
            // 注意：这里需要从外部传入 mode，或者默认只在Overheating模式下累积
            // 由于架构解耦，这里只负责数值结算。如果当前模式不支持存热，heatStored 应该在 setMode 时被清空，或者在这里被忽视。
            // 为了安全，我们只处理简单的累加，模式逻辑由 Controller 控制。

            if (cachedNetInput != 0) {
                double added = cachedNetInput * dt;
                this.heatStored += added;

                // 边界限制
                if (this.heatStored > maxHeatCapacity) this.heatStored = maxHeatCapacity;
                if (this.heatStored < 0) this.heatStored = 0;
            }
            lastGameTime = currentTime;
        }
    }

    /**
     * 重建热力缓存 (环境变动时调用)
     * 调用前会自动结算之前的热量
     */
    public void rebuildCache(Level level, ChamberStructure structure) {
        updateLazy(level); // 先结算

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

                BlockState targetState = level.getBlockState(targetPos);

                // A. 动态热源 (BE)
                if (targetState.getBlock() instanceof ThermalSourceBlock) {
                    if (targetState.getValue(ThermalSourceBlock.LIT)) {
                        BlockEntity be = level.getBlockEntity(targetPos);
                        if (be instanceof ThermalSourceBlockEntity sourceBe) {
                            int heat = sourceBe.getCurrentHeatOutput();
                            if (heat > 0) sumHigh += heat;
                            else sumLow += heat;
                        }
                    }
                    continue;
                }

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

                // D. 水 (简单处理)
                if (targetState.getFluidState().is(FluidTags.WATER) && targetState.getFluidState().isSource()) {
                    // 如果需要水提供被动冷却，可以在这里处理
                }
            }
        }

        // [核心修改] 应用外壳的速率限制
        // 正热：取 (总和, 上限) 的较小值
        this.cachedHighTemp = Math.min(sumHigh, structure.getMaxHeatRate());

        // 负热：sumLow 是负数，maxColdRate 是正数。
        // 我们要限制 sumLow 不能比 -maxColdRate 更"冷" (即不能更小)
        // 例: sumLow = -500, limit = 200. target = -200. -> Math.max(-500, -200) = -200
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

    // [删除] public void tick(...) - 不再需要每刻调用

    public void consumeHeat(Level level, int amount) {
        updateLazy(level); // 消费前先结算
        this.heatStored = Math.max(0, this.heatStored - amount);
    }

    public void setMaxHeatCapacity(int capacity) {
        this.maxHeatCapacity = capacity;
    }

    // 清空热量 (切换模式时调用)
    public void clearHeat() {
        this.heatStored = 0;
    }

    // --- NBT ---
    public void save(CompoundTag tag) {
        // 保存前无需 updateLazy，因为 lastGameTime 只有运行时有意义，
        // 存盘时只需要存当前瞬间的 heatStored。
        tag.putInt("HeatStored", (int) heatStored);
    }

    public void load(CompoundTag tag) {
        this.heatStored = tag.getInt("HeatStored");
        // 加载后 lastGameTime 为 -1，将在第一次 updateLazy 时重置为当前时间
        this.lastGameTime = -1;
    }

    // --- Getters (Auto Lazy Update) ---
    // 为了 GUI 显示准确，获取时尝试结算一下 (仅限服务端)
    // 客户端 heatStored 通过 ContainerData 同步，不需要计算
    public int getHeatStored(Level level) {
        if (level != null && !level.isClientSide) updateLazy(level);
        return (int) heatStored;
    }

    // 纯 Getter，不触发结算 (用于客户端或内部逻辑)
    public int getHeatStoredRaw() {
        return (int) heatStored;
    }

    public int getMaxHeatCapacity() { return maxHeatCapacity; }
    public int getCurrentHighTemp() { return cachedHighTemp; }
    public int getCurrentLowTemp() { return cachedLowTemp; }
    public int getCurrentDelta() { return cachedDelta; }
    public int getLastInputRate() { return cachedNetInput; }

    public void setHeatStored(int amount) {
        this.heatStored = amount;
    }
}