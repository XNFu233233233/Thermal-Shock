package com.xnfu.thermalshock.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record CatalystData(float bonusYield, float catalystPoints) {
    public static final Codec<CatalystData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.fieldOf("bonus_yield").forGetter(CatalystData::bonusYield), // 例如 0.1 代表 +10%
            Codec.FLOAT.optionalFieldOf("catalyst_points", 10.0f).forGetter(CatalystData::catalystPoints) // 1个物品提供的催化容量
    ).apply(instance, CatalystData::new));
}