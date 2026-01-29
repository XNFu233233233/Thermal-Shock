package com.xnfu.thermalshock.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;

public record ConverterRecipeInput(ItemStack item, FluidStack fluid) implements RecipeInput {
    @Override
    public @NotNull ItemStack getItem(int index) {
        return index == 0 ? item : ItemStack.EMPTY;
    }
    @Override
    public int size() { return 1; }
}