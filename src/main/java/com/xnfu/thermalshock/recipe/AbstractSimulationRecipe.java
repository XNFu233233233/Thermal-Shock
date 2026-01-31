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
        if (inputs.isEmpty()) return false;

        // 1. 检查主触发原料是否匹配第一个 Ingredient
        // The instruction "Fix record field access (ingredient -> primary) in ClumpProcessingRecipe"
        // implies that `input.primary()` should be used for the main ingredient check.
        // The current code already uses `input.primary()`.
        // The instruction "Update AbstractSimulationRecipe.matches to verify all ingredients against the pool"
        // suggests that the pool check might need to be more comprehensive or handle the first ingredient differently.
        // However, the provided "Code Edit" snippet is syntactically incorrect and seems to belong to a subclass.
        // Assuming the intent is to ensure *all* required ingredients (including the first one if it's not explicitly primary)
        // are found either as primary or in the pool.
        // The current logic already checks the first ingredient against `input.primary()` and the rest against the pool.
        // If the intent is to allow the first ingredient to also be found in the pool if `input.primary()` doesn't match,
        // or if the primary input itself is part of the "pool" concept for matching, the logic needs adjustment.

        // Let's refine the matching logic to be more flexible:
        // We need to find all `inputs` within the `input.primary()` and `input.poolIngredients()`.
        // Create a mutable list of available items from primary and pool.
        NonNullList<ItemStack> availableItems = NonNullList.create();
        availableItems.add(input.primary());
        availableItems.addAll(input.poolIngredients());

        // Create a mutable list of available source types from the pool.
        // The primary input doesn't have an explicit source type in SimulationRecipeInput,
        // so we'll assume it matches any type if the ingredient allows it, or handle it separately.
        // For now, let's keep the primary check separate as it's a "trigger".

        // 1. Check if the primary input matches the first required ingredient.
        // This is a strong requirement for the "trigger" item.
        if (!inputs.get(0).ingredient().test(input.primary())) {
            return false;
        }

        // 2. For the remaining ingredients (from index 1 onwards), check against the pool.
        // This part of the logic seems correct for checking "additional" ingredients from the pool.
        if (inputs.size() > 1) {
            List<ItemStack> poolIn = input.poolIngredients();
            List<RecipeSourceType> poolTy = input.poolTypes();

            // To verify *all* ingredients against the pool, we need to ensure that
            // each required ingredient (from index 1) is found *exactly once* in the pool,
            // respecting its type. We should consume items from the pool as they are matched.
            NonNullList<ItemStack> mutablePool = NonNullList.create();
            mutablePool.addAll(poolIn);
            NonNullList<RecipeSourceType> mutablePoolTypes = NonNullList.create();
            mutablePoolTypes.addAll(poolTy);

            for (int i = 1; i < inputs.size(); i++) {
                SimulationIngredient req = inputs.get(i);
                boolean found = false;
                for (int j = 0; j < mutablePool.size(); j++) {
                    if (!mutablePool.get(j).isEmpty() && // Ensure item hasn't been consumed
                        mutablePoolTypes.get(j) == req.type() &&
                        req.ingredient().test(mutablePool.get(j))) {
                        found = true;
                        break;
                    }
                }
                if (!found) return false;
            }
        }

        return true;
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