package com.xnfu.thermalshock.block.entity;


public class ChamberPerformance {

    private int batchSize = 1;
    private float efficiency = 1.0f;
    private float yieldMultiplier = 1.0f;
    private boolean isVirtual = false;

    public void update(ChamberStructure structure, int upgradeCount) {
        if (upgradeCount > 0) {
            // === 虚拟模式 (升级卡接管) ===
            this.isVirtual = true;

            // 1. 批处理 (Batch Size)
            // 规则: 基础1 + 每个升级增加4
            // 64个升级 = 257 物品/次
            this.batchSize = Math.max(1, upgradeCount * 4);

            // 2. 阶梯爆发算法 (Tiered Burst Strategy)
            // 每 8 个升级视为一个完整的 "Tier"
            // n=7 (Tier 0 max) -> Yield ~2.0x (+100%)
            // n=8 (Tier 1 min) -> Yield ~4.2x (+320%) -> 爆发式增长
            // n=64 (Tier 8)    -> Yield 26.6x (+2560%)

            int tier = upgradeCount / 8;
            int progress = upgradeCount % 8;

            // --- 产量倍率 (Yield) ---
            // 每一整级 (Tier) 提供 +3.2x (320%) 的基础阶梯加成
            // 目标: 8 * 3.2 = 25.6 (加成) + 1.0 (基础) = 26.6x
            float yieldBase = 1.0f + (tier * 3.2f);

            // 层级间的线性填充: 0-7 个升级提供约 +1.0x 的增长 (每个 0.14)
            // 这样凑齐 7 个时约为 1.0 + 0.98 = 1.98x，放入第 8 个瞬间跳到 4.2x
            float yieldProgress = progress * 0.14f;

            this.yieldMultiplier = yieldBase + yieldProgress;

            // --- 效率倍率 (Efficiency) ---
            // 目标: 64个时 +1280% (12.8x 加成 + 1.0 基础 = 13.8x)
            // 系数减半: Tier系数 1.6, Progress系数 0.07
            float effBase = 1.0f + (tier * 1.6f);
            float effProgress = progress * 0.07f;

            this.efficiency = effBase + effProgress;

        } else {
            // === 物理模式 (结构决定) ===
            this.isVirtual = false;

            if (structure.isFormed()) {
                // 批处理: 受内部体积限制
                this.batchSize = net.minecraft.util.Mth.clamp(structure.getVolume(), 1, 64);

                // 产量倍率: 3x3=1, 5x5=2...
                this.yieldMultiplier = structure.getYieldMultiplier();

                // 效率: 仅取决于外壳 (e.g. 1.0)
                this.efficiency = structure.getEfficiency();

            } else {
                this.batchSize = 0;
                this.efficiency = 0;
                this.yieldMultiplier = 0;
            }
        }
    }

    public int getBatchSize() { return batchSize; }
    public float getEfficiency() { return efficiency; }
    public float getYieldMultiplier() { return yieldMultiplier; }
    public boolean isVirtual() { return isVirtual; }
}