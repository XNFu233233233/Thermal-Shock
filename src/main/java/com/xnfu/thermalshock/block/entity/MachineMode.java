package com.xnfu.thermalshock.block.entity;

import com.mojang.serialization.Codec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import io.netty.buffer.ByteBuf;

public enum MachineMode implements StringRepresentable {
    OVERHEATING("overheating", 0xFF4400),   // 红色 (过热)
    THERMAL_SHOCK("thermal_shock", 0x00FFFF); // 青色 (热冲击)

    public static final Codec<MachineMode> CODEC = StringRepresentable.fromEnum(MachineMode::values);
    public static final StreamCodec<ByteBuf, MachineMode> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    private final String name;
    private final int color;

    MachineMode(String name, int color) {
        this.name = name;
        this.color = color;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public int getColor() { return color; }

    // 用于 GUI 按钮切换
    public MachineMode next() {
        return values()[(this.ordinal() + 1) % values().length];
    }
}