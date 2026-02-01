package com.xnfu.thermalshock.compat.kubejs;

import com.xnfu.thermalshock.ThermalShock;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchemaRegistry;

public class ThermalShockKJSPlugin implements KubeJSPlugin {
    @Override
    public void registerRecipeSchemas(RecipeSchemaRegistry registry) {
        var ns = registry.namespace(ThermalShock.MODID);
        
        ns.register("overheating", ThermalShockKJSSchemas.OVERHEATING);
        ns.register("thermal_shock", ThermalShockKJSSchemas.SHOCK);
        ns.register("thermal_shock_filling", ThermalShockKJSSchemas.SHOCK_FILLING);
        ns.register("thermal_fuel", ThermalShockKJSSchemas.FUEL);
        ns.register("thermal_converter", ThermalShockKJSSchemas.CONVERTER);
        ns.register("clump_processing", ThermalShockKJSSchemas.EXTRACTION);
        
        // Aliases/Requested types
        ns.register("extraction", ThermalShockKJSSchemas.EXTRACTION);
    }
}