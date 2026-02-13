package com.xnfu.thermalshock.compat.kubejs.events;

import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;

public class ThermalShockEvents {
    public static final EventGroup GROUP = EventGroup.of("ThermalShockEvents");

    // Change startup to server to match the script type and plugin posting
    public static final EventHandler REGISTER_CASING = GROUP.server("registerCasing", () -> RegisterCasingEventJS.class);
    public static final EventHandler REGISTER_CATALYST = GROUP.server("registerCatalyst", () -> RegisterCatalystEventJS.class);
    public static final EventHandler REGISTER_HEAT_SOURCE = GROUP.server("registerHeatSource", () -> RegisterHeatSourceEventJS.class);
    public static final EventHandler REGISTER_COLD_SOURCE = GROUP.server("registerColdSource", () -> RegisterColdSourceEventJS.class);
}
