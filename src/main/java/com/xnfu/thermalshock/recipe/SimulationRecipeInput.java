package com.xnfu.thermalshock.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

import java.util.List;

/**
 * 模拟室配方输入。
 * 包含：
 * 1. 核心触发物品 (primary)
 * 2. 机器内部当前缓存的所有材料 (池)
 */
public record SimulationRecipeInput(ItemStack primary, List<ItemStack> poolIngredients, List<RecipeSourceType> poolTypes) implements RecipeInput {
    
    @Override
    public ItemStack getItem(int index) {
        if (index == 0) return primary;
        if (index - 1 < poolIngredients.size()) return poolIngredients.get(index - 1);
        return ItemStack.EMPTY;
    }

    @Override
    public int size() {
        return 1 + poolIngredients.size();
    }
    
    // 快捷构造辅助
    public static SimulationRecipeInput of(ItemStack primary, List<ItemStack> poolIngredients, List<RecipeSourceType> poolTypes) {
        return new SimulationRecipeInput(primary, poolIngredients, poolTypes);
    }
}