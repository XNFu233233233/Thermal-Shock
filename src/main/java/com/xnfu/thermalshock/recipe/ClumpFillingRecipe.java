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

import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;

import net.minecraft.core.registries.BuiltInRegistries;

public class ClumpFillingRecipe implements CraftingRecipe {
    final String group;
    final CraftingBookCategory category;
    final ShapedRecipePattern pattern;
    final Holder<Item> targetItem;
    private final ClumpInfo cachedInfo;

    public ClumpFillingRecipe(String group, CraftingBookCategory category, ShapedRecipePattern pattern, Holder<Item> targetItem) {
        this.group = group;
        this.category = category;
        this.pattern = pattern;
        this.targetItem = targetItem;
        this.cachedInfo = new ClumpInfo(targetItem, 1);
    }

    @Override public String getGroup() { return group; }
    public CraftingBookCategory getCategory() { return category; }
    public ShapedRecipePattern getPattern() { return pattern; }
    public Holder<Item> getTargetItem() { return targetItem; }

    @Override
    public boolean matches(CraftingInput input, net.minecraft.world.level.Level level) {
        return this.pattern.matches(input);
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack result = new ItemStack(ThermalShockItems.MATERIAL_CLUMP.get());
        result.set(ThermalShockDataComponents.TARGET_OUTPUT, cachedInfo);
        return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= this.pattern.width() && height >= this.pattern.height();
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        ItemStack stack = new ItemStack(ThermalShockItems.MATERIAL_CLUMP.get());
        stack.set(ThermalShockDataComponents.TARGET_OUTPUT, cachedInfo);
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

    public static class Serializer implements RecipeSerializer<ClumpFillingRecipe> {
        public static final MapCodec<ClumpFillingRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(ClumpFillingRecipe::getGroup),
                CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC).forGetter(ClumpFillingRecipe::getCategory),
                ShapedRecipePattern.MAP_CODEC.forGetter(ClumpFillingRecipe::getPattern),
                BuiltInRegistries.ITEM.holderByNameCodec().fieldOf("target_item").forGetter(ClumpFillingRecipe::getTargetItem)
        ).apply(inst, ClumpFillingRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, ClumpFillingRecipe> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, r -> r.group,
                CraftingBookCategory.STREAM_CODEC, r -> r.category,
                ShapedRecipePattern.STREAM_CODEC, r -> r.pattern,
                ByteBufCodecs.holderRegistry(net.minecraft.core.registries.Registries.ITEM), r -> r.targetItem,
                ClumpFillingRecipe::new
        );

        @Override
        public @NotNull MapCodec<ClumpFillingRecipe> codec() { return CODEC; }
        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, ClumpFillingRecipe> streamCodec() { return STREAM_CODEC; }
    }
}
