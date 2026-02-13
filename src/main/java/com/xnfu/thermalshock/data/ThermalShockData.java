package com.xnfu.thermalshock.data;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import java.util.HashMap;
import java.util.Map;

/**
 * 运行时数据缓存
 * 供游戏逻辑查询，数据由 KubeJS 事件填充
 */
public class ThermalShockData {
    // 缓存 Map
    private static final Map<Block, CasingData> CASING_CACHE = new HashMap<>();
    private static final Map<Block, HeatSourceData> HEAT_SOURCE_CACHE = new HashMap<>();
    private static final Map<Block, ColdSourceData> COLD_SOURCE_CACHE = new HashMap<>();
    private static final Map<Item, CatalystData> CATALYST_CACHE = new HashMap<>();

    public static void clear() {
        CASING_CACHE.clear();
        HEAT_SOURCE_CACHE.clear();
        COLD_SOURCE_CACHE.clear();
        CATALYST_CACHE.clear();
    }

    public static void putCasing(Block block, CasingData data) {
        CASING_CACHE.put(block, data);
    }

    public static void putHeatSource(Block block, HeatSourceData data) {
        HEAT_SOURCE_CACHE.put(block, data);
    }

    public static void putColdSource(Block block, ColdSourceData data) {
        COLD_SOURCE_CACHE.put(block, data);
    }

    public static void putCatalyst(Item item, CatalystData data) {
        CATALYST_CACHE.put(item, data);
    }

    public static CasingData getCasing(Block block) {
        return CASING_CACHE.get(block);
    }

    public static HeatSourceData getHeatSource(Block block) {
        return HEAT_SOURCE_CACHE.get(block);
    }

    public static ColdSourceData getColdSource(Block block) {
        return COLD_SOURCE_CACHE.get(block);
    }

    public static CatalystData getCatalyst(Item item) {
        return CATALYST_CACHE.get(item);
    }
}
