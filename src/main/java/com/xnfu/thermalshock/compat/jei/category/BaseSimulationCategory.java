package com.xnfu.thermalshock.compat.jei.category;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseSimulationCategory<T> implements IRecipeCategory<T> {
    protected final IDrawable background;
    protected IDrawable icon;
    protected final Component title;
    
    // 槽位颜色常量
    protected static final int COLOR_BLOCK = 0xFF606060;
    protected static final int COLOR_ITEM = 0xFF008080;
    protected static final int COLOR_OUTPUT = 0xFF373737;
    protected static final int COLOR_SLOT_BG = 0xFF8B8B8B;
    protected static final int COLOR_SPECIAL = 0xFFD4AF37; // 金色/特殊的槽位
    
    protected final int width;
    protected final int height;

    protected record SlotHint(int x, int y, Component hint) {
        public boolean isMouseOver(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 18;
        }
    }
    protected final List<SlotHint> slotHints = new ArrayList<>();

    public BaseSimulationCategory(IGuiHelper helper, ItemStack iconStack, Component title, int width, int height) {
        this.title = title;
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, iconStack);
        this.width = width;
        this.height = height;
        this.background = helper.createBlankDrawable(width, height);
    }

    @Override public @NotNull Component getTitle() { return title; }
    @Override public @NotNull IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return width; }
    @Override public int getHeight() { return height; }

    protected void drawSlot(GuiGraphics gfx, int x, int y, int color) {
        // 描边 18x18
        gfx.fill(x, y, x + 18, y + 18, color);
        // 背景 16x16
        gfx.fill(x + 1, y + 1, x + 17, y + 17, COLOR_SLOT_BG);
    }

    protected void drawBackground(GuiGraphics gfx) {
        // 绘制灰色背景
        gfx.fill(0, 0, width, height, 0xFFC6C6C6);
        // 绘制阴影外框
        gfx.fill(0, 0, width, 1, 0xFFFFFFFF);
        gfx.fill(0, 0, 1, height, 0xFFFFFFFF);
        gfx.fill(width - 1, 0, width, height, 0xFF555555);
        gfx.fill(0, height - 1, width, height, 0xFF555555);
    }

    protected IRecipeSlotBuilder addSlotWithTooltip(IRecipeLayoutBuilder builder, RecipeIngredientRole role, int x, int y, Component hint) {
        // 有物品的槽位：注册到 JEI，并使用回调。JEI 此时会阻断 getTooltipStrings 的调用。
        return builder.addSlot(role, x + 1, y + 1)
            .addRichTooltipCallback((slotView, tooltip) -> {
                tooltip.add(hint);
            });
    }

    protected void addEmptySlotWithTooltip(IRecipeLayoutBuilder builder, RecipeIngredientRole role, int x, int y, Component hint) {
        // 空槽位：不注册到 JEI，由 BaseSimulationCategory 统一处理坐标判定（解决 16x16 中心不触发问题）。
        slotHints.add(new SlotHint(x, y, hint));
    }

    @Override
    public @NotNull List<Component> getTooltipStrings(T recipe, IRecipeSlotsView recipeSlots, double mouseX, double mouseY) {
        List<Component> tooltips = new ArrayList<>();
        for (SlotHint hint : slotHints) {
            if (hint.isMouseOver(mouseX, mouseY)) {
                tooltips.add(hint.hint);
                break;
            }
        }
        return tooltips;
    }

    protected void drawHints(GuiGraphics gfx, double mouseX, double mouseY) {
        for (SlotHint hint : slotHints) {
            if (hint.isMouseOver(mouseX, mouseY)) {
                // JEI 风格高亮：白色半透明叠层
                gfx.fill(hint.x + 1, hint.y + 1, hint.x + 17, hint.y + 17, 0x50FFFFFF);
                break;
            }
        }
    }

    protected void drawHeading(GuiGraphics gfx, String text, int y) {
        var font = Minecraft.getInstance().font;
        gfx.drawString(font, text, 88 - font.width(text) / 2, y, 0xFF404040, false);
    }

    protected void drawArrow(GuiGraphics gfx, int x, int y) {
        // 使用简单的多边形模拟原版箭头，更清爽
        gfx.fill(x, y + 4, x + 12, y + 10, 0xFF7E7E7E);
        gfx.fill(x + 12, y, x + 14, y + 14, 0xFF7E7E7E);
        gfx.fill(x + 14, y + 2, x + 16, y + 12, 0xFF7E7E7E);
        gfx.fill(x + 16, y + 4, x + 18, y + 10, 0xFF7E7E7E);
    }

    protected void setIconWithOverlay(mezz.jei.api.helpers.IGuiHelper helper, ItemStack base, ItemStack overlay) {
        this.icon = new IDrawable() {
            @Override public int getWidth() { return 16; }
            @Override public int getHeight() { return 16; }
            @Override
            public void draw(@NotNull GuiGraphics gfx, int x, int y) {
                gfx.renderFakeItem(base, x, y);
                gfx.pose().pushPose();
                gfx.pose().translate(x + 8, y + 8, 200);
                gfx.pose().scale(0.5f, 0.5f, 0.5f);
                gfx.renderFakeItem(overlay, 0, 0);
                gfx.pose().popPose();
            }
        };
    }
}
