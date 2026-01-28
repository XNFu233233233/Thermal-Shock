package com.xnfu.thermalshock.registries;

import com.xnfu.thermalshock.ThermalShock;
import com.xnfu.thermalshock.item.MaterialClumpItem;
import com.xnfu.thermalshock.item.SimulationUpgradeItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ThermalShockItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ThermalShock.MODID);

    public static final DeferredItem<MaterialClumpItem> MATERIAL_CLUMP = ITEMS.register("material_clump",
            () -> new MaterialClumpItem(new Item.Properties()));

    public static final DeferredItem<SimulationUpgradeItem> SIMULATION_UPGRADE = ITEMS.register("simulation_upgrade",
            () -> new SimulationUpgradeItem(new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}