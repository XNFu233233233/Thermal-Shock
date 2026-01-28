package com.xnfu.thermalshock.recipe;

import com.xnfu.thermalshock.block.entity.MachineMode;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;

import java.util.List;

public abstract class AbstractSimulationRecipe implements Recipe<SimulationRecipeInput> {

    protected final List<SimulationIngredient> inputs;
    protected final ItemStack result;

    public AbstractSimulationRecipe(List<SimulationIngredient> inputs, ItemStack result) {
        this.inputs = inputs;
        this.result = result;
    }

    // 核心抽象方法：由子类定义属于哪种机器模式
    public abstract MachineMode getMachineMode();

    public List<SimulationIngredient> getSimulationIngredients() {
        return inputs;
    }

    public ItemStack getResultStack() {
        return result;
    }

    // 通用 Recipe 实现
    @Override
    public boolean matches(SimulationRecipeInput input, Level level) {
        return !inputs.isEmpty();
    }

    @Override
    public ItemStack assemble(SimulationRecipeInput input, HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        for (SimulationIngredient simIng : inputs) {
            list.add(simIng.ingredient());
        }
        return list;
    }
}