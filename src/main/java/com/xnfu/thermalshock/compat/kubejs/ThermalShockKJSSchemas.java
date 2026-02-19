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
import java.util.Map;

public class ThermalShockKJSSchemas {

    public static final RecipeComponent<ItemStack> ITEM_STACK = ItemStackComponent.ITEM_STACK.instance();
    public static final RecipeComponent<Ingredient> INGREDIENT = IngredientComponent.INGREDIENT.instance();
    public static final RecipeComponent<String> ID_STR = StringComponent.ID.instance();
    public static final RecipeComponent<Integer> INT = NumberComponent.INT;
    public static final RecipeComponent<FluidStack> FLUID_STACK = FluidStackComponent.FLUID_STACK.instance();
    public static final RecipeComponent<String> ANY_STR = StringComponent.STRING.instance();

    // === 3. RAW Converter Components (Pass-through) ===
    public static final RecipeComponent<Object> RAW_COMPONENT = new RecipeComponent<Object>() {
        @Override public TypeInfo typeInfo() { return TypeInfo.of(Object.class); }
        @Override public Codec<Object> codec() { return null; }
        @Override public RecipeComponentType<?> type() { return RecipeComponentType.unit(ResourceLocation.fromNamespaceAndPath(ThermalShock.MODID, "raw"), this); }
        @Override public Object wrap(RecipeScriptContext cx, Object from) { return from; }
        @Override public boolean hasPriority(dev.latvian.mods.kubejs.recipe.filter.RecipeMatchContext cx, Object from) { return true; }
    };

    public static final RecipeKey<ItemStack> RESULT = ITEM_STACK.outputKey("result");
    public static final RecipeKey<String> TARGET_ITEM = ID_STR.outputKey("target_item"); 
    public static final RecipeKey<Integer> TARGET_COUNT = INT.otherKey("target_count").optional(1).alwaysWrite().exclude();

    // 统一输入/输出 Key 为单数 (KubeJS 方法名会自动变为 itemInput 等)
    public static final RecipeKey<List<Ingredient>> ITEM_INPUT = INGREDIENT.asList().withBounds(IntBounds.of(0, 9)).inputKey("item_input");
    public static final RecipeKey<List<String>> BLOCK_INPUT = ID_STR.asList().withBounds(IntBounds.of(0, 9)).inputKey("block_input");

    // 统一热量参数名
    public static final RecipeKey<Integer> MIN_HEAT = INT.otherKey("min_heat").optional(0).alwaysWrite().exclude();
    public static final RecipeKey<Integer> MAX_HEAT = INT.otherKey("max_heat").optional(Integer.MAX_VALUE).alwaysWrite().exclude();
    public static final RecipeKey<Integer> HEAT_COST = INT.otherKey("heat_cost").optional(100).alwaysWrite().exclude();
    
    public static final RecipeKey<Integer> MAX_COLD = INT.otherKey("max_cold").optional(Integer.MAX_VALUE).alwaysWrite().exclude();
    public static final RecipeKey<Integer> DELTA = INT.otherKey("delta").alwaysWrite();

    // Converter 专用 (使用单数 Key)
    public static final RecipeKey<Object> CON_OUT_I = RAW_COMPONENT.outputKey("item_output").optional(List.of());
    public static final RecipeKey<Object> CON_OUT_F = RAW_COMPONENT.outputKey("fluid_output").optional(List.of()).exclude();
    public static final RecipeKey<Object> CON_IN_I = RAW_COMPONENT.inputKey("item_input").optional(List.of());
    public static final RecipeKey<Object> CON_IN_F = RAW_COMPONENT.inputKey("fluid_input").optional(List.of()).exclude();

    public static final KubeRecipeFactory FACTORY = new KubeRecipeFactory(
            ResourceLocation.fromNamespaceAndPath(ThermalShock.MODID, "recipe_js"),
            ThermalShockRecipeJS.class,
            ThermalShockRecipeJS::new
    );

    // 更新 Schema 定义，统一使用 MIN_HEAT 代替 MIN_HOT
    public static final RecipeSchema OVERHEATING = new RecipeSchema(RESULT, ITEM_INPUT, BLOCK_INPUT, MIN_HEAT, HEAT_COST).factory(FACTORY);
    public static final RecipeSchema SHOCK = new RecipeSchema(RESULT, ITEM_INPUT, BLOCK_INPUT, DELTA, MIN_HEAT, MAX_COLD).factory(FACTORY);
    public static final RecipeSchema SHOCK_FILLING = new RecipeSchema(TARGET_ITEM, ITEM_INPUT, BLOCK_INPUT, DELTA, TARGET_COUNT, MIN_HEAT, MAX_COLD).factory(FACTORY);
    public static final RecipeSchema EXTRACTION = new RecipeSchema(RESULT, TARGET_ITEM, ITEM_INPUT, BLOCK_INPUT, MIN_HEAT, HEAT_COST).factory(FACTORY);
    public static final RecipeSchema FUEL = new RecipeSchema(INGREDIENT.inputKey("ingredient"), INT.otherKey("burn_time").optional(0).alwaysWrite(), INT.otherKey("heat_rate").optional(0).alwaysWrite());
    public static final RecipeSchema CONVERTER = new RecipeSchema(CON_OUT_I, CON_IN_I, INT.otherKey("process_time").optional(20).alwaysWrite(), CON_OUT_F, CON_IN_F, MIN_HEAT, MAX_HEAT).factory(FACTORY);
}
