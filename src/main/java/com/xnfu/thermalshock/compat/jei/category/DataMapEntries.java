package com.xnfu.thermalshock.compat.jei.category;

import com.xnfu.thermalshock.data.CasingData;
import com.xnfu.thermalshock.data.CatalystData;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import java.util.List;
import java.util.Optional;

public class DataMapEntries {
    public record CasingEntry(List<ItemStack> blocks, CasingData data) {}
    public record CatalystEntry(List<ItemStack> items, CatalystData data) {}
    public record SourceEntry(List<ItemStack> stacks, Optional<FluidStack> fluid, int value, int feValue, boolean isHeat, boolean isFE) {}

    public static class CasingCategory extends DataMapCategory<CasingEntry> {
        public CasingCategory(IGuiHelper helper, RecipeType<CasingEntry> type, ItemStack icon) {
            super(helper, type, "gui.thermalshock.jei.map.casing", icon);
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, CasingEntry recipe, IFocusGroup focuses) {
            builder.addSlot(RecipeIngredientRole.INPUT, 10, 12).addItemStacks(recipe.blocks);
        }

        @Override
        public void draw(CasingEntry recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics gfx, double mouseX, double mouseY) {
            gfx.fill(0, 0, 150, 40, 0xFFC6C6C6);
            super.draw(recipe, recipeSlotsView, gfx, mouseX, mouseY);
            var font = Minecraft.getInstance().font;
            ItemStack display = recipe.blocks.isEmpty() ? ItemStack.EMPTY : recipe.blocks.get(0);
            gfx.drawString(font, display.getHoverName(), 35, 5, 0xFF404040, false);
            gfx.drawString(font, Component.translatable("tooltip.thermalshock.efficiency", String.format("%.1f", recipe.data.efficiency())), 35, 15, 0xFF555500, false);
            gfx.drawString(font, String.format("Max: +%d / -%d H/t", recipe.data.maxHeatRate(), recipe.data.maxColdRate()), 35, 25, 0xFF606060, false);
        }
    }

    public static class CatalystCategory extends DataMapCategory<CatalystEntry> {
        public CatalystCategory(IGuiHelper helper, RecipeType<CatalystEntry> type, ItemStack icon) {
            super(helper, type, "gui.thermalshock.jei.map.catalyst", icon);
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, CatalystEntry recipe, IFocusGroup focuses) {
            builder.addSlot(RecipeIngredientRole.INPUT, 10, 12).addItemStacks(recipe.items);
        }

        @Override
        public void draw(CatalystEntry recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics gfx, double mouseX, double mouseY) {
            gfx.fill(0, 0, 150, 40, 0xFFC6C6C6);
            super.draw(recipe, recipeSlotsView, gfx, mouseX, mouseY);
            var font = Minecraft.getInstance().font;
            ItemStack display = recipe.items.isEmpty() ? ItemStack.EMPTY : recipe.items.get(0);
            gfx.drawString(font, display.getHoverName(), 35, 5, 0xFF404040, false);
            gfx.drawString(font, Component.translatable("tooltip.thermalshock.catalyst_yield", String.format("+%.0f%%", recipe.data.bonusYield() * 100)), 35, 15, 0xFF006600, false);
            gfx.drawString(font, Component.translatable("tooltip.thermalshock.catalyst_buffer", (int)recipe.data.catalystPoints()), 35, 25, 0xFF606060, false);
        }
    }

    public static class SourceCategory extends DataMapCategory<SourceEntry> {
        public SourceCategory(IGuiHelper helper, RecipeType<SourceEntry> type, String title, ItemStack icon) {
            super(helper, type, title, icon);
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, SourceEntry recipe, IFocusGroup focuses) {
            if (recipe.isFE) return; // 能量转换不需要槽位
            var slot = builder.addSlot(RecipeIngredientRole.INPUT, 10, 12);
            if (recipe.fluid.isPresent()) {
                slot.addIngredient(mezz.jei.api.neoforge.NeoForgeTypes.FLUID_STACK, recipe.fluid.get());
            } else {
                slot.addItemStacks(recipe.stacks);
            }
        }

        @Override
        public void draw(SourceEntry recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics gfx, double mouseX, double mouseY) {
            gfx.fill(0, 0, 150, 40, 0xFFC6C6C6);
            super.draw(recipe, recipeSlotsView, gfx, mouseX, mouseY);
            var font = Minecraft.getInstance().font;
            
            if (recipe.isFE) {
                // FE 转换渲染: 文字与进度条平齐
                String feStr = recipe.feValue + " FE/t";
                String heatStr = (recipe.isHeat ? "+" : "-") + recipe.value + " H/t";
                
                int barX = 65;
                int barY = 16;
                int barW = 24;
                
                // 1. 绘制进度条
                gfx.fill(barX, barY, barX + barW, barY + 8, 0xFF555555);
                long time = System.currentTimeMillis() / 50;
                float pct = (time % 40) / 40.0f;
                int fillW = (int) (pct * barW);
                if (fillW > 0) gfx.fill(barX, barY, barX + fillW, barY + 8, 0xFF00FF00);
                
                // 2. 绘制左侧 FE (右对齐，距离增加到 8px 以免拥挤)
                gfx.drawString(font, feStr, barX - 8 - font.width(feStr), barY, 0xFF006666, false);
                
                // 3. 绘制右侧 Heat (左对齐，距离 5px)
                gfx.drawString(font, heatStr, barX + barW + 5, barY, recipe.isHeat ? 0xFF990000 : 0xFF000099, false);
                
                // 4. 绘制下方标题
                gfx.drawString(font, Component.translatable("gui.thermalshock.jei.fe_conversion"), 10, 30, 0xFF404040, false);
            } else {
                Component name = Component.empty();
                if (recipe.fluid.isPresent()) {
                    name = recipe.fluid.get().getHoverName();
                } else if (!recipe.stacks.isEmpty()) {
                    name = recipe.stacks.get(0).getHoverName();
                }
                
                gfx.drawString(font, name, 35, 10, 0xFF404040, false);
                String val = (recipe.isHeat ? "+" : "-") + recipe.value + " H/t";
                gfx.drawString(font, val, 35, 22, recipe.isHeat ? 0xFF990000 : 0xFF000099, false);
            }
        }
    }
}
