package com.xnfu.thermalshock.datagen;

import com.xnfu.thermalshock.registries.ThermalShockBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.Collections;
import java.util.stream.Collectors;

public class ModBlockLootTableProvider extends BlockLootSubProvider {
    public ModBlockLootTableProvider(HolderLookup.Provider provider) {
        // 在 1.21.1 中使用所有启用的特性集
        super(Collections.emptySet(), FeatureFlags.REGISTRY.allFlags(), provider);
    }

    @Override
    protected void generate() {
        // 自动为所有方块生成掉落自身的战利品表
        dropSelf(ThermalShockBlocks.SIMULATION_CHAMBER_CONTROLLER.get());
        dropSelf(ThermalShockBlocks.SIMULATION_CHAMBER_PORT.get());
        dropSelf(ThermalShockBlocks.THERMAL_HEATER.get());
        dropSelf(ThermalShockBlocks.THERMAL_FREEZER.get());
        dropSelf(ThermalShockBlocks.THERMAL_CONVERTER.get());
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        // 显式映射以避免泛型推导错误
        return ThermalShockBlocks.BLOCKS.getEntries().stream()
                .map(DeferredHolder::get)
                .collect(Collectors.toList());
    }
}
