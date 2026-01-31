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
    public record SourceEntry(List<ItemStack> stacks, Optional<FluidStack> fluid, int value, boolean isHeat) {}

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
            super.draw(recipe, recipeSlotsView, gfx, mouseX, mouseY);
            var font = Minecraft.getInstance().font;
            ItemStack display = recipe.blocks.isEmpty() ? ItemStack.EMPTY : recipe.blocks.get(0);
            gfx.drawString(font, display.getHoverName(), 35, 5, 0xFFFFFFFF, false);
            gfx.drawString(font, Component.translatable("tooltip.thermalshock.efficiency", String.format("%.1f", recipe.data.efficiency())), 35, 15, 0xFFAAAA00, false);
            gfx.drawString(font, String.format("Max: +%d / -%d H/t", recipe.data.maxHeatRate(), recipe.data.maxColdRate()), 35, 25, 0xFF808080, false);
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
            super.draw(recipe, recipeSlotsView, gfx, mouseX, mouseY);
            var font = Minecraft.getInstance().font;
            ItemStack display = recipe.items.isEmpty() ? ItemStack.EMPTY : recipe.items.get(0);
            gfx.drawString(font, display.getHoverName(), 35, 5, 0xFFFFFFFF, false);
            gfx.drawString(font, Component.translatable("tooltip.thermalshock.catalyst_yield", String.format("+%.0f%%", recipe.data.bonusYield() * 100)), 35, 15, 0xFF55FF55, false);
            gfx.drawString(font, Component.translatable("tooltip.thermalshock.catalyst_buffer", (int)recipe.data.bufferAmount()), 35, 25, 0xFF808080, false);
        }
    }

    public static class SourceCategory extends DataMapCategory<SourceEntry> {
        public SourceCategory(IGuiHelper helper, RecipeType<SourceEntry> type, String title, ItemStack icon) {
            super(helper, type, title, icon);
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, SourceEntry recipe, IFocusGroup focuses) {
            var slot = builder.addSlot(RecipeIngredientRole.INPUT, 10, 12);
            if (recipe.fluid.isPresent()) {
                slot.addIngredient(mezz.jei.api.neoforge.NeoForgeTypes.FLUID_STACK, recipe.fluid.get());
            } else {
                slot.addItemStacks(recipe.stacks);
            }
        }

        @Override
        public void draw(SourceEntry recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics gfx, double mouseX, double mouseY) {
            super.draw(recipe, recipeSlotsView, gfx, mouseX, mouseY);
            var font = Minecraft.getInstance().font;
            Component name = Component.empty();
            if (recipe.fluid.isPresent()) {
                name = recipe.fluid.get().getHoverName();
            } else if (!recipe.stacks.isEmpty()) {
                name = recipe.stacks.get(0).getHoverName();
            }
            
            gfx.drawString(font, name, 35, 10, 0xFFFFFFFF, false);
            String val = (recipe.isHeat ? "+" : "-") + recipe.value + " H";
            gfx.drawString(font, val, 35, 20, recipe.isHeat ? 0xFFFF5555 : 0xFF55FFFF, false);
        }
    }
}
