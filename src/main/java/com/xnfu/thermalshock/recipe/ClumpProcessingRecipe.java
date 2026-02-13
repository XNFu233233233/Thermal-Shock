package com.xnfu.thermalshock.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.xnfu.thermalshock.data.ClumpInfo;
import com.xnfu.thermalshock.registries.ThermalShockDataComponents;
import com.xnfu.thermalshock.registries.ThermalShockItems;
import com.xnfu.thermalshock.registries.ThermalShockRecipes;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.List;

public class ClumpProcessingRecipe extends OverheatingRecipe {

    private final Holder<Item> targetItem; // Clump 里必须装的是这个东西

    public ClumpProcessingRecipe(List<SimulationIngredient> inputs, Holder<Item> targetItem, int minHeatRate, int heatCost) {
        // Parent result is the output when processing is done
        super(inputs, new ItemStack(targetItem), minHeatRate, heatCost);
        this.targetItem = targetItem;
    }

    @Override
    public boolean matches(SimulationRecipeInput input, Level level) {
        ItemStack stack = input.primary();
        if (!stack.is(ThermalShockItems.MATERIAL_CLUMP.get())) return false;

        ClumpInfo info = stack.get(ThermalShockDataComponents.TARGET_OUTPUT);
        if (info == null) return false;

        return info.item().equals(this.targetItem);
    }

    public Holder<Item> getTargetItem() {
        return targetItem;
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
                SimulationIngredient.CODEC.listOf().fieldOf("ingredients").forGetter(ClumpProcessingRecipe::getSimulationIngredients),
                BuiltInRegistries.ITEM.holderByNameCodec().fieldOf("target_item").forGetter(ClumpProcessingRecipe::getTargetItem),
                Codec.INT.optionalFieldOf("min_heat", 0).forGetter(r -> r.getMinHeatRate()),
                Codec.INT.optionalFieldOf("heat_cost", 100).forGetter(r -> r.getHeatCost())
        ).apply(inst, ClumpProcessingRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, ClumpProcessingRecipe> STREAM_CODEC = StreamCodec.composite(
                SimulationIngredient.STREAM_CODEC.apply(ByteBufCodecs.list()), ClumpProcessingRecipe::getSimulationIngredients,
                ByteBufCodecs.holderRegistry(net.minecraft.core.registries.Registries.ITEM), ClumpProcessingRecipe::getTargetItem,
                ByteBufCodecs.VAR_INT, ClumpProcessingRecipe::getMinHeatRate,
                ByteBufCodecs.VAR_INT, ClumpProcessingRecipe::getHeatCost,
                ClumpProcessingRecipe::new
        );

        @Override public MapCodec<ClumpProcessingRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, ClumpProcessingRecipe> streamCodec() { return STREAM_CODEC; }
    }
}
