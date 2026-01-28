package com.xnfu.thermalshock.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

public record SimulationRecipeInput(ItemStack ingredient, RecipeSourceType type) implements RecipeInput {
    @Override
    public ItemStack getItem(int index) {
        return index == 0 ? ingredient : ItemStack.EMPTY;
    }
    @Override
    public int size() { return 1; }
}