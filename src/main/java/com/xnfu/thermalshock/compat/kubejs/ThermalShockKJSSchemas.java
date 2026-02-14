package com.xnfu.thermalshock.compat.kubejs;

import com.xnfu.thermalshock.ThermalShock;
import com.xnfu.thermalshock.compat.kubejs.recipe.ThermalShockRecipeJS;
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

public class ThermalShockKJSSchemas {

    public static final RecipeComponent<ItemStack> ITEM_STACK = ItemStackComponent.ITEM_STACK.instance();
    public static final RecipeComponent<Ingredient> INGREDIENT = IngredientComponent.INGREDIENT.instance();
    public static final RecipeComponent<String> ID_STR = StringComponent.ID.instance();
    public static final RecipeComponent<Integer> INT = NumberComponent.INT;
    public static final RecipeComponent<FluidStack> FLUID_STACK = FluidStackComponent.FLUID_STACK.instance();

    public static final RecipeKey<ItemStack> RESULT = ITEM_STACK.outputKey("result");
    public static final RecipeKey<String> TARGET_ITEM = ID_STR.outputKey("target_item"); 
    public static final RecipeKey<Integer> TARGET_COUNT = INT.otherKey("target_count").optional(1).alwaysWrite().exclude();

    public static final RecipeKey<List<Ingredient>> ITEM_INPUTS = INGREDIENT.asList().withBounds(IntBounds.of(0, 9)).inputKey("item_inputs");
    public static final RecipeKey<List<String>> BLOCK_INPUTS = ID_STR.asList().withBounds(IntBounds.of(0, 9)).inputKey("block_inputs");

    public static final RecipeKey<Integer> MIN_HEAT = INT.otherKey("min_heat").optional(0).alwaysWrite().exclude();
    public static final RecipeKey<Integer> HEAT_COST = INT.otherKey("heat_cost").optional(100).alwaysWrite().exclude();
    
    public static final RecipeKey<Integer> MIN_HOT = INT.otherKey("min_hot").optional(Integer.MIN_VALUE).alwaysWrite().exclude();
    public static final RecipeKey<Integer> MAX_COLD = INT.otherKey("max_cold").optional(Integer.MAX_VALUE).alwaysWrite().exclude();
    public static final RecipeKey<Integer> MAX_HEAT = INT.otherKey("max_heat").optional(Integer.MAX_VALUE).alwaysWrite().exclude();
    public static final RecipeKey<Integer> DELTA = INT.otherKey("delta").alwaysWrite();

    // === 3. RAW Converter Components (Pass-through) ===
    // 我们使用 Object 类型并直接返回 from，这样 KubeJS 就会把原始的 JS Map/List/String 传给 RecipeJS
    // 从而让我们在 RecipeJS 中自己处理解析逻辑，避开 KubeJS 的自动转换干扰。
    
    public static final RecipeComponent<Object> RAW_COMPONENT = new RecipeComponent<Object>() {
        @Override public TypeInfo typeInfo() { return TypeInfo.of(Object.class); }
        @Override public Codec<Object> codec() { return null; } // 实际上不会用到，因为我们在 serialize 拦截了
        @Override public RecipeComponentType<?> type() { return RecipeComponentType.unit(ResourceLocation.fromNamespaceAndPath(ThermalShock.MODID, "raw"), this); }
        @Override public Object wrap(RecipeScriptContext cx, Object from) { return from; }
    };

    public static final RecipeKey<Object> CON_OUT_I = RAW_COMPONENT.outputKey("item_outputs").optional(List.of());
    public static final RecipeKey<Object> CON_OUT_F = RAW_COMPONENT.outputKey("fluid_outputs").optional(List.of()).exclude();
    public static final RecipeKey<Object> CON_IN_I = RAW_COMPONENT.inputKey("item_inputs").optional(List.of());
    public static final RecipeKey<Object> CON_IN_F = RAW_COMPONENT.inputKey("fluid_inputs").optional(List.of()).exclude();

    public static final KubeRecipeFactory FACTORY = new KubeRecipeFactory(
            ResourceLocation.fromNamespaceAndPath(ThermalShock.MODID, "recipe_js"),
            ThermalShockRecipeJS.class,
            ThermalShockRecipeJS::new
    );

    public static final RecipeSchema OVERHEATING = new RecipeSchema(RESULT, ITEM_INPUTS, BLOCK_INPUTS, MIN_HEAT, HEAT_COST).factory(FACTORY);
    public static final RecipeSchema SHOCK = new RecipeSchema(RESULT, ITEM_INPUTS, BLOCK_INPUTS, DELTA, MIN_HOT, MAX_COLD).factory(FACTORY);
    public static final RecipeSchema SHOCK_FILLING = new RecipeSchema(TARGET_ITEM, ITEM_INPUTS, BLOCK_INPUTS, DELTA, TARGET_COUNT, MIN_HOT, MAX_COLD).factory(FACTORY);
    public static final RecipeSchema EXTRACTION = new RecipeSchema(RESULT, TARGET_ITEM, ITEM_INPUTS, BLOCK_INPUTS, MIN_HEAT, HEAT_COST).factory(FACTORY);
    public static final RecipeSchema FUEL = new RecipeSchema(INGREDIENT.inputKey("ingredient"), INT.otherKey("burn_time").optional(0).alwaysWrite(), INT.otherKey("heat_rate").optional(0).alwaysWrite());
    public static final RecipeSchema CONVERTER = new RecipeSchema(CON_OUT_I, CON_IN_I, INT.otherKey("process_time").optional(20).alwaysWrite(), CON_OUT_F, CON_IN_F, MIN_HEAT, MAX_HEAT).factory(FACTORY);
}
