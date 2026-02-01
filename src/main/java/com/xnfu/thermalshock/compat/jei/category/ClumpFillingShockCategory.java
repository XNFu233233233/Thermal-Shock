package com.xnfu.thermalshock.compat.jei.category;

import com.xnfu.thermalshock.ThermalShock;
import com.xnfu.thermalshock.recipe.RecipeSourceType;
import com.xnfu.thermalshock.recipe.SimulationIngredient;
import com.xnfu.thermalshock.recipe.ThermalShockFillingRecipe;
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
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ClumpFillingShockCategory extends BaseSimulationCategory<ThermalShockFillingRecipe> {
    public static final RecipeType<ThermalShockFillingRecipe> TYPE = RecipeType.create(ThermalShock.MODID, "clump_filling_shock", ThermalShockFillingRecipe.class);

    public ClumpFillingShockCategory(IGuiHelper helper, ItemStack iconStack) {
        super(helper, iconStack, Component.translatable("gui.thermalshock.jei.category.clump_filling_shock").withStyle(ChatFormatting.DARK_AQUA), 130, 90);
        setIconWithOverlay(helper, iconStack, ThermalShockItems.MATERIAL_CLUMP.get().getDefaultInstance());
    }

    @Override public @NotNull RecipeType<ThermalShockFillingRecipe> getRecipeType() { return TYPE; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ThermalShockFillingRecipe recipe, IFocusGroup focuses) {
        slotHints.clear();
        List<SimulationIngredient> inputs = recipe.getSimulationIngredients();
        
        Component blockHint = Component.translatable("jei.thermalshock.slot.block_input").withStyle(ChatFormatting.GRAY);
        Component itemHint = Component.translatable("jei.thermalshock.slot.item_input").withStyle(ChatFormatting.DARK_AQUA);
        Component clumpHint = Component.translatable("jei.thermalshock.slot.clump_filling").withStyle(ChatFormatting.GOLD);
        Component outputHint = Component.translatable("jei.thermalshock.slot.output").withStyle(ChatFormatting.GOLD);

        // 1. 核心团块槽位 (65, 15)
        if (!inputs.isEmpty()) {
            addSimulationSlot(builder, RecipeIngredientRole.INPUT, 65, 15, inputs.get(0), clumpHint);
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
        // 3. 输出槽位 (105, 15)
        // 使用 recipe.getResultItem(null) 以获取动态生成的带数据的团块
        addSlotWithTooltip(builder, RecipeIngredientRole.OUTPUT, 105, 15, outputHint)
            .addItemStack(recipe.getResultItem(null));
    }

    @Override
    public void draw(ThermalShockFillingRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics gfx, double mouseX, double mouseY) {
        drawBackground(gfx);
        
        for (int i = 0; i < 3; i++) drawSlot(gfx, 5 + i * 19, 5, COLOR_BLOCK);
        for (int i = 0; i < 3; i++) drawSlot(gfx, 5 + i * 19, 26, COLOR_ITEM);
        drawSlot(gfx, 65, 15, COLOR_SPECIAL); // Empty Clump Slot
        drawSlot(gfx, 105, 15, COLOR_SPECIAL); // Filled Clump (Output)

        drawArrow(gfx, 85, 16);

        var font = Minecraft.getInstance().font;
        // 1. 模式 (青色)
        String modeName = Component.translatable("gui.thermalshock.mode.thermalshock").getString();
        gfx.drawString(font, modeName, 5, 50, 0xFF008B8B, false);
        
        // 2. 高低温需求 (红色/蓝色)
        Component highText = Component.translatable("jei.thermalshock.label.high", recipe.getMinHotTemp()).withStyle(ChatFormatting.RED);
        Component lowText = Component.translatable("jei.thermalshock.label.low", recipe.getMaxColdTemp()).withStyle(ChatFormatting.BLUE);
        
        gfx.drawString(font, highText, 5, 62, 0xFFFFFFFF, false);
        int highWidth = font.width(highText);
        gfx.drawString(font, ", ", 5 + highWidth, 62, 0xFF444444, false);
        gfx.drawString(font, lowText, 5 + highWidth + font.width(", "), 62, 0xFFFFFFFF, false);

        // 3. 热应力 (紫色)
        String deltaLine = String.format("热应力(ΔT): %d H", recipe.getRequiredDelta());
        gfx.drawString(font, deltaLine, 5, 74, 0xFF884EA0, false);

        drawHints(gfx, mouseX, mouseY);
    }
}
