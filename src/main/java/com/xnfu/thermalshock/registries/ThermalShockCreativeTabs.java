package com.xnfu.thermalshock.registries;

import com.xnfu.thermalshock.ThermalShock;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ThermalShockCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ThermalShock.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> THERMAL_SHOCK_TAB = CREATIVE_MODE_TABS.register("thermal_shock_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ThermalShockBlocks.SIMULATION_CHAMBER_CONTROLLER.get()))
                    .title(Component.translatable("itemGroup.thermalshock"))
                    .displayItems((parameters, output) -> {
                        output.accept(ThermalShockBlocks.SIMULATION_CHAMBER_CONTROLLER.get());
                        output.accept(ThermalShockBlocks.SIMULATION_CHAMBER_PORT.get());
                        output.accept(ThermalShockBlocks.THERMAL_HEATER);
                        output.accept(ThermalShockBlocks.THERMAL_FREEZER);
                        output.accept(ThermalShockBlocks.THERMAL_CONVERTER);
                        output.accept(ThermalShockItems.MATERIAL_CLUMP);
                        output.accept(ThermalShockItems.SIMULATION_UPGRADE);
                        output.accept(ThermalShockItems.OVERCLOCK_UPGRADE);

                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}