// [模式 A：全量重写]
package com.xnfu.thermalshock.events;

import com.xnfu.thermalshock.api.IThermalHandler;
import com.xnfu.thermalshock.registries.ThermalShockBlockEntities;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public class ModBusEvents {

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        // 0. 热力能力 (Thermal Capability)
        event.registerBlockEntity(
                IThermalHandler.INTERFACE,
                ThermalShockBlockEntities.THERMAL_SOURCE_BE.get(),
                (be, context) -> be
        );

        // 1. 控制器 (只暴露 itemHandler, Slot 0/1)
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ThermalShockBlockEntities.CHAMBER_CONTROLLER_BE.get(),
                (be, context) -> be.getItemHandler()
        );

        // 2. 接口 (物品) - 使用带逻辑的 Wrapper
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ThermalShockBlockEntities.SIMULATION_PORT_BE.get(),
                (be, context) -> be.getCapabilityItemHandler()
        );

        // 3. 接口 (流体) - 使用带逻辑的 Wrapper
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ThermalShockBlockEntities.SIMULATION_PORT_BE.get(),
                (be, context) -> be.getCapabilityFluidHandler()
        );

        // 4. 发生器 - 物品输入
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ThermalShockBlockEntities.THERMAL_SOURCE_BE.get(),
                (be, context) -> be.getItemHandler()
        );

        // 5. 发生器 - 能量输入
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ThermalShockBlockEntities.THERMAL_SOURCE_BE.get(),
                (be, context) -> be.getEnergyStorage()
        );

        // 6. 转换器 - 物品
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ThermalShockBlockEntities.THERMAL_CONVERTER_BE.get(),
                (be, context) -> be.getItemHandler()
        );

        // 7. 转换器 - 流体
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ThermalShockBlockEntities.THERMAL_CONVERTER_BE.get(),
                (be, context) -> be.getFluidHandler()
        );
    }
}