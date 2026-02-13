package com.xnfu.thermalshock.recipe;

import com.mojang.serialization.Codec;
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
        super(inputs, new ItemStack(ThermalShockItems.MATERIAL_CLUMP.get()), minHot, maxCold, delta);
        this.targetResult = targetResult;
    }

    @Override
    public ItemStack assemble(SimulationRecipeInput input, HolderLookup.Provider registries) {
        ItemStack result = new ItemStack(ThermalShockItems.MATERIAL_CLUMP.get());
        ClumpInfo info = new ClumpInfo(targetResult);
        result.set(ThermalShockDataComponents.TARGET_OUTPUT, info);
        return result;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        ItemStack stack = new ItemStack(ThermalShockItems.MATERIAL_CLUMP.get());
        ClumpInfo info = new ClumpInfo(targetResult);
        stack.set(ThermalShockDataComponents.TARGET_OUTPUT, info);
        return stack;
    }

    public ItemStack getTargetResult() { return targetResult; }

    @Override
    public boolean matches(SimulationRecipeInput input, net.minecraft.world.level.Level level) {
        if (!input.primary().is(ThermalShockItems.MATERIAL_CLUMP.get())) return false;
        return super.matches(input, level);
    }

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
                Codec.INT.optionalFieldOf("min_hot", Integer.MIN_VALUE).forGetter(ThermalShockRecipe::getMinHotTemp),
                Codec.INT.optionalFieldOf("max_cold", Integer.MAX_VALUE).forGetter(ThermalShockRecipe::getMaxColdTemp),
                Codec.INT.fieldOf("delta").forGetter(ThermalShockRecipe::getRequiredDelta)
        ).apply(instance, ThermalShockFillingRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, ThermalShockFillingRecipe> STREAM_CODEC = StreamCodec.of(
                (buf, recipe) -> {
                    SimulationIngredient.STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buf, recipe.getSimulationIngredients());
                    ItemStack.STREAM_CODEC.encode(buf, recipe.getTargetResult());
                    ByteBufCodecs.VAR_INT.encode(buf, recipe.getMinHotTemp());
                    ByteBufCodecs.VAR_INT.encode(buf, recipe.getMaxColdTemp());
                    ByteBufCodecs.VAR_INT.encode(buf, recipe.getRequiredDelta());
                },
                buf -> {
                    var ingredients = SimulationIngredient.STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buf);
                    var target = ItemStack.STREAM_CODEC.decode(buf);
                    var minHot = ByteBufCodecs.VAR_INT.decode(buf);
                    var maxCold = ByteBufCodecs.VAR_INT.decode(buf);
                    var delta = ByteBufCodecs.VAR_INT.decode(buf);
                    return new ThermalShockFillingRecipe(ingredients, target, minHot, maxCold, delta);
                }
        );

        @Override public MapCodec<ThermalShockFillingRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, ThermalShockFillingRecipe> streamCodec() { return STREAM_CODEC; }
    }
}
