package com.xnfu.thermalshock.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * @param minTemp 最小工作温度
 * @param maxTemp 最大工作温度
 * @param efficiency 催化效率 (越高越好，默认 1.0)
 */
public record CasingData(int minTemp, int maxTemp, float efficiency) {
    public static final Codec<CasingData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("min_temp").forGetter(CasingData::minTemp),
            Codec.INT.fieldOf("max_temp").forGetter(CasingData::maxTemp),
            Codec.FLOAT.optionalFieldOf("efficiency", 1.0f).forGetter(CasingData::efficiency)
    ).apply(instance, CasingData::new));
}