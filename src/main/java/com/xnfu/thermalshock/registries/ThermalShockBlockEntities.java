package com.xnfu.thermalshock.registries;

import com.xnfu.thermalshock.ThermalShock;
import com.xnfu.thermalshock.block.entity.SimulationChamberBlockEntity; // 稍后创建
import com.xnfu.thermalshock.block.entity.SimulationPortBlockEntity;
import com.xnfu.thermalshock.block.entity.ThermalConverterBlockEntity;
import com.xnfu.thermalshock.block.entity.ThermalSourceBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ThermalShockBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, ThermalShock.MODID);

    public static final Supplier<BlockEntityType<SimulationChamberBlockEntity>> CHAMBER_CONTROLLER_BE =
            BLOCK_ENTITIES.register("simulation_chamber_controller_be", () ->
                    BlockEntityType.Builder.of(SimulationChamberBlockEntity::new,
                            ThermalShockBlocks.SIMULATION_CHAMBER_CONTROLLER.get()
                    ).build(null));

    public static final Supplier<BlockEntityType<SimulationPortBlockEntity>> SIMULATION_PORT_BE =
            BLOCK_ENTITIES.register("simulation_port_be", () ->
                    BlockEntityType.Builder.of(SimulationPortBlockEntity::new,
                            ThermalShockBlocks.SIMULATION_CHAMBER_PORT.get()
                    ).build(null));

    public static final Supplier<BlockEntityType<ThermalSourceBlockEntity>> THERMAL_SOURCE_BE =
            BLOCK_ENTITIES.register("thermal_source_be", () ->
                    BlockEntityType.Builder.of(ThermalSourceBlockEntity::new,
                            ThermalShockBlocks.THERMAL_HEATER.get(),
                            ThermalShockBlocks.THERMAL_FREEZER.get()
                    ).build(null));
    
    public static final Supplier<BlockEntityType<ThermalConverterBlockEntity>> THERMAL_CONVERTER_BE =
            BLOCK_ENTITIES.register("thermal_converter_be", () ->
                    BlockEntityType.Builder.of(ThermalConverterBlockEntity::new,
                            ThermalShockBlocks.THERMAL_CONVERTER.get()
                    ).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
