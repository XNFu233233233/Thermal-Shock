package com.xnfu.thermalshock.registries;

import com.xnfu.thermalshock.ThermalShock;
import com.xnfu.thermalshock.recipe.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ThermalShockRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, ThermalShock.MODID);

    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, ThermalShock.MODID);

    // =========================================
    // 类型 A: 过热模式
    // =========================================
    public static final DeferredHolder<RecipeType<?>, RecipeType<OverheatingRecipe>> OVERHEATING_TYPE =
            RECIPE_TYPES.register("overheating", () -> new RecipeType<OverheatingRecipe>() {
                @Override public String toString() { return "thermalshock:overheating"; }
            });

    public static final DeferredHolder<RecipeSerializer<?>, OverheatingRecipe.Serializer> OVERHEATING_SERIALIZER =
            SERIALIZERS.register("overheating", OverheatingRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, ClumpProcessingRecipe.Serializer> CLUMP_PROCESSING_SERIALIZER =
            SERIALIZERS.register("clump_processing", ClumpProcessingRecipe.Serializer::new);

    // =========================================
    // 类型 B: 热冲击模式
    // =========================================
    public static final DeferredHolder<RecipeType<?>, RecipeType<ThermalShockRecipe>> THERMAL_SHOCK_TYPE =
            RECIPE_TYPES.register("thermal_shock", () -> new RecipeType<ThermalShockRecipe>() {
                @Override public String toString() { return "thermalshock:thermal_shock"; }
            });

    public static final DeferredHolder<RecipeSerializer<?>, ThermalShockRecipe.Serializer> THERMAL_SHOCK_SERIALIZER =
            SERIALIZERS.register("thermal_shock", ThermalShockRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, ThermalShockFillingRecipe.Serializer> THERMAL_SHOCK_FILLING_SERIALIZER =
            SERIALIZERS.register("thermal_shock_filling", ThermalShockFillingRecipe.Serializer::new);

    // =========================================
    // 工作台配方
    // =========================================
    // [修复] 使用自定义 Serializer
    public static final DeferredHolder<RecipeSerializer<?>, ClumpFillingRecipe.Serializer> CLUMP_FILLING_SERIALIZER =
            SERIALIZERS.register("clump_filling", ClumpFillingRecipe.Serializer::new);


    // =========================================
    // 类型 C: 燃料配方
    // =========================================
    public static final net.neoforged.neoforge.registries.DeferredHolder<RecipeType<?>, RecipeType<ThermalFuelRecipe>> THERMAL_FUEL_TYPE =
            RECIPE_TYPES.register("thermal_fuel", () -> new RecipeType<ThermalFuelRecipe>() {
                @Override public String toString() { return "thermalshock:thermal_fuel"; }
            });

    public static final net.neoforged.neoforge.registries.DeferredHolder<RecipeSerializer<?>, ThermalFuelRecipe.Serializer> THERMAL_FUEL_SERIALIZER =
            SERIALIZERS.register("thermal_fuel", ThermalFuelRecipe.Serializer::new);


    public static void register(IEventBus eventBus) {
        SERIALIZERS.register(eventBus);
        RECIPE_TYPES.register(eventBus);
    }
}