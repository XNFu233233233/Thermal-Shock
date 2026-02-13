package com.xnfu.thermalshock.compat.kubejs.events;

import com.xnfu.thermalshock.data.CasingData;
import dev.latvian.mods.kubejs.event.KubeEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RegisterCasingEventJS implements KubeEvent {
    private final Map<ResourceLocation, CasingData> casings = new HashMap<>();
    private final List<ResourceLocation> removals = new ArrayList<>();

    public void add(Block block, int maxHeat, int maxCold, float efficiency) {
        ResourceLocation id = block.builtInRegistryHolder().key().location();
        casings.put(id, new CasingData(maxHeat, maxCold, efficiency));
    }

    public void remove(Block block) {
        removals.add(block.builtInRegistryHolder().key().location());
    }

    public Map<ResourceLocation, CasingData> getCasings() { return casings; }
    public List<ResourceLocation> getRemovals() { return removals; }
}
