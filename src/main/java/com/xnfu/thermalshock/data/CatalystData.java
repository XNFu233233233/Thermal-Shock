package com.xnfu.thermalshock.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record CatalystData(float bonusYield, float bufferAmount) {
    public static final Codec<CatalystData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.fieldOf("bonus_yield").forGetter(CatalystData::bonusYield), // 例如 0.1 代表 +10%
            Codec.FLOAT.optionalFieldOf("buffer_amount", 10.0f).forGetter(CatalystData::bufferAmount) // 1个物品补充多少点数
    ).apply(instance, CatalystData::new));
}