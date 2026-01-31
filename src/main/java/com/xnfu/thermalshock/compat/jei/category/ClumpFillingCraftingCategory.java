package com.xnfu.thermalshock.compat.jei.category;

import com.xnfu.thermalshock.ThermalShock;
import com.xnfu.thermalshock.recipe.ClumpFillingRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ClumpFillingCraftingCategory extends BaseSimulationCategory<ClumpFillingRecipe> {
    public static final RecipeType<ClumpFillingRecipe> TYPE = RecipeType.create(ThermalShock.MODID, "clump_filling_crafting", ClumpFillingRecipe.class);

    public ClumpFillingCraftingCategory(IGuiHelper helper, ItemStack iconStack) {
        super(helper, iconStack, Component.translatable("gui.thermalshock.jei.category.clump_filling_crafting").withStyle(ChatFormatting.DARK_GREEN), 130, 64);
        setIconWithOverlay(helper, iconStack, new ItemStack(net.minecraft.world.level.block.Blocks.CRAFTING_TABLE));
    }

    @Override public @NotNull RecipeType<ClumpFillingRecipe> getRecipeType() { return TYPE; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ClumpFillingRecipe recipe, IFocusGroup focuses) {
        slotHints.clear();
        ShapedRecipePattern pattern = recipe.getPattern();
        int patternWidth = pattern.width();
        int patternHeight = pattern.height();
        List<Ingredient> ingredients = pattern.ingredients();

        Component itemHint = Component.translatable("jei.thermalshock.slot.item_input").withStyle(ChatFormatting.DARK_AQUA);
        Component outputHint = Component.translatable("jei.thermalshock.slot.clump_filling").withStyle(ChatFormatting.GOLD);

        // 注册 3x3 网格的所有槽位
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                int idx = y * patternWidth + x;
                boolean hasIng = x < patternWidth && y < patternHeight && idx < ingredients.size();
                if (hasIng) {
                    Ingredient ing = ingredients.get(idx);
                    if (!ing.isEmpty()) {
                        addSlotWithTooltip(builder, RecipeIngredientRole.INPUT, 5 + x * 19, 4 + y * 19, itemHint)
                            .addIngredients(ing);
                        continue;
                    }
                }
                addEmptySlotWithTooltip(builder, RecipeIngredientRole.INPUT, 5 + x * 19, 4 + y * 19, itemHint);
            }
        }
        
        addSlotWithTooltip(builder, RecipeIngredientRole.OUTPUT, 100, 23, outputHint)
                .addItemStack(recipe.getResultItem(null));
    }

    @Override
    public void draw(ClumpFillingRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics gfx, double mouseX, double mouseY) {
        drawBackground(gfx);
        
        // 渲染 3x3 网格 (模拟工作台)
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                drawSlot(gfx, 5 + x * 19, 4 + y * 19, COLOR_BLOCK);
            }
        }
        
        drawSlot(gfx, 100, 23, COLOR_SPECIAL);

        // 原版风格箭头
        drawArrow(gfx, 70, 24);

        drawHints(gfx, mouseX, mouseY);
    }
}
