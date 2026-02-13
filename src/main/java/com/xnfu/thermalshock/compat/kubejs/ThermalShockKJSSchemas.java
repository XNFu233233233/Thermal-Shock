package com.xnfu.thermalshock.compat.kubejs;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.xnfu.thermalshock.ThermalShock;
import com.xnfu.thermalshock.compat.kubejs.recipe.ThermalShockRecipeJS;
import com.xnfu.thermalshock.recipe.ThermalConverterRecipe;
import dev.latvian.mods.kubejs.recipe.KubeRecipe;
import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.kubejs.recipe.RecipeScriptContext;
import dev.latvian.mods.kubejs.recipe.component.*;
import dev.latvian.mods.kubejs.recipe.schema.KubeRecipeFactory;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;
import dev.latvian.mods.kubejs.util.IntBounds;
import dev.latvian.mods.rhino.type.TypeInfo;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.fluids.FluidStack;
import net.minecraft.resources.ResourceLocation;
import com.mojang.serialization.Codec;
import java.util.List;
import java.util.Map;

/**
 * Thermal Shock Recipe Schemas for KubeJS 1.21.1
 */
public class ThermalShockKJSSchemas {

    // === 1. Base Components ===
    public static final RecipeComponent<ItemStack> ITEM_STACK = ItemStackComponent.ITEM_STACK.instance();
    public static final RecipeComponent<Ingredient> INGREDIENT = IngredientComponent.INGREDIENT.instance();
    public static final RecipeComponent<String> ID_STR = StringComponent.ID.instance();
    public static final RecipeComponent<Integer> INT = NumberComponent.INT;
    public static final RecipeComponent<FluidStack> FLUID_STACK = FluidStackComponent.FLUID_STACK.instance();

    // === 2. Keys ===
    public static final RecipeKey<ItemStack> RESULT = ITEM_STACK.outputKey("result");
    public static final RecipeKey<ItemStack> TARGET_RESULT = ITEM_STACK.outputKey("target_result");
    public static final RecipeKey<ItemStack> TARGET_CONTENT = ITEM_STACK.outputKey("target_content");

    public static final RecipeKey<List<Ingredient>> ITEM_INPUTS = INGREDIENT.asList().withBounds(IntBounds.of(0, 9)).inputKey("item_inputs");
    public static final RecipeKey<List<String>> BLOCK_INPUTS = ID_STR.asList().withBounds(IntBounds.of(0, 9)).inputKey("block_inputs");

    // Adjusted defaults according to PLAN.md
    public static final RecipeKey<Integer> MIN_HEAT = INT.otherKey("min_heat").optional(0).alwaysWrite();
    public static final RecipeKey<Integer> HEAT_COST = INT.otherKey("heat_cost").optional(100).alwaysWrite();
    
    public static final RecipeKey<Integer> MIN_HOT = INT.otherKey("min_hot").optional(Integer.MIN_VALUE).alwaysWrite();
    public static final RecipeKey<Integer> MAX_COLD = INT.otherKey("max_cold").optional(Integer.MAX_VALUE).alwaysWrite();
    public static final RecipeKey<Integer> DELTA = INT.otherKey("delta").alwaysWrite(); // Required

    // === 3. Converter Keys with Custom Wrappers ===
    
    public static final RecipeKey<List<ThermalConverterRecipe.OutputItem>> CON_OUT_I = new RecipeComponent<ThermalConverterRecipe.OutputItem>() {
        @Override public TypeInfo typeInfo() { return TypeInfo.of(ThermalConverterRecipe.OutputItem.class); }
        @Override public Codec<ThermalConverterRecipe.OutputItem> codec() { return ThermalConverterRecipe.OutputItem.CODEC; }
        @Override public RecipeComponentType<?> type() { return RecipeComponentType.unit(ResourceLocation.fromNamespaceAndPath(ThermalShock.MODID, "con_out_i"), this); }
        
        @Override
        public ThermalConverterRecipe.OutputItem wrap(RecipeScriptContext cx, Object from) {
            if (from instanceof ThermalConverterRecipe.OutputItem i) return i;
            var map = (Map<?, ?>) cx.cx().jsToJava(from, TypeInfo.of(Map.class));
            if (map != null) {
                var itemObj = map.get("item");
                var chanceObj = map.get("chance");
                ItemStack stack = ITEM_STACK.wrap(cx, itemObj);
                float chance = chanceObj instanceof Number n ? n.floatValue() : 1.0f;
                return new ThermalConverterRecipe.OutputItem(stack == null ? ItemStack.EMPTY : stack, chance);
            }
            return new ThermalConverterRecipe.OutputItem(ItemStack.EMPTY, 1.0f);
        }
    }.asList().withBounds(IntBounds.of(0, 9)).outputKey("item_outputs");

    public static final RecipeKey<List<ThermalConverterRecipe.OutputFluid>> CON_OUT_F = new RecipeComponent<ThermalConverterRecipe.OutputFluid>() {
        @Override public TypeInfo typeInfo() { return TypeInfo.of(ThermalConverterRecipe.OutputFluid.class); }
        @Override public Codec<ThermalConverterRecipe.OutputFluid> codec() { return ThermalConverterRecipe.OutputFluid.CODEC; }
        @Override public RecipeComponentType<?> type() { return RecipeComponentType.unit(ResourceLocation.fromNamespaceAndPath(ThermalShock.MODID, "con_out_f"), this); }
        
        @Override
        public ThermalConverterRecipe.OutputFluid wrap(RecipeScriptContext cx, Object from) {
            if (from instanceof ThermalConverterRecipe.OutputFluid i) return i;
            var map = (Map<?, ?>) cx.cx().jsToJava(from, TypeInfo.of(Map.class));
            if (map != null) {
                var fluidObj = map.get("fluid");
                var chanceObj = map.get("chance");
                FluidStack stack = FLUID_STACK.wrap(cx, fluidObj);
                float chance = chanceObj instanceof Number n ? n.floatValue() : 1.0f;
                return new ThermalConverterRecipe.OutputFluid(stack == null ? FluidStack.EMPTY : stack, chance);
            }
            return new ThermalConverterRecipe.OutputFluid(FluidStack.EMPTY, 1.0f);
        }
    }.asList().withBounds(IntBounds.of(0, 9)).outputKey("fluid_outputs");

    public static final RecipeKey<List<ThermalConverterRecipe.InputItem>> CON_IN_I = new RecipeComponent<ThermalConverterRecipe.InputItem>() {
        @Override public TypeInfo typeInfo() { return TypeInfo.of(ThermalConverterRecipe.InputItem.class); }
        @Override public Codec<ThermalConverterRecipe.InputItem> codec() { return ThermalConverterRecipe.InputItem.CODEC; }
        @Override public RecipeComponentType<?> type() { return RecipeComponentType.unit(ResourceLocation.fromNamespaceAndPath(ThermalShock.MODID, "con_in_i"), this); }
        
        @Override
        public ThermalConverterRecipe.InputItem wrap(RecipeScriptContext cx, Object from) {
            if (from instanceof ThermalConverterRecipe.InputItem i) return i;
            var map = (Map<?, ?>) cx.cx().jsToJava(from, TypeInfo.of(Map.class));
            if (map != null) {
                var ingObj = map.get("ingredient");
                var countObj = map.get("count");
                var chanceObj = map.get("consume_chance");
                Ingredient ing = INGREDIENT.wrap(cx, ingObj);
                int count = countObj instanceof Number n ? n.intValue() : 1;
                float chance = chanceObj instanceof Number n ? n.floatValue() : 1.0f;
                return new ThermalConverterRecipe.InputItem(ing == null ? Ingredient.EMPTY : ing, count, chance);
            }
            return new ThermalConverterRecipe.InputItem(Ingredient.EMPTY, 1, 1.0f);
        }
    }.asList().withBounds(IntBounds.of(0, 9)).inputKey("item_inputs");

    public static final RecipeKey<List<ThermalConverterRecipe.InputFluid>> CON_IN_F = new RecipeComponent<ThermalConverterRecipe.InputFluid>() {
        @Override public TypeInfo typeInfo() { return TypeInfo.of(ThermalConverterRecipe.InputFluid.class); }
        @Override public Codec<ThermalConverterRecipe.InputFluid> codec() { return ThermalConverterRecipe.InputFluid.CODEC; }
        @Override public RecipeComponentType<?> type() { return RecipeComponentType.unit(ResourceLocation.fromNamespaceAndPath(ThermalShock.MODID, "con_in_f"), this); }
        
        @Override
        public ThermalConverterRecipe.InputFluid wrap(RecipeScriptContext cx, Object from) {
            if (from instanceof ThermalConverterRecipe.InputFluid i) return i;
            var map = (Map<?, ?>) cx.cx().jsToJava(from, TypeInfo.of(Map.class));
            if (map != null) {
                var fluidObj = map.get("fluid");
                var chanceObj = map.get("consume_chance");
                FluidStack stack = FLUID_STACK.wrap(cx, fluidObj);
                float chance = chanceObj instanceof Number n ? n.floatValue() : 1.0f;
                return new ThermalConverterRecipe.InputFluid(stack == null ? FluidStack.EMPTY : stack, chance);
            }
            return new ThermalConverterRecipe.InputFluid(FluidStack.EMPTY, 1.0f);
        }
    }.asList().withBounds(IntBounds.of(0, 9)).inputKey("fluid_inputs");

    // === 4. Factory ===
    public static final KubeRecipeFactory FACTORY = new KubeRecipeFactory(
            ResourceLocation.fromNamespaceAndPath(ThermalShock.MODID, "recipe_js"),
            ThermalShockRecipeJS.class,
            ThermalShockRecipeJS::new
    );

    // === 5. Schemas ===
    public static final RecipeSchema OVERHEATING = new RecipeSchema(RESULT, ITEM_INPUTS, BLOCK_INPUTS, MIN_HEAT, HEAT_COST)
            .factory(FACTORY);

    // FIX: DELTA (Required) must be before MIN_HOT/MAX_COLD (Optional)
    public static final RecipeSchema SHOCK = new RecipeSchema(RESULT, ITEM_INPUTS, BLOCK_INPUTS, DELTA, MIN_HOT, MAX_COLD)
            .factory(FACTORY);

    public static final RecipeSchema SHOCK_FILLING = new RecipeSchema(TARGET_RESULT, ITEM_INPUTS, BLOCK_INPUTS, DELTA, MIN_HOT, MAX_COLD)
            .factory(FACTORY);

    public static final RecipeSchema EXTRACTION = new RecipeSchema(RESULT, TARGET_CONTENT, ITEM_INPUTS, BLOCK_INPUTS, MIN_HEAT, HEAT_COST)
            .factory(FACTORY);

    public static final RecipeSchema FUEL = new RecipeSchema(INGREDIENT.inputKey("ingredient"), 
            INT.otherKey("burn_time").optional(0).alwaysWrite(), 
            INT.otherKey("heat_rate").optional(0).alwaysWrite());

    public static final RecipeSchema CONVERTER = new RecipeSchema(CON_OUT_I, CON_OUT_F, CON_IN_I, CON_IN_F, 
            INT.otherKey("process_time").optional(20).alwaysWrite(), 
            INT.otherKey("min_heat").optional(Integer.MIN_VALUE).alwaysWrite(), 
            INT.otherKey("max_heat").optional(Integer.MAX_VALUE).alwaysWrite());
}
