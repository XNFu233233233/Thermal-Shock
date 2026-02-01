package com.xnfu.thermalshock.compat.jei.category;

import com.xnfu.thermalshock.recipe.RecipeSourceType;
import com.xnfu.thermalshock.recipe.SimulationIngredient;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract class BaseSimulationCategory<T> implements IRecipeCategory<T> {
    
    protected final IDrawable background;
    protected IDrawable icon;
    protected final Component title;
    
    // 槽位颜色常量
    protected static final int COLOR_BLOCK = 0xFF606060;
    protected static final int COLOR_ITEM = 0xFF008080;
    protected static final int COLOR_OUTPUT = 0xFF373737;
    protected static final int COLOR_SLOT_BG = 0xFF8B8B8B;
    protected static final int COLOR_SPECIAL = 0xFFD4AF37;
    
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

    @Override public Component getTitle() { return title; }
    @Override public IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return width; }
    @Override public int getHeight() { return height; }

    protected void drawSlot(GuiGraphics gfx, int x, int y, int color) {
        gfx.fill(x, y, x + 18, y + 18, color);
        gfx.fill(x + 1, y + 1, x + 17, y + 17, COLOR_SLOT_BG);
    }

    protected void drawBackground(GuiGraphics gfx) {
        gfx.fill(0, 0, width, height, 0xFFC6C6C6);
        gfx.fill(0, 0, width, 1, 0xFFFFFFFF);
        gfx.fill(0, 0, 1, height, 0xFFFFFFFF);
        gfx.fill(width - 1, 0, width, height, 0xFF555555);
        gfx.fill(0, height - 1, width, height, 0xFF555555);
    }

    // [新增] 智能添加槽位：如果是 BLOCK 类型且为 Bucket，则渲染流体
    protected void addSimulationSlot(IRecipeLayoutBuilder builder, RecipeIngredientRole role, int x, int y, SimulationIngredient input, Component hint) {
        if (input.type() == RecipeSourceType.BLOCK) {
            // 检查是否为 Bucket
            Fluid fluid = getFluidFromIngredient(input);
            if (fluid != null) {
                // 渲染流体 (1000mB)
                builder.addSlot(role, x + 1, y + 1)
                        .addIngredient(NeoForgeTypes.FLUID_STACK, new FluidStack(fluid, 1000))
                        .setFluidRenderer(1000, false, 16, 16) // 填满 16x16 槽位
                        .addRichTooltipCallback((slotView, tooltip) -> tooltip.add(hint));
                return;
            }
        }
        
        // 默认渲染物品
        builder.addSlot(role, x + 1, y + 1)
                .addIngredients(input.ingredient())
                .addRichTooltipCallback((slotView, tooltip) -> tooltip.add(hint));
    }

    private Fluid getFluidFromIngredient(SimulationIngredient simIng) {
        for (ItemStack stack : simIng.ingredient().getItems()) {
            Optional<FluidStack> fs = FluidUtil.getFluidContained(stack);
            if (fs.isPresent()) return fs.get().getFluid();
        }
        return null;
    }

    protected IRecipeSlotBuilder addSlotWithTooltip(IRecipeLayoutBuilder builder, RecipeIngredientRole role, int x, int y, Component hint) {
        return builder.addSlot(role, x + 1, y + 1)
            .addRichTooltipCallback((slotView, tooltip) -> {
                tooltip.add(hint);
            });
    }

    protected void addEmptySlotWithTooltip(IRecipeLayoutBuilder builder, RecipeIngredientRole role, int x, int y, Component hint) {
        slotHints.add(new SlotHint(x, y, hint));
    }

    @Override
    public List<Component> getTooltipStrings(T recipe, IRecipeSlotsView recipeSlots, double mouseX, double mouseY) {
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
        gfx.fill(x, y + 4, x + 12, y + 10, 0xFF7E7E7E);
        gfx.fill(x + 12, y, x + 14, y + 14, 0xFF7E7E7E);
        gfx.fill(x + 14, y + 2, x + 16, y + 12, 0xFF7E7E7E);
        gfx.fill(x + 16, y + 4, x + 18, y + 10, 0xFF7E7E7E);
    }

    protected void setIconWithOverlay(IGuiHelper helper, ItemStack base, ItemStack overlay) {
        this.icon = new IDrawable() {
            @Override public int getWidth() { return 16; }
            @Override public int getHeight() { return 16; }
            @Override
            public void draw(GuiGraphics gfx, int x, int y) {
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
