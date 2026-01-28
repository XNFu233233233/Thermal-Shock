package com.xnfu.thermalshock.recipe;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

public enum RecipeSourceType implements StringRepresentable {
    BLOCK("block"),
    ITEM("item");

    private final String name;

    // 网络同步 Codec
    public static final StreamCodec<ByteBuf, RecipeSourceType> STREAM_CODEC = ByteBufCodecs.idMapper(i -> values()[i], RecipeSourceType::ordinal);

    RecipeSourceType(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}