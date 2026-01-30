package com.xnfu.thermalshock.datagen;

import com.xnfu.thermalshock.ThermalShock;
import com.xnfu.thermalshock.registries.ThermalShockItems;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class ModItemModelProvider extends ItemModelProvider {

    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, ThermalShock.MODID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        withExistingParent(ThermalShockItems.MATERIAL_CLUMP.getId().getPath(), "item/generated")
                .texture("layer0", modLoc("item/material_clump"));

        basicItem(ThermalShockItems.SIMULATION_UPGRADE.get());
        basicItem(ThermalShockItems.OVERCLOCK_UPGRADE.get());
    }
}