package com.xnfu.thermalshock.block.entity;

import com.mojang.serialization.Codec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import io.netty.buffer.ByteBuf;

public enum PortMode implements StringRepresentable {
    INPUT("input", 0xFF0000FF),       // 蓝色：原料输入
    OUTPUT("output", 0xFFFFAA00),     // 橙色：产物输出
    CATALYST("catalyst", 0xFFFF55FF), // 粉色：催化剂输入
    NONE("none", 0xFF888888);         // 灰色：关闭

    public static final Codec<PortMode> CODEC = StringRepresentable.fromEnum(PortMode::values);
    public static final StreamCodec<ByteBuf, PortMode> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    private final String name;
    private final int color;

    PortMode(String name, int color) {
        this.name = name;
        this.color = color;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public int getColor() { return color; }

    public PortMode next() {
        return values()[(this.ordinal() + 1) % values().length];
    }
}