package com.xnfu.thermalshock.client.gui;

import com.xnfu.thermalshock.client.gui.tooltip.RecipePreviewTooltip;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;

public class RecipePreviewClientTooltipComponent implements ClientTooltipComponent {
    private final RecipePreviewTooltip tooltip;

    public RecipePreviewClientTooltipComponent(RecipePreviewTooltip tooltip) {
        this.tooltip = tooltip;
    }

    @Override
    public int getHeight() {
        return RecipePreviewRenderer.HEIGHT + 4; // Add some padding
    }

    @Override
    public int getWidth(Font font) {
        return RecipePreviewRenderer.WIDTH;
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics guiGraphics) {
        RecipePreviewRenderer.render(guiGraphics, tooltip.recipe(), x, y + 2);
    }
}
