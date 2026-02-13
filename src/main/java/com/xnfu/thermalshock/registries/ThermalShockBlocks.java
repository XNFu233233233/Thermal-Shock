package com.xnfu.thermalshock.registries;

import com.xnfu.thermalshock.ThermalShock;
import com.xnfu.thermalshock.block.SimulationChamberBlock;
import com.xnfu.thermalshock.block.SimulationChamberPortBlock;
import com.xnfu.thermalshock.block.ThermalConverterBlock;
import com.xnfu.thermalshock.block.ThermalSourceBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.world.level.block.Block;

import java.util.function.Supplier;

public class ThermalShockBlocks {
        public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ThermalShock.MODID);
        public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ThermalShock.MODID);

        // 1. 控制器 (Controller)
        public static final DeferredBlock<Block> SIMULATION_CHAMBER_CONTROLLER = register("simulation_chamber_controller",
                () -> new SimulationChamberBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(5.0f).requiresCorrectToolForDrops().noOcclusion()));

        // 2. 通用接口 (Port)
        public static final DeferredBlock<Block> SIMULATION_CHAMBER_PORT = register("simulation_chamber_port",
                () -> new SimulationChamberPortBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(5.0f).requiresCorrectToolForDrops().noOcclusion()));

        // 3. 热发生器 (Heater)
        public static final DeferredBlock<Block> THERMAL_HEATER = register("thermal_heater",
                () -> new ThermalSourceBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(5.0f).requiresCorrectToolForDrops().lightLevel(state -> state.getValue(BlockStateProperties.LIT) ? 13 : 0)));

        // 4. 冷发生器 (Freezer)
        public static final DeferredBlock<Block> THERMAL_FREEZER = register("thermal_freezer",
                () -> new ThermalSourceBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(5.0f).requiresCorrectToolForDrops().lightLevel(state -> state.getValue(BlockStateProperties.LIT) ? 5 : 0)));

        // 5. 热力转换器 (Thermal Converter)
        public static final DeferredBlock<Block> THERMAL_CONVERTER = register("thermal_converter",
                () -> new ThermalConverterBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(5.0f).requiresCorrectToolForDrops().lightLevel(state -> state.getValue(BlockStateProperties.LIT) ? 13 : 0)));

        // 辅助方法
        private static <T extends Block> DeferredBlock<T> register(String name, Supplier<T> block) {
                DeferredBlock<T> toReturn = BLOCKS.register(name, block);
                ITEMS.register(name, () -> new BlockItem(toReturn.get(), new Item.Properties()));
                return toReturn;
        }

        public static void register(IEventBus eventBus) {
                BLOCKS.register(eventBus);
                ITEMS.register(eventBus);
        }

}
