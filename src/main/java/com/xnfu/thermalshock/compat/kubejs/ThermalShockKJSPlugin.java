package com.xnfu.thermalshock.compat.kubejs;

import com.xnfu.thermalshock.ThermalShock;
import com.xnfu.thermalshock.data.CasingData;
import com.xnfu.thermalshock.data.CatalystData;
import com.xnfu.thermalshock.data.ColdSourceData;
import com.xnfu.thermalshock.data.HeatSourceData;
import com.xnfu.thermalshock.registries.ThermalShockDataMaps;
import com.xnfu.thermalshock.compat.kubejs.events.*;
import dev.latvian.mods.kubejs.generator.KubeDataGenerator;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchemaRegistry;
import dev.latvian.mods.kubejs.event.EventGroupRegistry;
import dev.latvian.mods.kubejs.script.BindingRegistry;
import dev.latvian.mods.kubejs.script.ScriptType;
import net.minecraft.core.registries.BuiltInRegistries;

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
        ns.register("extraction", ThermalShockKJSSchemas.EXTRACTION);
        ns.shaped("clump_filling");
        ns.getRegisteredOrThrow("clump_filling").schema.factory(ThermalShockKJSSchemas.FACTORY);
    }

    @Override
    public void registerEvents(EventGroupRegistry registry) {
        // 注册事件组，这样 JS 侧才能直接通过名字使用事件
        registry.register(ThermalShockEvents.GROUP);
    }

    @Override
    public void registerBindings(BindingRegistry bindings) {
        // 移除 ThermalShockEvents 的绑定，因为它会由 EventGroup 自动处理
        bindings.add("ThermalShockDataMaps", ThermalShockDataMaps.class);
        bindings.add("CasingData", CasingData.class);
        bindings.add("CatalystData", CatalystData.class);
        bindings.add("HeatSourceData", HeatSourceData.class);
        bindings.add("ColdSourceData", ColdSourceData.class);
    }

    @Override
    public void generateData(KubeDataGenerator generator) {
        // 1. Casing Data
        var casingEvent = new RegisterCasingEventJS();
        // 明确使用 ScriptType.SERVER 触发（因为您的脚本在 server_scripts 下）
        ThermalShockEvents.REGISTER_CASING.post(ScriptType.SERVER, casingEvent);
        generator.dataMap(ThermalShockDataMaps.CASING_PROPERTY, map -> {
            casingEvent.getCasings().forEach((id, data) -> map.add(BuiltInRegistries.BLOCK.get(id), data));
            casingEvent.getRemovals().forEach(id -> map.remove(BuiltInRegistries.BLOCK.get(id)));
        });

        // 2. Catalyst Data
        var catalystEvent = new RegisterCatalystEventJS();
        ThermalShockEvents.REGISTER_CATALYST.post(ScriptType.SERVER, catalystEvent);
        generator.dataMap(ThermalShockDataMaps.CATALYST_PROPERTY, map -> {
            catalystEvent.getData().forEach((id, data) -> map.add(BuiltInRegistries.ITEM.get(id), data));
            catalystEvent.getRemovals().forEach(id -> map.remove(BuiltInRegistries.ITEM.get(id)));
        });

        // 3. Heat Source Data
        var heatEvent = new RegisterHeatSourceEventJS();
        ThermalShockEvents.REGISTER_HEAT_SOURCE.post(ScriptType.SERVER, heatEvent);
        generator.dataMap(ThermalShockDataMaps.HEAT_SOURCE_PROPERTY, map -> {
            heatEvent.getData().forEach((id, data) -> map.add(BuiltInRegistries.BLOCK.get(id), data));
            heatEvent.getRemovals().forEach(id -> map.remove(BuiltInRegistries.BLOCK.get(id)));
        });

        // 4. Cold Source Data
        var coldEvent = new RegisterColdSourceEventJS();
        ThermalShockEvents.REGISTER_COLD_SOURCE.post(ScriptType.SERVER, coldEvent);
        generator.dataMap(ThermalShockDataMaps.COLD_SOURCE_PROPERTY, map -> {
            coldEvent.getData().forEach((id, data) -> map.add(BuiltInRegistries.BLOCK.get(id), data));
            coldEvent.getRemovals().forEach(id -> map.remove(BuiltInRegistries.BLOCK.get(id)));
        });
    }
}
