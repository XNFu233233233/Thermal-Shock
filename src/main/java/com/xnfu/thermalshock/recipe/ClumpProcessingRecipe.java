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
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.List;

public class ClumpProcessingRecipe extends OverheatingRecipe {

    private final ItemStack targetContent; // Clump 里必须装的是这个东西
    protected final List<SimulationIngredient> simulationIngredients;

    public ClumpProcessingRecipe(List<SimulationIngredient> inputs, ItemStack targetContent, int minHeatRate, int heatCost) {
        // 父类构造：输入列表不再固定，由 JSON 定义
        super(inputs, targetContent, minHeatRate, heatCost);
        this.targetContent = targetContent;
        this.simulationIngredients = inputs;
    }

    @Override
    public boolean matches(SimulationRecipeInput input, Level level) {
        ItemStack stack = input.primary();
        // 1. 必须是 Material Clump (这里假设 input 是主原料)
        if (!stack.is(ThermalShockItems.MATERIAL_CLUMP.get())) return false;

        // 2. 获取数据组件
        ClumpInfo info = stack.get(ThermalShockDataComponents.TARGET_OUTPUT);
        if (info == null || info.result().isEmpty()) return false;

        // 3. [核心] 比较 Clump 里的东西 是否等于 配方要求的东西
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
        return ThermalShockRecipes.OVERHEATING_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<ClumpProcessingRecipe> {
        public static final MapCodec<ClumpProcessingRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
                SimulationIngredient.CODEC.listOf().fieldOf("ingredients").forGetter(r -> r.simulationIngredients),
                ItemStack.CODEC.fieldOf("target_content").forGetter(ClumpProcessingRecipe::getTargetContent),
                Codec.INT.fieldOf("min_heat").forGetter(r -> r.getMinHeatRate()),
                Codec.INT.fieldOf("heat_cost").forGetter(r -> r.getHeatCost())
        ).apply(inst, ClumpProcessingRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, ClumpProcessingRecipe> STREAM_CODEC = StreamCodec.composite(
                SimulationIngredient.STREAM_CODEC.apply(ByteBufCodecs.list()), r -> r.simulationIngredients,
                ItemStack.STREAM_CODEC, ClumpProcessingRecipe::getTargetContent,
                ByteBufCodecs.VAR_INT, r -> r.getMinHeatRate(),
                ByteBufCodecs.VAR_INT, r -> r.getHeatCost(),
                ClumpProcessingRecipe::new
        );

        @Override public MapCodec<ClumpProcessingRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, ClumpProcessingRecipe> streamCodec() { return STREAM_CODEC; }
    }
}