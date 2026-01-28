package com.xnfu.thermalshock.registries;

import com.xnfu.thermalshock.ThermalShock;
import com.xnfu.thermalshock.data.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

public class ThermalShockDataMaps {

    // 外壳属性 (Block)
    public static final DataMapType<Block, CasingData> CASING_PROPERTY = DataMapType.builder(
            ResourceLocation.fromNamespaceAndPath(ThermalShock.MODID, "chamber_casing"),
            Registries.BLOCK, CasingData.CODEC).synced(CasingData.CODEC, true).build();

    // 催化剂属性 (Item) - 新增
    public static final DataMapType<Item, CatalystData> CATALYST_PROPERTY = DataMapType.builder(
            ResourceLocation.fromNamespaceAndPath(ThermalShock.MODID, "chamber_catalyst"),
            Registries.ITEM, CatalystData.CODEC).synced(CatalystData.CODEC, true).build();

    // 热源属性 (Block) - 新增
    public static final DataMapType<Block, HeatSourceData> HEAT_SOURCE_PROPERTY = DataMapType.builder(
            ResourceLocation.fromNamespaceAndPath(ThermalShock.MODID, "heat_source"),
            Registries.BLOCK, HeatSourceData.CODEC).synced(HeatSourceData.CODEC, true).build();

    // 冷源属性 (Block)
    public static final DataMapType<Block, ColdSourceData> COLD_SOURCE_PROPERTY = DataMapType.builder(
            ResourceLocation.fromNamespaceAndPath(ThermalShock.MODID, "cold_source"),
            Registries.BLOCK, ColdSourceData.CODEC).synced(ColdSourceData.CODEC, true).build();



    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ThermalShockDataMaps::onRegisterDataMapTypes);
    }


    private static void onRegisterDataMapTypes(RegisterDataMapTypesEvent event) {
        event.register(CASING_PROPERTY);
        event.register(CATALYST_PROPERTY);
        event.register(HEAT_SOURCE_PROPERTY);
        event.register(COLD_SOURCE_PROPERTY);
    }
}