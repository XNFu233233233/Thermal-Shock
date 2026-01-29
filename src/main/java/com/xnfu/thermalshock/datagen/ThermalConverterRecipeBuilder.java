package com.xnfu.thermalshock.datagen;

import com.xnfu.thermalshock.recipe.ThermalConverterRecipe;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ThermalConverterRecipeBuilder implements RecipeBuilder {
    private final List<ThermalConverterRecipe.InputItem> itemInputs = new ArrayList<>();
    private final List<ThermalConverterRecipe.InputFluid> fluidInputs = new ArrayList<>();
    private final List<ThermalConverterRecipe.OutputItem> itemOutputs = new ArrayList<>();
    private final List<ThermalConverterRecipe.OutputFluid> fluidOutputs = new ArrayList<>();
    private int processTime = 100;
    private int minHeat = Integer.MIN_VALUE;
    private int maxHeat = Integer.MAX_VALUE;

    public static ThermalConverterRecipeBuilder create() {
        return new ThermalConverterRecipeBuilder();
    }

    public ThermalConverterRecipeBuilder inputItem(Ingredient ingredient, int count, float chance) {
        itemInputs.add(new ThermalConverterRecipe.InputItem(ingredient, count, chance));
        return this;
    }

    public ThermalConverterRecipeBuilder inputFluid(FluidStack fluid, float chance) {
        fluidInputs.add(new ThermalConverterRecipe.InputFluid(fluid, chance));
        return this;
    }

    public ThermalConverterRecipeBuilder outputItem(ItemStack stack, float chance) {
        itemOutputs.add(new ThermalConverterRecipe.OutputItem(stack, chance));
        return this;
    }

    public ThermalConverterRecipeBuilder outputFluid(FluidStack fluid, float chance) {
        fluidOutputs.add(new ThermalConverterRecipe.OutputFluid(fluid, chance));
        return this;
    }

    public ThermalConverterRecipeBuilder time(int ticks) {
        this.processTime = ticks;
        return this;
    }

    public ThermalConverterRecipeBuilder minHeat(int heat) {
        this.minHeat = heat;
        return this;
    }

    public ThermalConverterRecipeBuilder maxHeat(int heat) {
        this.maxHeat = heat;
        return this;
    }

    @Override
    public @NotNull RecipeBuilder unlockedBy(String criterionName, net.minecraft.advancements.Criterion<?> criterion) { return this; }

    @Override
    public @NotNull RecipeBuilder group(@Nullable String groupName) { return this; }

    @Override
    public @NotNull Item getResult() { return net.minecraft.world.item.Items.AIR; }

    @Override
    public void save(RecipeOutput output, ResourceLocation id) {
        output.accept(id, new ThermalConverterRecipe(itemInputs, fluidInputs, itemOutputs, fluidOutputs, processTime, minHeat, maxHeat), null);
    }
}