package com.xnfu.thermalshock.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.xnfu.thermalshock.data.ClumpInfo;
import com.xnfu.thermalshock.registries.ThermalShockDataComponents;
import com.xnfu.thermalshock.registries.ThermalShockItems;
import com.xnfu.thermalshock.registries.ThermalShockRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.List;

public class ThermalShockFillingRecipe extends ThermalShockRecipe {

    private final ItemStack targetResult;
    private final int clumpMinHeatRate;
    private final int clumpHeatCost;

    public ThermalShockFillingRecipe(List<SimulationIngredient> inputs, ItemStack targetResult, int minHot, int maxCold, int delta, int clumpMinHeatRate, int clumpHeatCost) {
        // [修复] 参数改为匹配 ThermalShockRecipe (Inputs, Result, MinHot, MaxCold, Delta)
        // Result 暂时给个空 Clump，assemble 时会覆盖
        super(inputs, new ItemStack(ThermalShockItems.MATERIAL_CLUMP.get()), minHot, maxCold, delta);
        this.targetResult = targetResult;
        this.clumpMinHeatRate = clumpMinHeatRate;
        this.clumpHeatCost = clumpHeatCost;
    }

    @Override
    public ItemStack assemble(SimulationRecipeInput input, HolderLookup.Provider registries) {
        ItemStack result = new ItemStack(ThermalShockItems.MATERIAL_CLUMP.get());
        ClumpInfo info = new ClumpInfo(targetResult, clumpMinHeatRate, clumpHeatCost);
        result.set(ThermalShockDataComponents.TARGET_OUTPUT, info);
        return result;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        ItemStack stack = new ItemStack(ThermalShockItems.MATERIAL_CLUMP.get());
        ClumpInfo info = new ClumpInfo(targetResult, clumpMinHeatRate, clumpHeatCost);
        stack.set(ThermalShockDataComponents.TARGET_OUTPUT, info);
        return stack;
    }

    public ItemStack getTargetResult() { return targetResult; }
    public int getClumpMinHeatRate() { return clumpMinHeatRate; }
    public int getClumpHeatCost() { return clumpHeatCost; }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ThermalShockRecipes.THERMAL_SHOCK_FILLING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ThermalShockRecipes.THERMAL_SHOCK_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<ThermalShockFillingRecipe> {
        public static final MapCodec<ThermalShockFillingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                SimulationIngredient.CODEC.listOf().fieldOf("ingredients").forGetter(ThermalShockRecipe::getSimulationIngredients),
                ItemStack.CODEC.fieldOf("target_result").forGetter(ThermalShockFillingRecipe::getTargetResult),
                com.mojang.serialization.Codec.INT.fieldOf("min_hot").forGetter(ThermalShockRecipe::getMinHotTemp),
                com.mojang.serialization.Codec.INT.fieldOf("max_cold").forGetter(ThermalShockRecipe::getMaxColdTemp),
                com.mojang.serialization.Codec.INT.fieldOf("delta").forGetter(ThermalShockRecipe::getRequiredDelta),
                com.mojang.serialization.Codec.INT.fieldOf("clump_min_temp").forGetter(ThermalShockFillingRecipe::getClumpMinHeatRate),
                com.mojang.serialization.Codec.INT.fieldOf("clump_heat_cost").forGetter(ThermalShockFillingRecipe::getClumpHeatCost)
        ).apply(instance, ThermalShockFillingRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, ThermalShockFillingRecipe> STREAM_CODEC = StreamCodec.of(
                (buf, recipe) -> {
                    SimulationIngredient.STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buf, recipe.getSimulationIngredients());
                    ItemStack.STREAM_CODEC.encode(buf, recipe.getTargetResult());
                    ByteBufCodecs.VAR_INT.encode(buf, recipe.getMinHotTemp());
                    ByteBufCodecs.VAR_INT.encode(buf, recipe.getMaxColdTemp());
                    ByteBufCodecs.VAR_INT.encode(buf, recipe.getRequiredDelta());
                    ByteBufCodecs.VAR_INT.encode(buf, recipe.getClumpMinHeatRate());
                    ByteBufCodecs.VAR_INT.encode(buf, recipe.getClumpHeatCost());
                },
                buf -> {
                    var ingredients = SimulationIngredient.STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buf);
                    var target = ItemStack.STREAM_CODEC.decode(buf);
                    var minHot = ByteBufCodecs.VAR_INT.decode(buf);
                    var maxCold = ByteBufCodecs.VAR_INT.decode(buf);
                    var delta = ByteBufCodecs.VAR_INT.decode(buf);
                    var clumpMin = ByteBufCodecs.VAR_INT.decode(buf);
                    var clumpCost = ByteBufCodecs.VAR_INT.decode(buf);
                    return new ThermalShockFillingRecipe(ingredients, target, minHot, maxCold, delta, clumpMin, clumpCost);
                }
        );

        @Override public MapCodec<ThermalShockFillingRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, ThermalShockFillingRecipe> streamCodec() { return STREAM_CODEC; }
    }
}