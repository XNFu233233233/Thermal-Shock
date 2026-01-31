package com.xnfu.thermalshock.compat.jei.category;

import com.xnfu.thermalshock.recipe.ThermalFuelRecipe;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class ThermalFuelCategory implements IRecipeCategory<ThermalFuelRecipe> {
    private final RecipeType<ThermalFuelRecipe> type;
    private final Component title;
    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable flame;
    private final boolean isHeater;

    public ThermalFuelCategory(IGuiHelper helper, RecipeType<ThermalFuelRecipe> type, String titleKey, ItemStack iconStack, boolean isHeater) {
        this.type = type;
        this.title = Component.translatable(titleKey);
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, iconStack);
        this.background = helper.createBlankDrawable(160, 50); // Widen
        this.flame = helper.createDrawable(ResourceLocation.withDefaultNamespace("textures/gui/container/furnace.png"), 176, 0, 14, 14);
        this.isHeater = isHeater;
    }

    @Override
    public @NotNull RecipeType<ThermalFuelRecipe> getRecipeType() {
        return type;
    }

    @Override
    public @NotNull Component getTitle() {
        return title;
    }

    @Override
    public @NotNull IDrawable getIcon() {
        return icon;
    }

    @Override public int getWidth() { return 160; }
    @Override public int getHeight() { return 50; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ThermalFuelRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 5, 17)
                .addIngredients(recipe.getIngredient());
    }

    @Override
    public void draw(ThermalFuelRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics gfx, double mouseX, double mouseY) {
        // 1. 绘制背景填充 (遮盖旧像素，防止文字重叠)
        gfx.fill(0, 0, 160, 50, 0xFFC6C6C6);
        background.draw(gfx);
        flame.draw(gfx, 28, 18);

        var font = Minecraft.getInstance().font;
        String timeStr = String.format("%.2f", recipe.getBurnTime() / 20.0f);
        // FIX: Remove redundant 'H' as it should be in the translation key
        String rateStr = (recipe.getHeatRate() > 0 ? "+" : "") + recipe.getHeatRate();

        // 2. 绘制文字 (禁用阴影，解决“重叠看不清”的问题)
        gfx.drawString(font, Component.translatable("gui.thermalshock.source.duration", timeStr).getString(), 48, 12, 0xFF404040, false);
        gfx.drawString(font, Component.translatable("gui.thermalshock.source.jei_output", rateStr).getString(), 48, 24, isHeater ? 0xFFFF5555 : 0xFF55FFFF, false);
    }
}
