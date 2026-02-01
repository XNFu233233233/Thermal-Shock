package com.xnfu.thermalshock.compat.kubejs;

import com.xnfu.thermalshock.ThermalShock;
import com.xnfu.thermalshock.recipe.SimulationIngredient;
import com.xnfu.thermalshock.recipe.ThermalConverterRecipe;
import com.mojang.serialization.Codec;
import dev.latvian.mods.kubejs.recipe.RecipeScriptContext;
import dev.latvian.mods.kubejs.recipe.component.*;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;
import dev.latvian.mods.rhino.type.TypeInfo;
import net.minecraft.resources.ResourceLocation;

public interface ThermalShockKJSSchemas {
    
    RecipeComponent<SimulationIngredient> SIMULATION_INGREDIENT = new RecipeComponent<SimulationIngredient>() {
        @Override
        public RecipeComponentType<?> type() {
            return RecipeComponentType.unit(ResourceLocation.fromNamespaceAndPath(ThermalShock.MODID, "simulation_ingredient"), this);
        }

        @Override
        public Codec<SimulationIngredient> codec() {
            return SimulationIngredient.CODEC;
        }

        @Override
        public TypeInfo typeInfo() {
            return TypeInfo.of(SimulationIngredient.class);
        }

        @Override
        public SimulationIngredient wrap(RecipeScriptContext cx, Object from) {
            if (from instanceof SimulationIngredient si) return si;
            return cx.ops().decode(cx.cx(), codec(), from);
        }
    };

    RecipeComponent<ThermalConverterRecipe.InputItem> CONVERTER_INPUT_ITEM = new RecipeComponent<ThermalConverterRecipe.InputItem>() {
        @Override
        public RecipeComponentType<?> type() {
            return RecipeComponentType.unit(ResourceLocation.fromNamespaceAndPath(ThermalShock.MODID, "converter_input_item"), this);
        }

        @Override
        public Codec<ThermalConverterRecipe.InputItem> codec() {
            return ThermalConverterRecipe.InputItem.CODEC;
        }

        @Override
        public TypeInfo typeInfo() {
            return TypeInfo.of(ThermalConverterRecipe.InputItem.class);
        }

        @Override
        public ThermalConverterRecipe.InputItem wrap(RecipeScriptContext cx, Object from) {
            if (from instanceof ThermalConverterRecipe.InputItem val) return val;
            return cx.ops().decode(cx.cx(), codec(), from);
        }
    };

    RecipeComponent<ThermalConverterRecipe.InputFluid> CONVERTER_INPUT_FLUID = new RecipeComponent<ThermalConverterRecipe.InputFluid>() {
        @Override
        public RecipeComponentType<?> type() {
            return RecipeComponentType.unit(ResourceLocation.fromNamespaceAndPath(ThermalShock.MODID, "converter_input_fluid"), this);
        }

        @Override
        public Codec<ThermalConverterRecipe.InputFluid> codec() {
            return ThermalConverterRecipe.InputFluid.CODEC;
        }

        @Override
        public TypeInfo typeInfo() {
            return TypeInfo.of(ThermalConverterRecipe.InputFluid.class);
        }

        @Override
        public ThermalConverterRecipe.InputFluid wrap(RecipeScriptContext cx, Object from) {
            if (from instanceof ThermalConverterRecipe.InputFluid val) return val;
            return cx.ops().decode(cx.cx(), codec(), from);
        }
    };

    RecipeComponent<ThermalConverterRecipe.OutputItem> CONVERTER_OUTPUT_ITEM = new RecipeComponent<ThermalConverterRecipe.OutputItem>() {
        @Override
        public RecipeComponentType<?> type() {
            return RecipeComponentType.unit(ResourceLocation.fromNamespaceAndPath(ThermalShock.MODID, "converter_output_item"), this);
        }

        @Override
        public Codec<ThermalConverterRecipe.OutputItem> codec() {
            return ThermalConverterRecipe.OutputItem.CODEC;
        }

        @Override
        public TypeInfo typeInfo() {
            return TypeInfo.of(ThermalConverterRecipe.OutputItem.class);
        }

        @Override
        public ThermalConverterRecipe.OutputItem wrap(RecipeScriptContext cx, Object from) {
            if (from instanceof ThermalConverterRecipe.OutputItem val) return val;
            return cx.ops().decode(cx.cx(), codec(), from);
        }
    };

    RecipeComponent<ThermalConverterRecipe.OutputFluid> CONVERTER_OUTPUT_FLUID = new RecipeComponent<ThermalConverterRecipe.OutputFluid>() {
        @Override
        public RecipeComponentType<?> type() {
            return RecipeComponentType.unit(ResourceLocation.fromNamespaceAndPath(ThermalShock.MODID, "converter_output_fluid"), this);
        }

        @Override
        public Codec<ThermalConverterRecipe.OutputFluid> codec() {
            return ThermalConverterRecipe.OutputFluid.CODEC;
        }

        @Override
        public TypeInfo typeInfo() {
            return TypeInfo.of(ThermalConverterRecipe.OutputFluid.class);
        }

        @Override
        public ThermalConverterRecipe.OutputFluid wrap(RecipeScriptContext cx, Object from) {
            if (from instanceof ThermalConverterRecipe.OutputFluid val) return val;
            return cx.ops().decode(cx.cx(), codec(), from);
        }
    };

    RecipeSchema OVERHEATING = new RecipeSchema(
            SIMULATION_INGREDIENT.asList().inputKey("ingredients"),
            ItemStackComponent.ITEM_STACK.instance().outputKey("result"),
            NumberComponent.INT.inputKey("min_heat"),
            NumberComponent.INT.inputKey("heat_cost")
    );

    RecipeSchema SHOCK = new RecipeSchema(
            SIMULATION_INGREDIENT.asList().inputKey("ingredients"),
            ItemStackComponent.ITEM_STACK.instance().outputKey("result"),
            NumberComponent.INT.inputKey("min_hot"),
            NumberComponent.INT.inputKey("max_cold"),
            NumberComponent.INT.inputKey("delta")
    );

    RecipeSchema SHOCK_FILLING = new RecipeSchema(
            SIMULATION_INGREDIENT.asList().inputKey("ingredients"),
            ItemStackComponent.ITEM_STACK.instance().outputKey("target_result"),
            NumberComponent.INT.inputKey("min_hot"),
            NumberComponent.INT.inputKey("max_cold"),
            NumberComponent.INT.inputKey("delta"),
            NumberComponent.INT.inputKey("clump_min_heat"),
            NumberComponent.INT.inputKey("clump_heat_cost")
    );

    RecipeSchema EXTRACTION = new RecipeSchema(
            SIMULATION_INGREDIENT.asList().inputKey("ingredients"),
            ItemStackComponent.ITEM_STACK.instance().outputKey("target_content"),
            NumberComponent.INT.inputKey("min_heat"),
            NumberComponent.INT.inputKey("heat_cost")
    );

    RecipeSchema FUEL = new RecipeSchema(
            IngredientComponent.INGREDIENT.instance().inputKey("ingredient"),
            NumberComponent.INT.inputKey("heat_rate"),
            NumberComponent.INT.inputKey("burn_time")
    );

    RecipeSchema CONVERTER = new RecipeSchema(
            CONVERTER_INPUT_ITEM.asList().inputKey("item_inputs"),
            CONVERTER_INPUT_FLUID.asList().inputKey("fluid_inputs"),
            CONVERTER_OUTPUT_ITEM.asList().outputKey("item_outputs"),
            CONVERTER_OUTPUT_FLUID.asList().outputKey("fluid_outputs"),
            NumberComponent.INT.inputKey("process_time"),
            NumberComponent.INT.inputKey("min_heat"),
            NumberComponent.INT.inputKey("max_heat")
    );
}