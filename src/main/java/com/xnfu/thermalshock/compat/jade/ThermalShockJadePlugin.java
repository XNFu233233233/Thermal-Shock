package com.xnfu.thermalshock.compat.jade;

import com.xnfu.thermalshock.ThermalShock;
import com.xnfu.thermalshock.block.SimulationChamberBlock;
import com.xnfu.thermalshock.block.ThermalConverterBlock;
import com.xnfu.thermalshock.block.ThermalSourceBlock;
import com.xnfu.thermalshock.block.entity.SimulationChamberBlockEntity;
import com.xnfu.thermalshock.block.entity.ThermalConverterBlockEntity;
import com.xnfu.thermalshock.block.entity.ThermalSourceBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.*;
import snownee.jade.api.config.IPluginConfig;

@WailaPlugin
public class ThermalShockJadePlugin implements IWailaPlugin {
    
    @Override
    public void register(IWailaCommonRegistration registration) {
        // --- 1. Simulation Chamber ---
        registration.registerBlockDataProvider(SimulationChamberHandler.INSTANCE, SimulationChamberBlockEntity.class);

        // --- 2. Thermal Source ---
        registration.registerBlockDataProvider(ThermalSourceHandler.INSTANCE, ThermalSourceBlockEntity.class);

        // --- 3. Thermal Converter ---
        registration.registerBlockDataProvider(ThermalConverterHandler.INSTANCE, ThermalConverterBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        // --- 1. Simulation Chamber ---
        registration.registerBlockComponent(SimulationChamberHandler.INSTANCE, SimulationChamberBlock.class);

        // --- 2. Thermal Source ---
        registration.registerBlockComponent(ThermalSourceHandler.INSTANCE, ThermalSourceBlock.class);

        // --- 3. Thermal Converter ---
        registration.registerBlockComponent(ThermalConverterHandler.INSTANCE, ThermalConverterBlock.class);
    }

    // ==========================================
    // 1. Simulation Chamber Handler
    // ==========================================
    public enum SimulationChamberHandler implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
        INSTANCE;

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (accessor.getServerData() != null) {
                var data = accessor.getServerData();
                
                // 1. 结构状态 (Status)
                if (data.contains("IsFormed")) {
                    boolean formed = data.getBoolean("IsFormed");
                    Component statusComp = formed ? Component.translatable("gui.thermalshock.status.valid").withStyle(ChatFormatting.GREEN)
                                                : Component.translatable("gui.thermalshock.status.invalid").withStyle(ChatFormatting.RED);
                    tooltip.add(Component.translatable("jade.thermalshock.status").append(statusComp));
                }

                // 2. 机器模式 (Machine Mode)
                if (data.contains("MachineMode")) {
                    Component modeComp = Component.literal(data.getString("MachineMode")).withStyle(ChatFormatting.AQUA);
                    tooltip.add(Component.translatable("jade.thermalshock.mode").append(modeComp));
                }

                // 3. 最大批处理 (Max Batch)
                if (data.contains("MaxBatch")) {
                    tooltip.add(Component.translatable("jade.thermalshock.max_batch", data.getInt("MaxBatch")));
                }

                // 4. 配方锁定 (Recipe Locked)
                if (data.contains("Locked")) {
                    boolean locked = data.getBoolean("Locked");
                    Component lockComp = locked ? Component.translatable("tooltip.thermalshock.locked").withStyle(ChatFormatting.RED)
                                               : Component.translatable("tooltip.thermalshock.unlocked").withStyle(ChatFormatting.GRAY);
                    tooltip.add(Component.translatable("jade.thermalshock.recipe_locked").append(lockComp));
                }

                // 5. 模式特定信息
                if (data.contains("RawMode")) {
                    int mode = data.getInt("RawMode");
                    if (mode == 0) { // OVERHEATING
                        if (data.contains("Heat")) {
                            tooltip.add(Component.translatable("jade.thermalshock.heat", data.getInt("Heat")).withStyle(ChatFormatting.GOLD));
                        }
                        if (data.contains("NetInput")) {
                            tooltip.add(Component.translatable("jade.thermalshock.net_input", data.getInt("NetInput")).withStyle(ChatFormatting.YELLOW));
                        }
                    } else if (mode == 1) { // THERMAL_SHOCK
                        if (data.contains("Delta")) {
                            tooltip.add(Component.translatable("jade.thermalshock.delta", data.getInt("Delta")).withStyle(ChatFormatting.LIGHT_PURPLE));
                        }
                    }
                }
            }
        }

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (accessor.getBlockEntity() instanceof SimulationChamberBlockEntity be) {
                data.putBoolean("IsFormed", be.isFormed());
                data.putString("MachineMode", Component.translatable("gui.thermalshock.mode." + be.getMode().name().toLowerCase()).getString());
                data.putInt("Heat", be.getHeat());
                data.putInt("Delta", be.getDelta());
                data.putInt("NetInput", be.getNetInputRate());
                data.putInt("HighTemp", be.getThermo().getCurrentHighTemp());
                data.putInt("LowTemp", be.getThermo().getCurrentLowTemp());
                data.putInt("MaxBatch", be.getMaxBatchSize());
                data.putBoolean("Locked", be.isRecipeLocked());
                data.putInt("RawMode", be.getMode().ordinal());
            }
        }

        @Override
        public ResourceLocation getUid() {
            return ResourceLocation.fromNamespaceAndPath(ThermalShock.MODID, "simulation_chamber");
        }
    }

    // ==========================================
    // 2. Thermal Source Handler
    // ==========================================
    public enum ThermalSourceHandler implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
        INSTANCE;

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (accessor.getServerData() != null) {
                var data = accessor.getServerData();
                if (data.contains("Output")) {
                    tooltip.add(Component.translatable("jade.thermalshock.output", data.getInt("Output")).withStyle(ChatFormatting.GOLD));
                }
                if (data.contains("Energy") && data.contains("MaxEnergy")) {
                    tooltip.add(Component.translatable("jade.thermalshock.energy", data.getLong("Energy"), data.getLong("MaxEnergy")).withStyle(ChatFormatting.RED));
                }
            }
        }

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (accessor.getBlockEntity() instanceof ThermalSourceBlockEntity be) {
                data.putInt("Output", be.getCurrentHeatOutput());
                data.putLong("Energy", be.getEnergyStorage().getEnergyStored());
                data.putLong("MaxEnergy", be.getEnergyStorage().getMaxEnergyStored());
            }
        }

        @Override
        public ResourceLocation getUid() {
            return ResourceLocation.fromNamespaceAndPath(ThermalShock.MODID, "thermal_source");
        }
    }

    // ==========================================
    // 3. Thermal Converter Handler
    // ==========================================
    public enum ThermalConverterHandler implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
        INSTANCE;

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (accessor.getServerData() != null) {
                var data = accessor.getServerData();
                if (data.contains("HeatInput")) {
                    tooltip.add(Component.translatable("jade.thermalshock.net_input", data.getInt("HeatInput")).withStyle(ChatFormatting.YELLOW));
                }
            }
        }

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (accessor.getBlockEntity() instanceof ThermalConverterBlockEntity be) {
                data.putInt("HeatInput", be.getCachedHeatInput());
            }
        }

        @Override
        public ResourceLocation getUid() {
            return ResourceLocation.fromNamespaceAndPath(ThermalShock.MODID, "thermal_converter");
        }
    }
}