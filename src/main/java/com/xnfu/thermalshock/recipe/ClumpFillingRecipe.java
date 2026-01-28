package com.xnfu.thermalshock.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.xnfu.thermalshock.data.ClumpInfo;
import com.xnfu.thermalshock.registries.ThermalShockDataComponents;
import com.xnfu.thermalshock.registries.ThermalShockItems;
import com.xnfu.thermalshock.registries.ThermalShockRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;

import java.util.List;

public class ClumpFillingRecipe implements CraftingRecipe {
    final String group;
    final CraftingBookCategory category;
    final NonNullList<Ingredient> ingredients;
    final ItemStack resultItem;
    final int minTemp;
    final int heatCost;

    public ClumpFillingRecipe(String group, CraftingBookCategory category, List<Ingredient> ingredients, ItemStack resultItem, int minTemp, int heatCost) {
        this.group = group;
        this.category = category;
        this.ingredients = NonNullList.copyOf(ingredients);
        this.resultItem = resultItem;
        this.minTemp = minTemp;
        this.heatCost = heatCost;
    }

    @Override
    public boolean matches(CraftingInput input, net.minecraft.world.level.Level level) {
        boolean hasClump = false;
        java.util.List<Ingredient> remaining = new java.util.ArrayList<>(ingredients);

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;

            if (stack.is(ThermalShockItems.MATERIAL_CLUMP.get()) && !stack.has(ThermalShockDataComponents.TARGET_OUTPUT)) {
                if (hasClump) return false;
                hasClump = true;
            } else {
                boolean matched = false;
                for (int j = 0; j < remaining.size(); j++) {
                    if (remaining.get(j).test(stack)) {
                        remaining.remove(j);
                        matched = true;
                        break;
                    }
                }
                if (!matched) return false;
            }
        }
        return hasClump && remaining.isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack result = new ItemStack(ThermalShockItems.MATERIAL_CLUMP.get());
        result.set(ThermalShockDataComponents.TARGET_OUTPUT, new ClumpInfo(resultItem, minTemp, heatCost));
        return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= ingredients.size() + 1;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return new ItemStack(ThermalShockItems.MATERIAL_CLUMP.get());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ThermalShockRecipes.CLUMP_FILLING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return RecipeType.CRAFTING;
    }

    @Override
    public CraftingBookCategory category() {
        return category;
    }

    @Override
    public String getGroup() {
        return group;
    }

    // 必须定义自己的 Serializer，不能用 SimpleCraftingRecipeSerializer，因为我们有自定义字段
    public static class Serializer implements RecipeSerializer<ClumpFillingRecipe> {
        public static final MapCodec<ClumpFillingRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(r -> r.group),
                CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC).forGetter(r -> r.category),
                Ingredient.CODEC_NONEMPTY.listOf().fieldOf("ingredients").forGetter(r -> r.ingredients),
                ItemStack.CODEC.fieldOf("result_item").forGetter(r -> r.resultItem),
                Codec.INT.fieldOf("min_temp").forGetter(r -> r.minTemp),
                Codec.INT.fieldOf("heat_cost").forGetter(r -> r.heatCost)
        ).apply(inst, ClumpFillingRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, ClumpFillingRecipe> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, r -> r.group,
                CraftingBookCategory.STREAM_CODEC, r -> r.category,
                Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()), r -> r.ingredients,
                ItemStack.STREAM_CODEC, r -> r.resultItem,
                ByteBufCodecs.VAR_INT, r -> r.minTemp,
                ByteBufCodecs.VAR_INT, r -> r.heatCost,
                ClumpFillingRecipe::new
        );

        @Override
        public MapCodec<ClumpFillingRecipe> codec() { return CODEC; }
        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ClumpFillingRecipe> streamCodec() { return STREAM_CODEC; }
    }
}