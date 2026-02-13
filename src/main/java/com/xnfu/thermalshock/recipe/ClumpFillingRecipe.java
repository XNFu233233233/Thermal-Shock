package com.xnfu.thermalshock.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.xnfu.thermalshock.data.ClumpInfo;
import com.xnfu.thermalshock.registries.ThermalShockDataComponents;
import com.xnfu.thermalshock.registries.ThermalShockItems;
import com.xnfu.thermalshock.registries.ThermalShockRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import org.jetbrains.annotations.NotNull;

public class ClumpFillingRecipe implements CraftingRecipe {
    final String group;
    final CraftingBookCategory category;
    final ShapedRecipePattern pattern;
    final ItemStack resultItem;

    public ClumpFillingRecipe(String group, CraftingBookCategory category, ShapedRecipePattern pattern, ItemStack resultItem) {
        this.group = group;
        this.category = category;
        this.pattern = pattern;
        this.resultItem = resultItem;
    }

    public ShapedRecipePattern getPattern() { return pattern; }
    public ItemStack getResultTemplate() { return resultItem; }

    @Override
    public boolean matches(CraftingInput input, net.minecraft.world.level.Level level) {
        return this.pattern.matches(input);
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack result = new ItemStack(ThermalShockItems.MATERIAL_CLUMP.get());
        result.set(ThermalShockDataComponents.TARGET_OUTPUT, new ClumpInfo(resultItem));
        return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= this.pattern.width() && height >= this.pattern.height();
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        ItemStack stack = new ItemStack(ThermalShockItems.MATERIAL_CLUMP.get());
        stack.set(ThermalShockDataComponents.TARGET_OUTPUT, new ClumpInfo(resultItem));
        return stack;
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

    public static class Serializer implements RecipeSerializer<ClumpFillingRecipe> {
        public static final MapCodec<ClumpFillingRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(r -> r.group),
                CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC).forGetter(r -> r.category),
                ShapedRecipePattern.MAP_CODEC.forGetter(r -> r.pattern),
                ItemStack.CODEC.fieldOf("result_item").forGetter(r -> r.resultItem)
        ).apply(inst, ClumpFillingRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, ClumpFillingRecipe> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, r -> r.group,
                CraftingBookCategory.STREAM_CODEC, r -> r.category,
                ShapedRecipePattern.STREAM_CODEC, r -> r.pattern,
                ItemStack.STREAM_CODEC, r -> r.resultItem,
                ClumpFillingRecipe::new
        );

        @Override
        public @NotNull MapCodec<ClumpFillingRecipe> codec() { return CODEC; }
        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, ClumpFillingRecipe> streamCodec() { return STREAM_CODEC; }
    }
}
