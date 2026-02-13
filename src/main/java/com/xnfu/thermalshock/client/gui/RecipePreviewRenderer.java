package com.xnfu.thermalshock.client.gui;

import com.xnfu.thermalshock.client.ClientModEvents;
import com.xnfu.thermalshock.data.ClumpInfo;
import com.xnfu.thermalshock.recipe.*;
import com.xnfu.thermalshock.registries.ThermalShockDataComponents;
import com.xnfu.thermalshock.registries.ThermalShockItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;

public class RecipePreviewRenderer {

    private static final int COLOR_SPECIAL = 0xFFDDAA00;
    private static final int COLOR_BLOCK = 0xFF606060;
    private static final int COLOR_ITEM = 0xFF008080;
    private static final int COLOR_OUTPUT = 0xFF373737;
    private static final int COLOR_SLOT_BG = 0xFF8B8B8B;
    
    // 渲染尺寸与 JEI Category 保持一致：130x90
    public static final int WIDTH = 130;
    public static final int HEIGHT = 90;

    public static void render(GuiGraphics gfx, AbstractSimulationRecipe recipe, int x, int y) {
        // [Fix] 暂时禁用团块的 Shift 变身逻辑，强制显示右上角角标模式
        ClientModEvents.SUPPRESS_CLUMP_SHIFT_RENDER = true;
        try {
            renderBackground(gfx, x, y);
            
            if (recipe instanceof ClumpProcessingRecipe extraction) {
                renderClumpExtraction(gfx, extraction, x, y);
            } else if (recipe instanceof ThermalShockFillingRecipe filling) {
                renderClumpFilling(gfx, filling, x, y);
            } else if (recipe instanceof OverheatingRecipe ov) {
                renderOverheating(gfx, ov, x, y);
            } else if (recipe instanceof ThermalShockRecipe ts) {
                renderShock(gfx, ts, x, y);
            }
        } finally {
            ClientModEvents.SUPPRESS_CLUMP_SHIFT_RENDER = false;
        }
    }

    private static void renderBackground(GuiGraphics gfx, int x, int y) {
        gfx.fill(x, y, x + WIDTH, y + HEIGHT, 0xFFC6C6C6);
        gfx.fill(x, y, x + WIDTH, y + 1, 0xFFFFFFFF);
        gfx.fill(x, y, x + 1, y + HEIGHT, 0xFFFFFFFF);
        gfx.fill(x + WIDTH - 1, y, x + WIDTH, y + HEIGHT, 0xFF555555);
        gfx.fill(x, y + HEIGHT - 1, x + WIDTH, y + HEIGHT, 0xFF555555);
    }

    // 1+3+2 布局 (团块提取)
    private static void renderClumpExtraction(GuiGraphics gfx, ClumpProcessingRecipe recipe, int x, int y) {
        List<SimulationIngredient> inputs = recipe.getSimulationIngredients();
        // Lead Clump
        ItemStack displayClump = new ItemStack(ThermalShockItems.MATERIAL_CLUMP.get());
        displayClump.set(ThermalShockDataComponents.TARGET_OUTPUT, 
                        new ClumpInfo(recipe.getTargetContent()));
        
        drawSlot(gfx, x + 65, y + 15, COLOR_SPECIAL);
        gfx.renderItem(displayClump, x + 66, y + 16);

        renderMatrix(gfx, inputs.stream().skip(1).toList(), x, y);
        
        // Output
        drawSlot(gfx, x + 105, y + 15, COLOR_SPECIAL);
        gfx.renderItem(recipe.getResultStack(), x + 106, y + 16);
        drawArrow(gfx, x + 85, y + 16);

        renderModeInfo(gfx, x, y, "gui.thermalshock.mode.overheating", 0xFFAA0000);
        
        var font = Minecraft.getInstance().font;
        Component reqText = Component.translatable("gui.thermalshock.jei.label.min_heat_rate", recipe.getMinHeatRate());
        gfx.drawString(font, reqText, x + 5, y + 62, 0xFF555555, false);
        
        Component costText = Component.translatable("gui.thermalshock.jei.label.heat_cost", recipe.getHeatCost());
        gfx.drawString(font, costText, x + 5, y + 74, 0xFFD35400, false);
    }

    // 1+3+2 布局 (团块填充)
    private static void renderClumpFilling(GuiGraphics gfx, ThermalShockFillingRecipe recipe, int x, int y) {
        List<SimulationIngredient> inputs = recipe.getSimulationIngredients();
        // Lead Clump
        if (!inputs.isEmpty()) {
            drawSlot(gfx, x + 65, y + 15, COLOR_SPECIAL);
            drawIngredient(gfx, inputs.get(0).ingredient(), x + 66, y + 16);
        }

        renderMatrix(gfx, inputs.stream().skip(1).toList(), x, y);

        // Output
        drawSlot(gfx, x + 105, y + 15, COLOR_SPECIAL);
        ItemStack result = recipe.getResultItem(Minecraft.getInstance().level.registryAccess());
        gfx.renderItem(result, x + 106, y + 16);
        gfx.renderItemDecorations(Minecraft.getInstance().font, result, x + 106, y + 16);
        
        drawArrow(gfx, x + 85, y + 16);

        renderModeInfo(gfx, x, y, "gui.thermalshock.mode.shock", 0xFF008B8B);
        renderShockTemps(gfx, x, y, recipe.getMinHotTemp(), recipe.getMaxColdTemp(), recipe.getRequiredDelta());
    }

    private static void renderMatrix(GuiGraphics gfx, List<SimulationIngredient> matrixInputs, int x, int y) {
        List<SimulationIngredient> blocks = matrixInputs.stream().filter(i -> i.type() == RecipeSourceType.BLOCK).toList();
        List<SimulationIngredient> items = matrixInputs.stream().filter(i -> i.type() == RecipeSourceType.ITEM).toList();

        for (int i = 0; i < 3; i++) {
            int sx = x + 5 + i * 19;
            int sy = y + 5;
            drawSlot(gfx, sx, sy, COLOR_BLOCK);
            if (i < blocks.size()) drawIngredient(gfx, blocks.get(i).ingredient(), sx + 1, sy + 1);
        }
        for (int i = 0; i < 3; i++) {
            int sx = x + 5 + i * 19;
            int sy = y + 26;
            drawSlot(gfx, sx, sy, COLOR_ITEM);
            if (i < items.size()) drawIngredient(gfx, items.get(i).ingredient(), sx + 1, sy + 1);
        }
    }

    private static void renderOverheating(GuiGraphics gfx, OverheatingRecipe recipe, int x, int y) {
        renderStandardGrid(gfx, recipe.getSimulationIngredients(), x, y);
        
        drawSlot(gfx, x + 100, y + 15, COLOR_OUTPUT);
        ItemStack result = recipe.getResultItem(Minecraft.getInstance().level.registryAccess());
        gfx.renderItem(result, x + 101, y + 16);
        gfx.renderItemDecorations(Minecraft.getInstance().font, result, x + 101, y + 16);

        drawArrow(gfx, x + 70, y + 19);

        renderModeInfo(gfx, x, y, "gui.thermalshock.mode.overheating", 0xFFAA0000);
        
        var font = Minecraft.getInstance().font;
        Component reqText = Component.translatable("gui.thermalshock.jei.label.min_heat_rate", recipe.getMinHeatRate());
        gfx.drawString(font, reqText, x + 5, y + 62, 0xFF555555, false);
        
        Component costText = Component.translatable("gui.thermalshock.jei.label.heat_cost", recipe.getHeatCost());
        gfx.drawString(font, costText, x + 5, y + 74, 0xFFD35400, false);
    }
    
    private static void renderShock(GuiGraphics gfx, ThermalShockRecipe recipe, int x, int y) {
        renderStandardGrid(gfx, recipe.getSimulationIngredients(), x, y);
        
        drawSlot(gfx, x + 100, y + 15, COLOR_OUTPUT);
        ItemStack result = recipe.getResultItem(Minecraft.getInstance().level.registryAccess());
        gfx.renderItem(result, x + 101, y + 16);
        gfx.renderItemDecorations(Minecraft.getInstance().font, result, x + 101, y + 16);

        drawArrow(gfx, x + 70, y + 19);

        renderModeInfo(gfx, x, y, "gui.thermalshock.mode.shock", 0xFF008B8B);
        renderShockTemps(gfx, x, y, recipe.getMinHotTemp(), recipe.getMaxColdTemp(), recipe.getRequiredDelta());
    }

    private static void renderStandardGrid(GuiGraphics gfx, List<SimulationIngredient> inputs, int x, int y) {
        List<SimulationIngredient> blocks = inputs.stream().filter(i -> i.type() == RecipeSourceType.BLOCK).toList();
        List<SimulationIngredient> items = inputs.stream().filter(i -> i.type() == RecipeSourceType.ITEM).toList();

        for (int i = 0; i < 3; i++) {
            int sx = x + 5 + i * 19, sy = y + 5;
            drawSlot(gfx, sx, sy, COLOR_BLOCK);
            if (i < blocks.size()) drawIngredient(gfx, blocks.get(i).ingredient(), sx + 1, sy + 1);
        }
        for (int i = 0; i < 3; i++) {
            int sx = x + 5 + i * 19, sy = y + 26;
            drawSlot(gfx, sx, sy, COLOR_ITEM);
            if (i < items.size()) drawIngredient(gfx, items.get(i).ingredient(), sx + 1, sy + 1);
        }
    }

    private static void renderModeInfo(GuiGraphics gfx, int x, int y, String langKey, int color) {
        var font = Minecraft.getInstance().font;
        Component modeName = Component.translatable(langKey);
        gfx.drawString(font, modeName, x + 5, y + 50, color, false);
    }

    private static void renderShockTemps(GuiGraphics gfx, int x, int y, int hot, int cold, int delta) {
        var font = Minecraft.getInstance().font;
        String hotStr = hot == Integer.MIN_VALUE ? "-" : String.valueOf(hot);
        String coldStr = cold == Integer.MAX_VALUE ? "-" : String.valueOf(cold);
        
        Component highText = Component.translatable("jei.thermalshock.label.high", hotStr);
        Component lowText = Component.translatable("jei.thermalshock.label.low", coldStr);
        
        gfx.drawString(font, highText, x + 5, y + 62, 0xFFAAAA00, false);
        int highWidth = font.width(highText);
        gfx.drawString(font, ", ", x + 5 + highWidth, 62 + y, 0xFF444444, false);
        gfx.drawString(font, lowText, x + 5 + highWidth + font.width(", "), y + 62, 0xFF55FFFF, false);

        Component deltaLine = Component.translatable("gui.thermalshock.label.delta", delta);
        gfx.drawString(font, deltaLine, x + 5, y + 74, 0xFF884EA0, false);
    }
    
    private static void drawSlot(GuiGraphics gfx, int x, int y, int color) {
        gfx.fill(x, y, x + 18, y + 18, color);
        gfx.fill(x + 1, y + 1, x + 17, y + 17, COLOR_SLOT_BG);
    }
    
    private static void drawArrow(GuiGraphics gfx, int x, int y) {
        gfx.fill(x, y + 4, x + 12, y + 10, 0xFF7E7E7E);
        gfx.fill(x + 12, y, x + 14, y + 14, 0xFF7E7E7E);
        gfx.fill(x + 14, y + 2, x + 16, y + 12, 0xFF7E7E7E);
        gfx.fill(x + 16, y + 4, x + 18, y + 10, 0xFF7E7E7E);
    }
    
    private static void drawIngredient(GuiGraphics gfx, Ingredient ing, int x, int y) {
        if (ing.isEmpty()) return;
        ItemStack[] items = ing.getItems();
        if (items.length == 0) return;
        
        long millis = System.currentTimeMillis();
        int index = (int) ((millis / 1000) % items.length);
        ItemStack stack = items[index];
        
        gfx.renderItem(stack, x, y);
        gfx.renderItemDecorations(Minecraft.getInstance().font, stack, x, y);
    }
}
