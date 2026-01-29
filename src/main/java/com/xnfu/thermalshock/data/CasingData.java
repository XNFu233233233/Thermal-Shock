package com.xnfu.thermalshock.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * 外壳属性定义
 * @param maxHeatRate 最大热量输入速率 (Heat/tick)，限制正热源
 * @param maxColdRate 最大冷量输入速率 (Heat/tick)，限制负热源 (绝对值)
 * @param efficiency 催化效率 (默认 1.0)
 */
public record CasingData(int maxHeatRate, int maxColdRate, float efficiency) {
    public static final Codec<CasingData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("max_heat_rate").forGetter(CasingData::maxHeatRate),
            Codec.INT.fieldOf("max_cold_rate").forGetter(CasingData::maxColdRate),
            Codec.FLOAT.optionalFieldOf("efficiency", 1.0f).forGetter(CasingData::efficiency)
    ).apply(instance, CasingData::new));
}