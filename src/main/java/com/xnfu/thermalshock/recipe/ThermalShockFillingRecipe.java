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

    public ThermalShockFillingRecipe(List<SimulationIngredient> inputs, ItemStack targetResult, int minHot, int maxCold, int delta) {
        // [修复] 参数改为匹配 ThermalShockRecipe (Inputs, Result, MinHot, MaxCold, Delta)
        // Result 暂时给个空 Clump，assemble 时会覆盖
        super(inputs, new ItemStack(ThermalShockItems.MATERIAL_CLUMP.get()), minHot, maxCold, delta);
        this.targetResult = targetResult;
    }

    @Override
    public ItemStack assemble(SimulationRecipeInput input, HolderLookup.Provider registries) {
        ItemStack result = new ItemStack(ThermalShockItems.MATERIAL_CLUMP.get());
        ClumpInfo info = new ClumpInfo(targetResult, 200, 100);
        result.set(ThermalShockDataComponents.TARGET_OUTPUT, info);
        return result;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        ItemStack stack = new ItemStack(ThermalShockItems.MATERIAL_CLUMP.get());
        ClumpInfo info = new ClumpInfo(targetResult, 200, 100);
        stack.set(ThermalShockDataComponents.TARGET_OUTPUT, info);
        return stack;
    }

    public ItemStack getTargetResult() { return targetResult; }

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
                com.mojang.serialization.Codec.INT.fieldOf("delta").forGetter(ThermalShockRecipe::getRequiredDelta)
        ).apply(instance, ThermalShockFillingRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, ThermalShockFillingRecipe> STREAM_CODEC = StreamCodec.composite(
                SimulationIngredient.STREAM_CODEC.apply(ByteBufCodecs.list()), ThermalShockRecipe::getSimulationIngredients,
                ItemStack.STREAM_CODEC, ThermalShockFillingRecipe::getTargetResult,
                ByteBufCodecs.VAR_INT, ThermalShockRecipe::getMinHotTemp,
                ByteBufCodecs.VAR_INT, ThermalShockRecipe::getMaxColdTemp,
                ByteBufCodecs.VAR_INT, ThermalShockRecipe::getRequiredDelta,
                ThermalShockFillingRecipe::new
        );

        @Override public MapCodec<ThermalShockFillingRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, ThermalShockFillingRecipe> streamCodec() { return STREAM_CODEC; }
    }
}