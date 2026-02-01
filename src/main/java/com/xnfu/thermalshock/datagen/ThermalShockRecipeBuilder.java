package com.xnfu.thermalshock.datagen;

import com.xnfu.thermalshock.block.entity.MachineMode;
import com.xnfu.thermalshock.recipe.*;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ThermalShockRecipeBuilder implements RecipeBuilder {
    private final ItemStack result;
    private final MachineMode mode;
    private final List<SimulationIngredient> ingredients = new ArrayList<>();

    private int minHeatRate = 0;
    private int heatCost = 0;
    private int minHotTemp = 0;
    private int maxColdTemp = 0;
    private int requiredDelta = 0;

    private ThermalShockRecipeBuilder(MachineMode mode, ItemStack result) {
        this.mode = mode;
        this.result = result;
    }

    public static ThermalShockRecipeBuilder overheating(ItemStack result) {
        return new ThermalShockRecipeBuilder(MachineMode.OVERHEATING, result);
    }
    public static ThermalShockRecipeBuilder overheating(Item result, int count) {
        return overheating(new ItemStack(result, count));
    }

    public static ThermalShockRecipeBuilder thermalShock(ItemStack result) {
        return new ThermalShockRecipeBuilder(MachineMode.THERMAL_SHOCK, result);
    }
    public static ThermalShockRecipeBuilder thermalShock(Item result, int count) {
        return thermalShock(new ItemStack(result, count));
    }

    public ThermalShockRecipeBuilder inputBlock(Ingredient ingredient) {
        long count = this.ingredients.stream().filter(i -> i.type() == RecipeSourceType.BLOCK).count();
        if (count >= 3) throw new IllegalStateException("Max 3 block inputs allowed!");
        this.ingredients.add(new SimulationIngredient(ingredient, RecipeSourceType.BLOCK));
        return this;
    }

    public ThermalShockRecipeBuilder inputItem(Ingredient ingredient) {
        long count = this.ingredients.stream().filter(i -> i.type() == RecipeSourceType.ITEM).count();
        if (count >= 3) throw new IllegalStateException("Max 3 item inputs allowed!");
        this.ingredients.add(new SimulationIngredient(ingredient, RecipeSourceType.ITEM));
        return this;
    }

    public ThermalShockRecipeBuilder setOverheatingParams(int minHeatRate, int heatCost) {
        if (mode != MachineMode.OVERHEATING) throw new IllegalStateException("Invalid mode");
        this.minHeatRate = minHeatRate;
        this.heatCost = heatCost;
        return this;
    }

    public ThermalShockRecipeBuilder setThermalShockParams(int minHotTemp, int maxColdTemp, int requiredDelta) {
        if (mode != MachineMode.THERMAL_SHOCK) throw new IllegalStateException("Invalid mode");
        this.minHotTemp = minHotTemp;
        this.maxColdTemp = maxColdTemp;
        this.requiredDelta = requiredDelta;
        return this;
    }

    @Override public ThermalShockRecipeBuilder unlockedBy(String c, net.minecraft.advancements.Criterion<?> t) { return this; }
    @Override public ThermalShockRecipeBuilder group(@Nullable String g) { return this; }
    @Override public Item getResult() { return result.getItem(); }

    @Override
    public void save(RecipeOutput output, ResourceLocation id) {
        // [修改] 移除对 BLOCK 类型必须是 BlockItem 的强校验
        // 因为我们现在允许流体桶作为 BLOCK 类型的输入 (代表世界中的流体方块)

        // [核心修改] 根据模式生成不同的配方对象 (对应的 Serializer 会负责写出干净的 JSON)
        if (mode == MachineMode.OVERHEATING) {
            OverheatingRecipe recipe = new OverheatingRecipe(ingredients, result, minHeatRate, heatCost);
            output.accept(id, recipe, null);
        } else {
            ThermalShockRecipe recipe = new ThermalShockRecipe(ingredients, result, minHotTemp, maxColdTemp, requiredDelta);
            output.accept(id, recipe, null);
        }
    }
}