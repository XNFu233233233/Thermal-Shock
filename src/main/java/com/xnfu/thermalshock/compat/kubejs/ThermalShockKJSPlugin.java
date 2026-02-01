package com.xnfu.thermalshock.compat.kubejs;

import com.xnfu.thermalshock.ThermalShock;
import dev.latvian.mods.kubejs.KubeJSPlugin;
import dev.latvian.mods.kubejs.recipe.schema.RegisterRecipeSchemasEvent;

public class ThermalShockKJSPlugin implements KubeJSPlugin {
    @Override
    public void registerRecipeSchemas(RegisterRecipeSchemasEvent event) {
        var namespace = event.namespace(ThermalShock.MODID);
        
        namespace.register("overheating", ThermalShockKJSSchemas.OVERHEATING);
        namespace.register("thermal_shock", ThermalShockKJSSchemas.SHOCK);
        namespace.register("thermal_shock_filling", ThermalShockKJSSchemas.SHOCK_FILLING);
        namespace.register("thermal_fuel", ThermalShockKJSSchemas.FUEL);
        namespace.register("thermal_converter", ThermalShockKJSSchemas.CONVERTER);
    }
}
