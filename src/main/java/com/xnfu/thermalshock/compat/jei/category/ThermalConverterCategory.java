package com.xnfu.thermalshock.compat.jei.category;

import com.xnfu.thermalshock.ThermalShock;
import com.xnfu.thermalshock.recipe.ThermalConverterRecipe;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class ThermalConverterCategory implements IRecipeCategory<ThermalConverterRecipe> {
    public static final RecipeType<ThermalConverterRecipe> TYPE = mezz.jei.api.recipe.RecipeType.create(ThermalShock.MODID, "thermal_converter", ThermalConverterRecipe.class);
    
    // === 布局坐标 (对齐机器 GUI) ===
    private static final int FLUID_L_X = 6;
    private static final int FLUID_L_Y = 10;
    private static final int SLOT_IN_X = 40;
    private static final int SLOT_IN_Y = 25;
    private static final int ARROW_X = 66;
    private static final int ARROW_Y = 26;
    private static final int SLOT_OUT1_X = 98;
    private static final int SLOT_OUT1_Y = 25;
    private static final int SLOT_OUT2_X = 118;
    private static final int SLOT_OUT2_Y = 25;
    private static final int FLUID_R_X = 152;
    private static final int FLUID_R_Y = 10;
    private static final int HEAT_BAR_X = 28;
    private static final int HEAT_BAR_Y = 64;
    private static final int HEAT_BAR_W = 120;
    private static final int HEAT_BAR_H = 10;

    private static final int COLOR_SLOT_BORDER = 0xFF373737;
    private static final int COLOR_SLOT_BG = 0xFF8B8B8B;

    private final Component title;
    private final IDrawable icon;

    public ThermalConverterCategory(IGuiHelper helper, ItemStack iconStack) {
        this.title = Component.translatable("block.thermalshock.thermal_converter").withStyle(ChatFormatting.DARK_GRAY);
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, iconStack);
    }

    @Override public @NotNull RecipeType<ThermalConverterRecipe> getRecipeType() { return TYPE; }
    @Override public @NotNull Component getTitle() { return title; }
    @Override public @NotNull IDrawable getIcon() { return icon; }

    @Override public int getWidth() { return 176; }
    @Override public int getHeight() { return 85; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ThermalConverterRecipe recipe, IFocusGroup focuses) {
        // 输入物品
        if (!recipe.getItemInputs().isEmpty()) {
            var input = recipe.getItemInputs().get(0);
            builder.addSlot(RecipeIngredientRole.INPUT, SLOT_IN_X + 1, SLOT_IN_Y + 1)
                    .addIngredients(input.ingredient())
                    .addRichTooltipCallback((slotView, tooltip) -> {
                        if (input.consumeChance() < 1.0f) {
                            tooltip.add(Component.translatable("gui.thermalshock.jei.chance.consume", (int)(input.consumeChance() * 100)).withStyle(net.minecraft.ChatFormatting.YELLOW));
                        }
                    });
        }

        // 输入流体
        if (!recipe.getFluidInputs().isEmpty()) {
            var fluid = recipe.getFluidInputs().get(0);
            builder.addSlot(RecipeIngredientRole.INPUT, FLUID_L_X + 1, FLUID_L_Y + 1)
                    .setFluidRenderer(fluid.fluid().getAmount(), false, 16, 48)
                    .addIngredient(NeoForgeTypes.FLUID_STACK, fluid.fluid())
                    .addRichTooltipCallback((slotView, tooltip) -> {
                        if (fluid.consumeChance() < 1.0f) {
                            tooltip.add(Component.translatable("gui.thermalshock.jei.chance.consume", (int)(fluid.consumeChance() * 100)).withStyle(net.minecraft.ChatFormatting.YELLOW));
                        }
                    });
        }

        // 输出物品
        for (int i = 0; i < recipe.getItemOutputs().size(); i++) {
            var output = recipe.getItemOutputs().get(i);
            int x = (i == 0) ? SLOT_OUT1_X : SLOT_OUT2_X;
            builder.addSlot(RecipeIngredientRole.OUTPUT, x + 1, SLOT_OUT1_Y + 1)
                    .addItemStack(output.stack())
                    .addRichTooltipCallback((slotView, tooltip) -> {
                        if (output.chance() < 1.0f) {
                            tooltip.add(Component.translatable("gui.thermalshock.jei.chance.output", (int)(output.chance() * 100)).withStyle(net.minecraft.ChatFormatting.GOLD));
                        }
                    });
        }

        // 输出流体
        if (!recipe.getFluidOutputs().isEmpty()) {
            var fluid = recipe.getFluidOutputs().get(0);
            builder.addSlot(RecipeIngredientRole.OUTPUT, FLUID_R_X + 1, FLUID_R_Y + 1)
                    .setFluidRenderer(fluid.fluid().getAmount(), false, 16, 48)
                    .addIngredient(NeoForgeTypes.FLUID_STACK, fluid.fluid())
                    .addRichTooltipCallback((slotView, tooltip) -> {
                        if (fluid.chance() < 1.0f) {
                            tooltip.add(Component.translatable("gui.thermalshock.jei.chance.output", (int)(fluid.chance() * 100)).withStyle(net.minecraft.ChatFormatting.GOLD));
                        }
                    });
        }
    }

    @Override
    public void draw(ThermalConverterRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics gfx, double mouseX, double mouseY) {
        // 1. 绘制背景 UI (确保颜色纯净无残留)
        gfx.fill(0, 0, 176, 85, 0xFFC6C6C6);
        
        drawSlot(gfx, SLOT_IN_X, SLOT_IN_Y);
        drawSlot(gfx, SLOT_OUT1_X, SLOT_OUT1_Y);
        drawSlot(gfx, SLOT_OUT2_X, SLOT_OUT2_Y);
        drawTank(gfx, FLUID_L_X, FLUID_L_Y);
        drawTank(gfx, FLUID_R_X, FLUID_R_Y);

        // 进度条 (ARROW_X, ARROW_Y) - 手动绘制简单的绿色进度条
        drawProgressBar(gfx, ARROW_X, ARROW_Y);

        // 2. 绘制热量条 (与机器 GUI 同步)
        drawHeatBar(gfx, recipe);

        // 3. 绘制文字 (无阴影 clear 模式)
        var font = Minecraft.getInstance().font;
        
        // 底部文字汇总 (0.9x 缩放，禁用阴影)
        String subText = String.format("min: %d H | max: %d H", recipe.getMinHeat(), recipe.getMaxHeat());
        if (recipe.getMinHeat() <= -2000000) subText = String.format("max: %d H", recipe.getMaxHeat());
        else if (recipe.getMaxHeat() >= 2000000) subText = String.format("min: %d H", recipe.getMinHeat());
        
        gfx.pose().pushPose();
        gfx.pose().translate(88, HEAT_BAR_Y - 12, 0);
        gfx.drawString(font, subText, -font.width(subText) / 2, 0, 0xFF404040, false);
        gfx.pose().popPose();

        // 4. 绘制概率百分比 (右上角)
        drawProbabilities(recipe, gfx, font);
    }

    private void drawProgressBar(GuiGraphics gfx, int x, int y) {
        // 背景 24x8
        gfx.fill(x, y + 4, x + 24, y + 12, 0xFF555555);
        
        // 简单动画 (2秒循环)
        long time = System.currentTimeMillis();
        float pct = (time % 2000L) / 2000.0f;
        int w = (int)(pct * 24);
        
        gfx.fill(x, y + 4, x + w, y + 12, 0xFF00FF00);
    }

    private void drawSlot(GuiGraphics gfx, int x, int y) {
        gfx.fill(x, y, x + 18, y + 18, COLOR_SLOT_BORDER);
        gfx.fill(x + 1, y + 1, x + 17, y + 17, COLOR_SLOT_BG);
    }

    private void drawTank(GuiGraphics gfx, int x, int y) {
        gfx.fill(x, y, x + 18, y + 50, COLOR_SLOT_BORDER);
        gfx.fill(x + 1, y + 1, x + 17, y + 49, COLOR_SLOT_BG);
    }

    private void drawHeatBar(GuiGraphics gfx, ThermalConverterRecipe recipe) {
        int minH = recipe.getMinHeat();
        int maxH = recipe.getMaxHeat();
        int maxAbs = Math.max(Math.abs(minH), Math.abs(maxH));

        // 动态调整量程
        int range = 1000;
        if (maxAbs > 5000) range = 10000;
        else if (maxAbs > 1000) range = 5000;
        else if (maxAbs > 100) range = 2000;

        float scale = (HEAT_BAR_W / 2.0f) / range;
        int cx = HEAT_BAR_X + HEAT_BAR_W / 2;

        // 1. 背景底色 (深灰色轨道)
        gfx.fill(HEAT_BAR_X, HEAT_BAR_Y, HEAT_BAR_X + HEAT_BAR_W, HEAT_BAR_Y + HEAT_BAR_H, 0xFF333333);
        
        // 2. 绘制区间填充 (与机器 GUI 颜色同步)
        int xStart = cx + (int)(minH * scale);
        int xEnd = cx + (int)(maxH * scale);
        xStart = Mth.clamp(xStart, HEAT_BAR_X + 1, HEAT_BAR_X + HEAT_BAR_W - 1);
        xEnd = Mth.clamp(xEnd, HEAT_BAR_X + 1, HEAT_BAR_X + HEAT_BAR_W - 1);
        if (xStart > xEnd) { int t = xStart; xStart = xEnd; xEnd = t; }

        if (minH >= 0) {
            // 纯正数区间：红色渐变
            gfx.fillGradient(xStart, HEAT_BAR_Y + 1, xEnd, HEAT_BAR_Y + HEAT_BAR_H - 1, 0xFFFF0000, 0xFFFFAA00);
        } else if (maxH <= 0) {
            // 纯负数区间：蓝色渐变
            gfx.fillGradient(xStart, HEAT_BAR_Y + 1, xEnd, HEAT_BAR_Y + HEAT_BAR_H - 1, 0xFF00AAAA, 0xFF0055FF);
        } else {
            // 跨越 0 点：双色渐变
            int midX = cx;
            gfx.fillGradient(xStart, HEAT_BAR_Y + 1, midX, HEAT_BAR_Y + HEAT_BAR_H - 1, 0xFF00AAAA, 0xFF0055FF);
            gfx.fillGradient(midX, HEAT_BAR_Y + 1, xEnd, HEAT_BAR_Y + HEAT_BAR_H - 1, 0xFFFF0000, 0xFFFFAA00);
        }

        // 3. 绘制刻度线 (同步机器 GUI 样式)
        for (int i = 1; i <= 10; i++) {
            int offset = (int)((range / 10.0f * i) * scale);
            if (offset > HEAT_BAR_W / 2) break;
            // 上下各 2px 刻度
            gfx.fill(cx + offset, HEAT_BAR_Y, cx + offset + 1, HEAT_BAR_Y + 2, 0x44FFFFFF);
            gfx.fill(cx - offset, HEAT_BAR_Y, cx - offset + 1, HEAT_BAR_Y + 2, 0x44FFFFFF);
            gfx.fill(cx + offset, HEAT_BAR_Y + HEAT_BAR_H - 2, cx + offset + 1, HEAT_BAR_Y + HEAT_BAR_H, 0x44FFFFFF);
            gfx.fill(cx - offset, HEAT_BAR_Y + HEAT_BAR_H - 2, cx - offset + 1, HEAT_BAR_Y + HEAT_BAR_H, 0x44FFFFFF);
        }

        // 6. 指针绘制 (区分颜色)
        // 原点：粗白
        gfx.fill(cx - 1, HEAT_BAR_Y - 2, cx + 2, HEAT_BAR_Y + HEAT_BAR_H + 2, 0xFFFFFFFF);
        // 区间指针：鲜艳黄 (仅当不是无穷大时显示)
        if (minH > -2000000) {
            gfx.fill(xStart, HEAT_BAR_Y - 2, xStart + 1, HEAT_BAR_Y + HEAT_BAR_H + 2, 0xFFFFFF00);
        }
        if (maxH < 2000000) {
            gfx.fill(xEnd, HEAT_BAR_Y - 2, xEnd + 1, HEAT_BAR_Y + HEAT_BAR_H + 2, 0xFFFFFF00);
        }

        // 7. 绘制文字展示 (稍微缩小以避免拥挤，无阴影，深色)
        var font = Minecraft.getInstance().font;
        String s1 = String.valueOf(minH);
        String s2 = String.valueOf(maxH);
        
        // 坐标不再需要除以缩放比例
        int ty = HEAT_BAR_Y + HEAT_BAR_H + 2;
        if (minH > -2000000) {
            gfx.drawString(font, s1, xStart - font.width(s1) / 2, ty, 0xFF404040, false);
        }
        if (maxH < 2000000) {
            gfx.drawString(font, s2, xEnd - font.width(s2) / 2, ty, 0xFF404040, false);
        }
    }

    private void drawProbabilities(ThermalConverterRecipe recipe, GuiGraphics gfx, net.minecraft.client.gui.Font font) {
        // 输入
        if (!recipe.getItemInputs().isEmpty() && recipe.getItemInputs().get(0).consumeChance() < 1.0f) {
            drawChance(gfx, font, SLOT_IN_X, SLOT_IN_Y, recipe.getItemInputs().get(0).consumeChance());
        }
        if (!recipe.getFluidInputs().isEmpty() && recipe.getFluidInputs().get(0).consumeChance() < 1.0f) {
            drawChance(gfx, font, FLUID_L_X, FLUID_L_Y, recipe.getFluidInputs().get(0).consumeChance());
        }
        // 输出
        for (int i = 0; i < recipe.getItemOutputs().size(); i++) {
            if (recipe.getItemOutputs().get(i).chance() < 1.0f) {
                int x = (i == 0) ? SLOT_OUT1_X : SLOT_OUT2_X;
                drawChance(gfx, font, x, SLOT_OUT1_Y, recipe.getItemOutputs().get(i).chance());
            }
        }
        if (!recipe.getFluidOutputs().isEmpty() && recipe.getFluidOutputs().get(0).chance() < 1.0f) {
            drawChance(gfx, font, FLUID_R_X, FLUID_R_Y, recipe.getFluidOutputs().get(0).chance());
        }
    }

    private void drawChance(GuiGraphics gfx, net.minecraft.client.gui.Font font, int x, int y, float chance) {
        String s = (int)(chance * 100) + "%";
        gfx.pose().pushPose();
        gfx.pose().translate(x + 18, y, 200);
        gfx.pose().scale(0.5f, 0.5f, 0.5f);
        gfx.drawString(font, s, -font.width(s), 0, 0xFFFFFFAA, true);
        gfx.pose().popPose();
    }
}
