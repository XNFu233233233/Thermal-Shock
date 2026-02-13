package com.xnfu.thermalshock.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

/**
 * 物质团块的数据载体 (极致优化版)。
 * 只存储物品引用和数量，绕过笨重的 ItemStack 容器。
 */
public record ClumpInfo(Holder<Item> item, int count) {

    /**
     * 方案 B: 紧凑型 Codec。
     * count 为 1 时表现为字符串 ID，否则表现为对象。
     */
    public static final Codec<ClumpInfo> CODEC = Codec.lazyInitialized(() -> 
        Codec.either(
            BuiltInRegistries.ITEM.holderByNameCodec(), // 简单模式: "id"
            RecordCodecBuilder.<ClumpInfo>create(inst -> inst.group(
                BuiltInRegistries.ITEM.holderByNameCodec().fieldOf("item").forGetter(ClumpInfo::item),
                Codec.INT.optionalFieldOf("count", 1).forGetter(ClumpInfo::count)
            ).apply(inst, ClumpInfo::new)) // 对象模式: {"item":"id", "count":32}
        ).xmap(
            either -> either.map(h -> new ClumpInfo(h, 1), info -> info),
            info -> info.count() == 1 ? com.mojang.datafixers.util.Either.left(info.item()) : com.mojang.datafixers.util.Either.right(info)
        )
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, ClumpInfo> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.holderRegistry(net.minecraft.core.registries.Registries.ITEM), ClumpInfo::item,
            ByteBufCodecs.VAR_INT, ClumpInfo::count,
            ClumpInfo::new
    );

    public ItemStack createStack() {
        return new ItemStack(item, count);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClumpInfo that = (ClumpInfo) o;
        return count == that.count && Objects.equals(item, that.item);
    }

    @Override
    public int hashCode() {
        return Objects.hash(item, count);
    }
}
