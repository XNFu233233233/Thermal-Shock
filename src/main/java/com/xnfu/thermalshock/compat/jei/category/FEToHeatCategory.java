package com.xnfu.thermalshock.compat.jei.category;

import com.xnfu.thermalshock.ThermalShock;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * 专门展示 FE -> Heat 的静态分类
 */
public class FEToHeatCategory implements IRecipeCategory<FEToHeatCategory.FEToHeatRecipe> {
    public static final RecipeType<FEToHeatRecipe> TYPE = RecipeType.create(ThermalShock.MODID, "fe_to_heat", FEToHeatRecipe.class);

    private final Component title;
    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawableAnimated arrow;

    public record FEToHeatRecipe(int fePerTick, int heatPerTick) {}

    public FEToHeatCategory(IGuiHelper helper, ItemStack iconStack) {
        this.title = Component.translatable("gui.thermalshock.jei.category.fe_to_heat").withStyle(ChatFormatting.GOLD);
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, iconStack);
        this.background = helper.createBlankDrawable(160, 50);

        ResourceLocation widgets = ResourceLocation.fromNamespaceAndPath(ThermalShock.MODID, "textures/gui/widgets.png");
        IDrawableStatic staticArrow = helper.createDrawable(widgets, 0, 0, 24, 17);
        this.arrow = helper.createAnimatedDrawable(staticArrow, 200, IDrawableAnimated.StartDirection.LEFT, false);
    }

    @Override public @NotNull RecipeType<FEToHeatRecipe> getRecipeType() { return TYPE; }
    @Override public @NotNull Component getTitle() { return title; }
    @Override public @NotNull IDrawable getIcon() { return icon; }

    @Override public int getWidth() { return 160; }
    @Override public int getHeight() { return 50; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, FEToHeatRecipe recipe, IFocusGroup focuses) {
        // 这是一个展示性页面，没有实际槽位
    }

    @Override
    public void draw(FEToHeatRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics gfx, double mouseX, double mouseY) {
        // 1. 绘制背景填充 (遮盖像素，防止重叠)
        gfx.fill(0, 0, 160, 50, 0xFFC6C6C6);
        background.draw(gfx);
        arrow.draw(gfx, 68, 18);
        
        var font = Minecraft.getInstance().font;
        String feStr = recipe.fePerTick + " FE/t";
        String heatStr = (recipe.heatPerTick > 0 ? "+" : "") + recipe.heatPerTick + " H/t";
        String titleStr = Component.translatable("gui.thermalshock.jei.fe_conversion").getString();

        // 2. 绘制文字 (清除阴影)
        gfx.drawString(font, feStr, 35 - font.width(feStr) / 2, 22, 0xFF00AAAA, false);
        gfx.drawString(font, heatStr, 125 - font.width(heatStr) / 2, 22, 0xFFFF5555, false);
        gfx.drawString(font, titleStr, 80 - font.width(titleStr) / 2, 5, 0xFF404040, false);
    }
}
