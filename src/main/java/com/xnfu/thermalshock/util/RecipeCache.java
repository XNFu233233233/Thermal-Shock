package com.xnfu.thermalshock.util;

import com.xnfu.thermalshock.block.entity.MachineMode;
import com.xnfu.thermalshock.recipe.AbstractSimulationRecipe;
import com.xnfu.thermalshock.registries.ThermalShockRecipes;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RecipesUpdatedEvent;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import com.xnfu.thermalshock.ThermalShock;

import java.util.*;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

@EventBusSubscriber(modid = ThermalShock.MODID)
public class RecipeCache {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<MachineMode, List<RecipeHolder<? extends AbstractSimulationRecipe>>> CACHE = new EnumMap<>(MachineMode.class);

    @SubscribeEvent
    public static void onRecipesUpdated(RecipesUpdatedEvent event) {
        updateCache(event.getRecipeManager());
    }

    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        updateCache(event.getServerResources().getRecipeManager());
    }

    public static void updateCache(RecipeManager manager) {
        CACHE.clear();
        for (MachineMode mode : MachineMode.values()) {
            CACHE.put(mode, new ArrayList<>());
        }

        // 收集 Overheating 配方
        manager.getAllRecipesFor(ThermalShockRecipes.OVERHEATING_TYPE.get()).forEach(holder -> {
            AbstractSimulationRecipe recipe = holder.value();
            CACHE.get(recipe.getMachineMode()).add(holder);
        });

        // 收集 Thermal Shock 配方
        manager.getAllRecipesFor(ThermalShockRecipes.THERMAL_SHOCK_TYPE.get()).forEach(holder -> {
            AbstractSimulationRecipe recipe = holder.value();
            CACHE.get(recipe.getMachineMode()).add(holder);
        });
        
        LOGGER.info("Thermal Shock recipe cache updated: {} overheating, {} shock recipes found.", 
                CACHE.get(MachineMode.OVERHEATING).size(), CACHE.get(MachineMode.THERMAL_SHOCK).size());
    }

    public static List<RecipeHolder<? extends AbstractSimulationRecipe>> getRecipes(MachineMode mode) {
        return CACHE.getOrDefault(mode, Collections.emptyList());
    }
}
