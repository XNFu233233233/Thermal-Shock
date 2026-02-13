package com.xnfu.thermalshock.compat.kubejs.events;

import com.xnfu.thermalshock.data.HeatSourceData;
import dev.latvian.mods.kubejs.event.KubeEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RegisterHeatSourceEventJS implements KubeEvent {
    private final Map<ResourceLocation, HeatSourceData> data = new HashMap<>();
    private final List<ResourceLocation> removals = new ArrayList<>();

    public void add(Block block, int heatPerTick) {
        ResourceLocation id = block.builtInRegistryHolder().key().location();
        data.put(id, new HeatSourceData(heatPerTick));
    }

    public void remove(Block block) {
        removals.add(block.builtInRegistryHolder().key().location());
    }

    public Map<ResourceLocation, HeatSourceData> getData() { return data; }
    public List<ResourceLocation> getRemovals() { return removals; }
}
