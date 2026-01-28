package com.xnfu.thermalshock.datagen;

import com.xnfu.thermalshock.recipe.ThermalFuelRecipe;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.Nullable;

public class ThermalFuelRecipeBuilder implements RecipeBuilder {
    private final Ingredient ingredient;
    private final int burnTime;
    private final int heatRate;

    public ThermalFuelRecipeBuilder(Ingredient ingredient, int burnTime, int heatRate) {
        this.ingredient = ingredient;
        this.burnTime = burnTime;
        this.heatRate = heatRate;
    }

    public static ThermalFuelRecipeBuilder fuel(Ingredient ingredient, int burnTime, int heatRate) {
        return new ThermalFuelRecipeBuilder(ingredient, burnTime, heatRate);
    }

    @Override
    public RecipeBuilder unlockedBy(String criterionName, net.minecraft.advancements.Criterion<?> criterion) { return this; }

    @Override
    public RecipeBuilder group(@Nullable String groupName) { return this; }

    @Override
    public Item getResult() { return net.minecraft.world.item.Items.AIR; }

    @Override
    public void save(RecipeOutput output, ResourceLocation id) {
        output.accept(id, new ThermalFuelRecipe(ingredient, burnTime, heatRate), null);
    }
}