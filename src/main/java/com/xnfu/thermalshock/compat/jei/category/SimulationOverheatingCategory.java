package com.xnfu.thermalshock.compat.jei.category;

import com.xnfu.thermalshock.ThermalShock;
import com.xnfu.thermalshock.recipe.OverheatingRecipe;
import com.xnfu.thermalshock.recipe.RecipeSourceType;
import com.xnfu.thermalshock.recipe.SimulationIngredient;
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
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SimulationOverheatingCategory extends BaseSimulationCategory<OverheatingRecipe> {
    public static final RecipeType<OverheatingRecipe> TYPE = RecipeType.create(ThermalShock.MODID, "simulation_overheating", OverheatingRecipe.class);

    public SimulationOverheatingCategory(IGuiHelper helper, ItemStack iconStack) {
        super(helper, iconStack, Component.translatable("gui.thermalshock.jei.category.overheating").withStyle(ChatFormatting.DARK_RED), 130, 90);
        setIconWithOverlay(helper, iconStack, new ItemStack(Items.BLAZE_POWDER));
    }

    @Override public @NotNull RecipeType<OverheatingRecipe> getRecipeType() { return TYPE; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, OverheatingRecipe recipe, IFocusGroup focuses) {
        slotHints.clear();
        List<SimulationIngredient> inputs = recipe.getSimulationIngredients();
        
        Component blockHint = Component.translatable("jei.thermalshock.slot.block_input").withStyle(ChatFormatting.GRAY);
        Component itemHint = Component.translatable("jei.thermalshock.slot.item_input").withStyle(ChatFormatting.DARK_AQUA);
        Component outputHint = Component.translatable("jei.thermalshock.slot.output").withStyle(ChatFormatting.GOLD);

        List<SimulationIngredient> blocks = inputs.stream().filter(i -> i.type() == RecipeSourceType.BLOCK).toList();
        List<SimulationIngredient> items = inputs.stream().filter(i -> i.type() == RecipeSourceType.ITEM).toList();

        // Register 3 block slots
        for (int i = 0; i < 3; i++) {
            if (i < blocks.size()) {
                addSimulationSlot(builder, RecipeIngredientRole.INPUT, 5 + i * 19, 5, blocks.get(i), blockHint);
            } else {
                addEmptySlotWithTooltip(builder, RecipeIngredientRole.INPUT, 5 + i * 19, 5, blockHint);
            }
        }

        // Register 3 item slots
        for (int i = 0; i < 3; i++) {
            if (i < items.size()) {
                addSimulationSlot(builder, RecipeIngredientRole.INPUT, 5 + i * 19, 26, items.get(i), itemHint);
            } else {
                addEmptySlotWithTooltip(builder, RecipeIngredientRole.INPUT, 5 + i * 19, 26, itemHint);
            }
        }
        
        addSlotWithTooltip(builder, RecipeIngredientRole.OUTPUT, 100, 15, outputHint)
            .addItemStack(recipe.getResultStack());
    }

    @Override
    public void draw(OverheatingRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics gfx, double mouseX, double mouseY) {
        drawBackground(gfx);
        
        for (int i = 0; i < 3; i++) drawSlot(gfx, 5 + i * 19, 5, COLOR_BLOCK);
        for (int i = 0; i < 3; i++) drawSlot(gfx, 5 + i * 19, 26, COLOR_ITEM);
        drawSlot(gfx, 100, 15, COLOR_OUTPUT);

        drawArrow(gfx, 70, 19);

        var font = Minecraft.getInstance().font;
        
        // 1. Mode Name (Dark Red)
        String modeName = Component.translatable("gui.thermalshock.mode.overheating").getString();
        gfx.drawString(font, modeName, 5, 50, 0xFFAA0000, false);
        
        // 2. Heat Req (Gray)
        String reqLine = String.format("min > %d H/t", recipe.getMinHeatRate());
        gfx.drawString(font, reqLine, 5, 62, 0xFF555555, false);
        
        // 3. Heat Cost (Orange)
        String costLine = String.format("消耗热量: %d H", recipe.getHeatCost());
        gfx.drawString(font, costLine, 5, 74, 0xFFD35400, false);

        drawHints(gfx, mouseX, mouseY);
    }
}
