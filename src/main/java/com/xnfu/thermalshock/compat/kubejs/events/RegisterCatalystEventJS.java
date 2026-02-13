package com.xnfu.thermalshock.compat.kubejs.events;

import com.xnfu.thermalshock.data.CatalystData;
import dev.latvian.mods.kubejs.event.KubeEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RegisterCatalystEventJS implements KubeEvent {
    private final Map<ResourceLocation, CatalystData> data = new HashMap<>();
    private final List<ResourceLocation> removals = new ArrayList<>();

    public void add(Item item, float bonus, float buffer) {
        ResourceLocation id = item.builtInRegistryHolder().key().location();
        data.put(id, new CatalystData(bonus, buffer));
    }

    public void remove(Item item) {
        removals.add(item.builtInRegistryHolder().key().location());
    }

    public Map<ResourceLocation, CatalystData> getData() { return data; }
    public List<ResourceLocation> getRemovals() { return removals; }
}
