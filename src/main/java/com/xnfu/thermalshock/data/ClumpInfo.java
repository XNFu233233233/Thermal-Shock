package com.xnfu.thermalshock.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

/**
 * 物质团块的数据载体。
 * 现在只负责存储注入的产物信息。提取阶段所需的温度和消耗由配方 (ClumpProcessingRecipe) 独立定义。
 * @param result 最终产出物品
 */
public record ClumpInfo(ItemStack result) {

    public static final Codec<ClumpInfo> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ItemStack.CODEC.fieldOf("result").forGetter(ClumpInfo::result)
    ).apply(instance, ClumpInfo::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ClumpInfo> STREAM_CODEC = StreamCodec.composite(
            ItemStack.STREAM_CODEC, ClumpInfo::result,
            ClumpInfo::new
    );

    public ClumpInfo {
        if (!result.isEmpty()) {
            if (result.getCount() != 1) {
                result = result.copy();
                result.setCount(1);
            }
        } else {
            result = ItemStack.EMPTY;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClumpInfo clumpInfo = (ClumpInfo) o;
        return ItemStack.isSameItemSameComponents(result, clumpInfo.result);
    }

    @Override
    public int hashCode() {
        return ItemStack.hashItemAndComponents(result);
    }
}
