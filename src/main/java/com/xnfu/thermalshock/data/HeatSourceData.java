package com.xnfu.thermalshock.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record HeatSourceData(int heatPerTick) {
    public static final Codec<HeatSourceData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("heat_per_tick").forGetter(HeatSourceData::heatPerTick)
    ).apply(instance, HeatSourceData::new));
}