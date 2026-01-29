package com.xnfu.thermalshock.registries;

import com.xnfu.thermalshock.ThermalShock;
import com.xnfu.thermalshock.client.gui.SimulationChamberMenu;
import com.xnfu.thermalshock.client.gui.SimulationPortMenu;
import com.xnfu.thermalshock.client.gui.ThermalConverterMenu;
import com.xnfu.thermalshock.client.gui.ThermalSourceMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ThermalShockMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, ThermalShock.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<SimulationChamberMenu>> SIMULATION_CHAMBER_MENU =
            MENUS.register("simulation_chamber_menu", () -> IMenuTypeExtension.create(SimulationChamberMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<SimulationPortMenu>> SIMULATION_PORT_MENU =
            MENUS.register("simulation_port_menu", () -> IMenuTypeExtension.create(SimulationPortMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<ThermalSourceMenu>> THERMAL_SOURCE_MENU =
            MENUS.register("thermal_source_menu", () ->IMenuTypeExtension.create(ThermalSourceMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<ThermalConverterMenu>> THERMAL_CONVERTER_MENU =
            MENUS.register("thermal_converter_menu", () ->IMenuTypeExtension.create(ThermalConverterMenu::new));

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}