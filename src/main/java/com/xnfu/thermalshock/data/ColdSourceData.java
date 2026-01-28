package com.xnfu.thermalshock.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record ColdSourceData(int coolingPerTick) {
    public static final Codec<ColdSourceData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("cooling_per_tick").forGetter(ColdSourceData::coolingPerTick)
    ).apply(instance, ColdSourceData::new));
}