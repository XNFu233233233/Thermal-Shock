package com.xnfu.thermalshock.client.gui.tooltip;

import com.xnfu.thermalshock.recipe.AbstractSimulationRecipe;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

public record RecipePreviewTooltip(AbstractSimulationRecipe recipe) implements TooltipComponent {
}
