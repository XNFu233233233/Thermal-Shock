package com.xnfu.thermalshock.compat.kubejs.events;

import com.xnfu.thermalshock.data.ColdSourceData;
import dev.latvian.mods.kubejs.event.KubeEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RegisterColdSourceEventJS implements KubeEvent {
    private final Map<ResourceLocation, ColdSourceData> data = new HashMap<>();
    private final List<ResourceLocation> removals = new ArrayList<>();

    public void add(Block block, int coolingPerTick) {
        ResourceLocation id = block.builtInRegistryHolder().key().location();
        data.put(id, new ColdSourceData(coolingPerTick));
    }

    public void remove(Block block) {
        removals.add(block.builtInRegistryHolder().key().location());
    }

    public Map<ResourceLocation, ColdSourceData> getData() { return data; }
    public List<ResourceLocation> getRemovals() { return removals; }
}
