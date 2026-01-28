package com.xnfu.thermalshock.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.xnfu.thermalshock.data.ClumpInfo;
import com.xnfu.thermalshock.registries.ThermalShockDataComponents;
import com.xnfu.thermalshock.registries.ThermalShockItems;
import com.xnfu.thermalshock.registries.ThermalShockRecipes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.List;

public class ClumpProcessingRecipe extends OverheatingRecipe {

    private final ItemStack targetContent; // Clump 里必须装的是这个东西

    public ClumpProcessingRecipe(ItemStack targetContent, int minTemp, int heatCost) {
        // 父类构造：输入固定为 Material Clump，输出就是 targetContent
        super(List.of(new SimulationIngredient(Ingredient.of(ThermalShockItems.MATERIAL_CLUMP.get()), RecipeSourceType.ITEM)),
                targetContent, minTemp, heatCost);
        this.targetContent = targetContent;
    }

    @Override
    public boolean matches(SimulationRecipeInput input, Level level) {
        ItemStack inputStack = input.ingredient();
        // 1. 必须是 Material Clump
        if (!inputStack.is(ThermalShockItems.MATERIAL_CLUMP.get())) return false;

        // 2. 获取数据组件
        ClumpInfo info = inputStack.get(ThermalShockDataComponents.TARGET_OUTPUT);
        if (info == null || info.result().isEmpty()) return false;

        // 3. [核心] 比较 Clump 里的东西 是否等于 配方要求的东西
        // 使用 isSameItemSameComponents 忽略数量差异
        return ItemStack.isSameItemSameComponents(info.result(), this.targetContent);
    }

    public ItemStack getTargetContent() {
        return targetContent;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ThermalShockRecipes.CLUMP_PROCESSING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        // 复用 Overheating 类型，这样机器的通用逻辑能扫到它
        // 或者使用单独的 Type，为了方便管理，这里建议复用，但在 GUI 里过滤
        return ThermalShockRecipes.OVERHEATING_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<ClumpProcessingRecipe> {
        public static final MapCodec<ClumpProcessingRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
                ItemStack.CODEC.fieldOf("target_content").forGetter(ClumpProcessingRecipe::getTargetContent),
                Codec.INT.fieldOf("min_temp").forGetter(ClumpProcessingRecipe::getMinTemp),
                Codec.INT.fieldOf("heat_cost").forGetter(ClumpProcessingRecipe::getHeatCost)
        ).apply(inst, ClumpProcessingRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, ClumpProcessingRecipe> STREAM_CODEC = StreamCodec.composite(
                ItemStack.STREAM_CODEC, ClumpProcessingRecipe::getTargetContent,
                ByteBufCodecs.VAR_INT, ClumpProcessingRecipe::getMinTemp,
                ByteBufCodecs.VAR_INT, ClumpProcessingRecipe::getHeatCost,
                ClumpProcessingRecipe::new
        );

        @Override public MapCodec<ClumpProcessingRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, ClumpProcessingRecipe> streamCodec() { return STREAM_CODEC; }
    }
}