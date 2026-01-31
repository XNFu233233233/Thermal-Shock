package com.xnfu.thermalshock.compat.jei.category;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public abstract class DataMapCategory<T> implements IRecipeCategory<T> {
    protected final RecipeType<T> type;
    protected final Component title;
    protected final IDrawable background;
    protected final IDrawable icon;

    public DataMapCategory(IGuiHelper helper, RecipeType<T> type, String titleKey, ItemStack iconStack) {
        this.type = type;
        this.title = Component.translatable(titleKey).withStyle(ChatFormatting.GRAY);
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, iconStack);
        this.background = helper.createBlankDrawable(150, 40);
    }

    @Override public @NotNull RecipeType<T> getRecipeType() { return type; }
    @Override public @NotNull Component getTitle() { return title; }
    @Override public @NotNull IDrawable getIcon() { return icon; }

    @Override public int getWidth() { return 150; }
    @Override public int getHeight() { return 40; }

    @Override
    public void draw(T recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics gfx, double mouseX, double mouseY) {
        background.draw(gfx);
    }
}
