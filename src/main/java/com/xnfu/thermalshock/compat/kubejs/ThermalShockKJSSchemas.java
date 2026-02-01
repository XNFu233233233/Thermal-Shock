package com.xnfu.thermalshock.compat.kubejs;

import com.xnfu.thermalshock.recipe.*;
import com.xnfu.thermalshock.recipe.ThermalConverterRecipe.*;
import dev.latvian.mods.kubejs.recipe.RecipeComponent;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;

public interface ThermalShockKJSSchemas {

    RecipeComponent<SimulationIngredient> SIMULATION_INGREDIENT = RecipeComponent.composite(
            "simulation_ingredient",
            SimulationIngredient.class,
            RecipeComponent.INGREDIENT.fieldOf("value"),
            RecipeComponent.ENUM(RecipeSourceType.class).fieldOf("type"),
            (value, type) -> new SimulationIngredient(value, type),
            SimulationIngredient::ingredient,
            SimulationIngredient::type
    );

    RecipeSchema OVERHEATING = new RecipeSchema(
            SIMULATION_INGREDIENT.listOf().fieldOf("ingredients"),
            RecipeComponent.ITEM_STACK.fieldOf("result"),
            RecipeComponent.INT.fieldOf("min_temp"),
            RecipeComponent.INT.fieldOf("heat_cost")
    );

    RecipeSchema SHOCK = new RecipeSchema(
            SIMULATION_INGREDIENT.listOf().fieldOf("ingredients"),
            RecipeComponent.ITEM_STACK.fieldOf("result"),
            RecipeComponent.INT.fieldOf("min_hot"),
            RecipeComponent.INT.fieldOf("max_cold"),
            RecipeComponent.INT.fieldOf("delta")
    );

    RecipeSchema SHOCK_FILLING = new RecipeSchema(
            SIMULATION_INGREDIENT.listOf().fieldOf("ingredients"),
            RecipeComponent.ITEM_STACK.fieldOf("target_result"),
            RecipeComponent.INT.fieldOf("min_hot"),
            RecipeComponent.INT.fieldOf("max_cold"),
            RecipeComponent.INT.fieldOf("delta"),
            RecipeComponent.INT.fieldOf("clump_min_temp"),
            RecipeComponent.INT.fieldOf("clump_heat_cost")
    );

    RecipeSchema FUEL = new RecipeSchema(
            RecipeComponent.INGREDIENT.fieldOf("ingredient"),
            RecipeComponent.INT.fieldOf("burn_time"),
            RecipeComponent.INT.fieldOf("heat_rate")
    );

    // Converter Components
    RecipeComponent<InputItem> CONVERTER_INPUT_ITEM = RecipeComponent.composite(
            "converter_input_item",
            InputItem.class,
            RecipeComponent.INGREDIENT.fieldOf("ingredient"),
            RecipeComponent.INT.fieldOf("count").optional(1),
            RecipeComponent.FLOAT.fieldOf("consume_chance").optional(1.0f),
            InputItem::new,
            InputItem::ingredient,
            InputItem::count,
            InputItem::consumeChance
    );

    RecipeComponent<InputFluid> CONVERTER_INPUT_FLUID = RecipeComponent.composite(
            "converter_input_fluid",
            InputFluid.class,
            RecipeComponent.FLUID_STACK.fieldOf("fluid"),
            RecipeComponent.FLOAT.fieldOf("consume_chance").optional(1.0f),
            InputFluid::new,
            InputFluid::fluid,
            InputFluid::consumeChance
    );

    RecipeComponent<OutputItem> CONVERTER_OUTPUT_ITEM = RecipeComponent.composite(
            "converter_output_item",
            OutputItem.class,
            RecipeComponent.ITEM_STACK.fieldOf("item"),
            RecipeComponent.FLOAT.fieldOf("chance").optional(1.0f),
            OutputItem::new,
            OutputItem::stack,
            OutputItem::chance
    );

    RecipeComponent<OutputFluid> CONVERTER_OUTPUT_FLUID = RecipeComponent.composite(
            "converter_output_fluid",
            OutputFluid.class,
            RecipeComponent.FLUID_STACK.fieldOf("fluid"),
            RecipeComponent.FLOAT.fieldOf("chance").optional(1.0f),
            OutputFluid::new,
            OutputFluid::fluid,
            OutputFluid::chance
    );

    RecipeSchema CONVERTER = new RecipeSchema(
            CONVERTER_INPUT_ITEM.listOf().fieldOf("item_inputs").optional(java.util.List.of()),
            CONVERTER_INPUT_FLUID.listOf().fieldOf("fluid_inputs").optional(java.util.List.of()),
            CONVERTER_OUTPUT_ITEM.listOf().fieldOf("item_outputs").optional(java.util.List.of()),
            CONVERTER_OUTPUT_FLUID.listOf().fieldOf("fluid_outputs").optional(java.util.List.of()),
            RecipeComponent.INT.fieldOf("process_time"),
            RecipeComponent.INT.fieldOf("min_heat").optional(Integer.MIN_VALUE),
            RecipeComponent.INT.fieldOf("max_heat").optional(Integer.MAX_VALUE)
    );
}
