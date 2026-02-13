package com.xnfu.thermalshock.compat.jei.category;

import com.xnfu.thermalshock.ThermalShock;
import com.xnfu.thermalshock.recipe.ClumpProcessingRecipe;
import com.xnfu.thermalshock.recipe.RecipeSourceType;
import com.xnfu.thermalshock.recipe.SimulationIngredient;
import com.xnfu.thermalshock.data.ClumpInfo;
import com.xnfu.thermalshock.registries.ThermalShockDataComponents;
import com.xnfu.thermalshock.registries.ThermalShockItems;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ClumpExtractionCategory extends BaseSimulationCategory<ClumpProcessingRecipe> {
    public static final RecipeType<ClumpProcessingRecipe> TYPE = RecipeType.create(ThermalShock.MODID, "clump_extraction", ClumpProcessingRecipe.class);

    public ClumpExtractionCategory(IGuiHelper helper, ItemStack iconStack) {
        super(helper, iconStack, Component.translatable("gui.thermalshock.jei.category.clump_extraction").withStyle(ChatFormatting.DARK_RED), 130, 90);
        setIconWithOverlay(helper, iconStack, new ItemStack(Blocks.BLAST_FURNACE));
    }

    @Override public @NotNull RecipeType<ClumpProcessingRecipe> getRecipeType() { return TYPE; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ClumpProcessingRecipe recipe, IFocusGroup focuses) {
        slotHints.clear();
        List<SimulationIngredient> inputs = recipe.getSimulationIngredients();
        
        Component blockHint = Component.translatable("jei.thermalshock.slot.block_input").withStyle(ChatFormatting.GRAY);
        Component itemHint = Component.translatable("jei.thermalshock.slot.item_input").withStyle(ChatFormatting.DARK_AQUA);
        Component clumpHint = Component.translatable("jei.thermalshock.slot.clump_extraction").withStyle(ChatFormatting.GOLD);
        Component outputHint = Component.translatable("jei.thermalshock.slot.output").withStyle(ChatFormatting.GOLD);

        // 1. 核心团块槽位 (65, 15)
        // 创建一个包含正确内容的团块用于展示
        ItemStack displayClump = new ItemStack(ThermalShockItems.MATERIAL_CLUMP.get());
        displayClump.set(ThermalShockDataComponents.TARGET_OUTPUT, 
                        new ClumpInfo(recipe.getTargetItem(), 1));

        if (!inputs.isEmpty()) {
            addSlotWithTooltip(builder, RecipeIngredientRole.INPUT, 65, 15, clumpHint)
                .addItemStack(displayClump); 
        } else {
            addEmptySlotWithTooltip(builder, RecipeIngredientRole.INPUT, 65, 15, clumpHint);
        }

        // 2. 注册 3x2 的原料网格
        List<SimulationIngredient> blocks = inputs.stream().skip(1).filter(i -> i.type() == RecipeSourceType.BLOCK).toList();
        List<SimulationIngredient> items = inputs.stream().skip(1).filter(i -> i.type() == RecipeSourceType.ITEM).toList();

        for (int i = 0; i < 3; i++) {
            if (i < blocks.size()) {
                addSimulationSlot(builder, RecipeIngredientRole.INPUT, 5 + i * 19, 5, blocks.get(i), blockHint);
            } else {
                addEmptySlotWithTooltip(builder, RecipeIngredientRole.INPUT, 5 + i * 19, 5, blockHint);
            }
        }

        for (int i = 0; i < 3; i++) {
            if (i < items.size()) {
                addSimulationSlot(builder, RecipeIngredientRole.INPUT, 5 + i * 19, 26, items.get(i), itemHint);
            } else {
                addEmptySlotWithTooltip(builder, RecipeIngredientRole.INPUT, 5 + i * 19, 26, itemHint);
            }
        }
        
        // 3. 输出槽位 (105, 15)
        addSlotWithTooltip(builder, RecipeIngredientRole.OUTPUT, 105, 15, outputHint)
            .addItemStack(recipe.getResultStack());
    }

    @Override
    public void draw(ClumpProcessingRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics gfx, double mouseX, double mouseY) {
        drawBackground(gfx);
        
        for (int i = 0; i < 3; i++) drawSlot(gfx, 5 + i * 19, 5, COLOR_BLOCK);
        for (int i = 0; i < 3; i++) drawSlot(gfx, 5 + i * 19, 26, COLOR_ITEM);
        drawSlot(gfx, 65, 15, COLOR_SPECIAL); // Filled Clump Slot
        drawSlot(gfx, 105, 15, COLOR_SPECIAL); // Extracted Item (Output)

        drawArrow(gfx, 85, 16);

        var font = Minecraft.getInstance().font;
        // 1. 模式 (暗红色)
        String modeName = Component.translatable("gui.thermalshock.mode.overheating").getString();
        gfx.drawString(font, modeName, 5, 50, 0xFFAA0000, false);
        
        // 2. 热量输入需求 (灰色)
        String reqLine = String.format("min > %d H/t", recipe.getMinHeatRate());
        gfx.drawString(font, reqLine, 5, 62, 0xFF555555, false);
        
        // 3. 消耗热量 (橙色)
        String costLine = String.format("消耗热量: %d H", recipe.getHeatCost());
        gfx.drawString(font, costLine, 5, 74, 0xFFD35400, false);

        drawHints(gfx, mouseX, mouseY);
    }
}
