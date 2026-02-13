package com.xnfu.thermalshock.datagen;

import com.xnfu.thermalshock.ThermalShock;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = ThermalShock.MODID)
public class DataGenerators {

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

        // Server Data Providers (测试使用)
        generator.addProvider(event.includeServer(), new ModBlockTagsProvider(packOutput, lookupProvider, existingFileHelper));
        //generator.addProvider(event.includeServer(), new ModDataMapProvider(packOutput, lookupProvider));
        generator.addProvider(event.includeServer(), new ModRecipeProvider(packOutput, lookupProvider));
        generator.addProvider(event.includeServer(), new net.minecraft.data.loot.LootTableProvider(packOutput, java.util.Collections.emptySet(), 
                java.util.List.of(new net.minecraft.data.loot.LootTableProvider.SubProviderEntry(ModBlockLootTableProvider::new, net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.BLOCK)), lookupProvider));

        // Client Data Providers
        generator.addProvider(event.includeServer(), new ModCnLangProvider(packOutput));
        generator.addProvider(event.includeServer(), new ModEnLangProvider(packOutput));

        // BlockState & Block Models
        generator.addProvider(event.includeClient(), new ModBlockStateProvider(packOutput, existingFileHelper));

        // Item Models
        generator.addProvider(event.includeClient(), new ModItemModelProvider(packOutput, existingFileHelper));
    }
}