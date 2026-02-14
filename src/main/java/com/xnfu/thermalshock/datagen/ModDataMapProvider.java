package com.xnfu.thermalshock.datagen;

import com.xnfu.thermalshock.data.*;
import com.xnfu.thermalshock.registries.ThermalShockDataMaps;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.data.DataMapProvider;

import java.util.concurrent.CompletableFuture;

public class ModDataMapProvider extends DataMapProvider {

    public ModDataMapProvider(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(packOutput, lookupProvider);
    }

    @Override
    protected void gather(HolderLookup.Provider lookupProvider) {
        // 开发测试
        /*// 1. 外壳 (Casing)
        var casing = this.builder(ThermalShockDataMaps.CASING_PROPERTY);
        // [更新] 参数: maxHeatRate, maxColdRate, efficiency
        casing.add(BlockTags.PLANKS, new CasingData(50, 20, 0.8f), false);
        casing.add(key(Blocks.COBBLESTONE), new CasingData(200, 100, 1.0f), false);
        casing.add(key(Blocks.IRON_BLOCK), new CasingData(1000, 500, 1.0f), false);
        casing.add(key(Blocks.OBSIDIAN), new CasingData(5000, 5000, 1.0f), false);

        // 2. 催化剂 (Catalyst)
        var catalyst = this.builder(ThermalShockDataMaps.CATALYST_PROPERTY);
        // 参数: bonusYield, catalystPoints
        catalyst.add(key(Items.IRON_INGOT), new CatalystData(0.10f, 10.0f), false);
        catalyst.add(key(Items.DIAMOND), new CatalystData(0.50f, 20.0f), false);

        // 3. 热源 (Heat Source) - 正数
        var heat = this.builder(ThermalShockDataMaps.HEAT_SOURCE_PROPERTY);
        heat.add(key(Blocks.TORCH), new HeatSourceData(2), false);
        heat.add(key(Blocks.LAVA), new HeatSourceData(20), false);
        heat.add(key(Blocks.MAGMA_BLOCK), new HeatSourceData(50), false);

        // 4. 冷源 (Cold Source)
        // [修复] 使用正数表示制冷量 (Magnitude)
        // 逻辑层会自动取负值：-5, -40
        var cold = this.builder(ThermalShockDataMaps.COLD_SOURCE_PROPERTY);
        cold.add(key(Blocks.WATER), new ColdSourceData(2), false);
        cold.add(key(Blocks.ICE), new ColdSourceData(5), false);
        cold.add(key(Blocks.BLUE_ICE), new ColdSourceData(40), false);*/

        var cold = this.builder(ThermalShockDataMaps.COLD_SOURCE_PROPERTY);
        cold.add(key(Blocks.WATER), new ColdSourceData(2), false);
    }

    private static ResourceKey<Block> key(Block block) {
        return BuiltInRegistries.BLOCK.getResourceKey(block).orElseThrow();
    }
    private static ResourceKey<Item> key(Item item) {
        return BuiltInRegistries.ITEM.getResourceKey(item).orElseThrow();
    }
}