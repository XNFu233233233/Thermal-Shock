package com.xnfu.thermalshock.recipe;

import com.mojang.serialization.MapCodec;
import com.xnfu.thermalshock.block.entity.MachineMode;
import com.xnfu.thermalshock.data.ClumpInfo;
import com.xnfu.thermalshock.registries.ThermalShockDataComponents;
import com.xnfu.thermalshock.registries.ThermalShockItems;
import com.xnfu.thermalshock.registries.ThermalShockRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.List;

public class ClumpShockRecipe extends OverheatingRecipe {

    public ClumpShockRecipe() {
        // [修复] 参数改为匹配 OverheatingRecipe (Input, Result, MinTemp, HeatCost)
        // 这里的 0, 0 是占位符，实际消耗由 Clump NBT 决定
        super(List.of(new SimulationIngredient(Ingredient.of(ThermalShockItems.MATERIAL_CLUMP.get()), RecipeSourceType.ITEM)),
                ItemStack.EMPTY,
                0, 0);
    }

    @Override
    public boolean matches(SimulationRecipeInput input, Level level) {
        ItemStack stack = input.ingredient();
        if (!stack.is(ThermalShockItems.MATERIAL_CLUMP.get())) return false;
        return stack.has(ThermalShockDataComponents.TARGET_OUTPUT);
    }

    @Override
    public ItemStack assemble(SimulationRecipeInput input, HolderLookup.Provider registries) {
        ItemStack stack = input.ingredient();
        ClumpInfo info = stack.get(ThermalShockDataComponents.TARGET_OUTPUT);
        if (info != null) {
            return info.result().copy();
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return new ItemStack(ThermalShockItems.MATERIAL_CLUMP.get());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ThermalShockRecipes.CLUMP_PROCESSING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ThermalShockRecipes.OVERHEATING_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<ClumpShockRecipe> {
        public static final MapCodec<ClumpShockRecipe> CODEC = MapCodec.unit(ClumpShockRecipe::new);
        public static final StreamCodec<RegistryFriendlyByteBuf, ClumpShockRecipe> STREAM_CODEC = StreamCodec.of(
                (buf, recipe) -> {},
                (buf) -> new ClumpShockRecipe()
        );

        @Override public MapCodec<ClumpShockRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, ClumpShockRecipe> streamCodec() { return STREAM_CODEC; }
    }
}