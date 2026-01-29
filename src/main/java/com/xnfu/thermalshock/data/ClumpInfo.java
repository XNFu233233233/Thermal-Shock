package com.xnfu.thermalshock.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

/**
 * 热熔团的数据载体。
 * @param result 最终产出物品 (机器实际产出的东西，如: 铁锭)
 * @param minHeatRate 所需温度条件 (用于过热模式处理这个团)
 * @param heatCost 热量消耗 (用于过热模式处理这个团)
 */
public record ClumpInfo(ItemStack result, int minHeatRate, int heatCost) {

    public static final Codec<ClumpInfo> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ItemStack.CODEC.fieldOf("result").forGetter(ClumpInfo::result),
            Codec.INT.fieldOf("min_temp").forGetter(ClumpInfo::minHeatRate),
            Codec.INT.fieldOf("heat_cost").forGetter(ClumpInfo::heatCost)
    ).apply(instance, ClumpInfo::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ClumpInfo> STREAM_CODEC = StreamCodec.composite(
            ItemStack.STREAM_CODEC, ClumpInfo::result,
            net.minecraft.network.codec.ByteBufCodecs.VAR_INT, ClumpInfo::minHeatRate,
            net.minecraft.network.codec.ByteBufCodecs.VAR_INT, ClumpInfo::heatCost,
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
        return minHeatRate == clumpInfo.minHeatRate &&
                heatCost == clumpInfo.heatCost &&
                ItemStack.isSameItemSameComponents(result, clumpInfo.result);
    }

    @Override
    public int hashCode() {
        int h = ItemStack.hashItemAndComponents(result);
        return Objects.hash(h, minHeatRate, heatCost);
    }
}