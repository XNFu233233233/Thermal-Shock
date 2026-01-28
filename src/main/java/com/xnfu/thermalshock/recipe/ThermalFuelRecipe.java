package com.xnfu.thermalshock.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.xnfu.thermalshock.registries.ThermalShockRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public class ThermalFuelRecipe implements Recipe<net.minecraft.world.item.crafting.SingleRecipeInput> {

    private final Ingredient ingredient;
    private final int burnTime;
    private final int heatRate; // 正数=热，负数=冷

    public ThermalFuelRecipe(Ingredient ingredient, int burnTime, int heatRate) {
        this.ingredient = ingredient;
        this.burnTime = burnTime;
        this.heatRate = heatRate;
    }

    @Override
    public boolean matches(net.minecraft.world.item.crafting.SingleRecipeInput input, Level level) {
        return ingredient.test(input.item());
    }

    @Override
    public ItemStack assemble(net.minecraft.world.item.crafting.SingleRecipeInput input, HolderLookup.Provider registries) {
        return ItemStack.EMPTY; // 无物品产出
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    public Ingredient getIngredient() { return ingredient; }
    public int getBurnTime() { return burnTime; }
    public int getHeatRate() { return heatRate; }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.of(Ingredient.EMPTY, ingredient);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ThermalShockRecipes.THERMAL_FUEL_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ThermalShockRecipes.THERMAL_FUEL_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<ThermalFuelRecipe> {
        public static final MapCodec<ThermalFuelRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
                Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(ThermalFuelRecipe::getIngredient),
                Codec.INT.fieldOf("burn_time").forGetter(ThermalFuelRecipe::getBurnTime),
                Codec.INT.fieldOf("heat_rate").forGetter(ThermalFuelRecipe::getHeatRate)
        ).apply(inst, ThermalFuelRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, ThermalFuelRecipe> STREAM_CODEC = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC, ThermalFuelRecipe::getIngredient,
                ByteBufCodecs.VAR_INT, ThermalFuelRecipe::getBurnTime,
                ByteBufCodecs.VAR_INT, ThermalFuelRecipe::getHeatRate,
                ThermalFuelRecipe::new
        );

        @Override public MapCodec<ThermalFuelRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, ThermalFuelRecipe> streamCodec() { return STREAM_CODEC; }
    }
}