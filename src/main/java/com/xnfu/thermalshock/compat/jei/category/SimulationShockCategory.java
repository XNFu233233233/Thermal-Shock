package com.xnfu.thermalshock.compat.jei.category;

import com.xnfu.thermalshock.ThermalShock;
import com.xnfu.thermalshock.recipe.RecipeSourceType;
import com.xnfu.thermalshock.recipe.SimulationIngredient;
import com.xnfu.thermalshock.recipe.ThermalShockRecipe;
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

public class SimulationShockCategory extends BaseSimulationCategory<ThermalShockRecipe> {
    public static final RecipeType<ThermalShockRecipe> TYPE = RecipeType.create(ThermalShock.MODID, "simulation_shock", ThermalShockRecipe.class);

    public SimulationShockCategory(IGuiHelper helper, ItemStack iconStack) {
        super(helper, iconStack, Component.translatable("gui.thermalshock.jei.category.shock").withStyle(ChatFormatting.DARK_AQUA), 130, 90);
        setIconWithOverlay(helper, iconStack, new ItemStack(Items.BLUE_ICE));
    }

    @Override public @NotNull RecipeType<ThermalShockRecipe> getRecipeType() { return TYPE; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ThermalShockRecipe recipe, IFocusGroup focuses) {
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
    public void draw(ThermalShockRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics gfx, double mouseX, double mouseY) {
        drawBackground(gfx);
        
        for (int i = 0; i < 3; i++) drawSlot(gfx, 5 + i * 19, 5, COLOR_BLOCK);
        for (int i = 0; i < 3; i++) drawSlot(gfx, 5 + i * 19, 26, COLOR_ITEM);
        drawSlot(gfx, 100, 15, COLOR_OUTPUT);

        drawArrow(gfx, 70, 19);

        var font = Minecraft.getInstance().font;
        // 1. Mode (Teal)
        String modeName = Component.translatable("gui.thermalshock.mode.thermalshock").getString();
        gfx.drawString(font, modeName, 5, 50, 0xFF008B8B, false);
        
        // 2. High/Low Temp Req (Red/Blue)
        String hotStr = recipe.getMinHotTemp() == Integer.MIN_VALUE ? "-" : String.valueOf(recipe.getMinHotTemp());
        String coldStr = recipe.getMaxColdTemp() == Integer.MAX_VALUE ? "-" : String.valueOf(recipe.getMaxColdTemp());
        
        Component highText = Component.translatable("jei.thermalshock.label.high", hotStr).withStyle(ChatFormatting.RED);
        Component lowText = Component.translatable("jei.thermalshock.label.low", coldStr).withStyle(ChatFormatting.BLUE);
        
        gfx.drawString(font, highText, 5, 62, 0xFFFFFFFF, false);
        int highWidth = font.width(highText);
        gfx.drawString(font, ", ", 5 + highWidth, 62, 0xFF444444, false);
        gfx.drawString(font, lowText, 5 + highWidth + font.width(", "), 62, 0xFFFFFFFF, false);

        // 3. Delta T (Purple)
        String deltaLine = String.format("热应力(ΔT): %d H", recipe.getRequiredDelta());
        gfx.drawString(font, deltaLine, 5, 74, 0xFF884EA0, false);

        drawHints(gfx, mouseX, mouseY);
    }
}
