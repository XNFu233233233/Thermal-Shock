package com.xnfu.thermalshock.datagen;

import com.xnfu.thermalshock.recipe.RecipeSourceType;
import com.xnfu.thermalshock.recipe.SimulationIngredient;
import com.xnfu.thermalshock.recipe.ThermalShockFillingRecipe;
import com.xnfu.thermalshock.registries.ThermalShockItems;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ThermalShockFillingRecipeBuilder implements RecipeBuilder {
    private final Ingredient inputIngredient;
    private final net.minecraft.core.Holder<Item> targetItem;
    private final int targetCount;
    private final int minHot;
    private final int maxCold;
    private final int delta;

    public ThermalShockFillingRecipeBuilder(Ingredient input, net.minecraft.core.Holder<Item> targetItem, int targetCount, int minHot, int maxCold, int delta) {
        this.inputIngredient = input;
        this.targetItem = targetItem;
        this.targetCount = targetCount;
        this.minHot = minHot;
        this.maxCold = maxCold;
        this.delta = delta;
    }

    @Override
    public RecipeBuilder unlockedBy(String criterionName, net.minecraft.advancements.Criterion<?> criterion) { return this; }

    @Override
    public RecipeBuilder group(@Nullable String groupName) { return this; }

    @Override
    public Item getResult() { return ThermalShockItems.MATERIAL_CLUMP.get(); }

    @Override
    public void save(RecipeOutput output, ResourceLocation id) {
        List<SimulationIngredient> inputs = List.of(
                new SimulationIngredient(inputIngredient, RecipeSourceType.ITEM),
                new SimulationIngredient(Ingredient.of(ThermalShockItems.MATERIAL_CLUMP.get()), RecipeSourceType.ITEM)
        );

        ThermalShockFillingRecipe recipe = new ThermalShockFillingRecipe(
                inputs, targetItem, targetCount, minHot, maxCold, delta
        );
        output.accept(id, recipe, null);
    }
}
