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
                
                // 1. 状态与模式合并 (Status & Mode)
                if (data.contains("IsFormed") && data.contains("MachineMode")) {
                    boolean formed = data.getBoolean("IsFormed");
                    Component statusComp = formed ? Component.translatable("gui.thermalshock.status.valid").withStyle(ChatFormatting.GREEN)
                                                : Component.translatable("gui.thermalshock.status.invalid").withStyle(ChatFormatting.RED);
                    
                    Component modeComp = Component.literal(data.getString("MachineMode")).withStyle(ChatFormatting.AQUA);
                    
                    tooltip.add(Component.translatable("jade.thermalshock.status", statusComp)
                            .append(Component.literal(" | ")).withStyle(ChatFormatting.GRAY)
                            .append(Component.translatable("jade.thermalshock.mode", modeComp)));
                } else {
                    // Fallback separate
                     if (data.contains("IsFormed")) {
                        boolean formed = data.getBoolean("IsFormed");
                        tooltip.add(Component.translatable("jade.thermalshock.status",
                                formed ? Component.translatable("gui.thermalshock.status.valid").withStyle(ChatFormatting.GREEN)
                                       : Component.translatable("gui.thermalshock.status.invalid").withStyle(ChatFormatting.RED)));
                    }
                    if (data.contains("MachineMode")) {
                        tooltip.add(Component.translatable("jade.thermalshock.mode", 
                                Component.literal(data.getString("MachineMode")).withStyle(ChatFormatting.AQUA)));
                    }
                }

                // 2. 热量与温差合并 (Heat & Delta)
                if (data.contains("Heat") && data.contains("Delta")) {
                    Component heatComp = Component.translatable("jade.thermalshock.heat", data.getInt("Heat")).withStyle(ChatFormatting.GOLD);
                    Component deltaComp = Component.translatable("jade.thermalshock.delta", data.getInt("Delta")).withStyle(ChatFormatting.LIGHT_PURPLE);
                    
                    // Simple concatenation: "Heat: 100 H   Delta: 50 H"
                    tooltip.add(heatComp.copy().append("   ").append(deltaComp));
                } else {
                    if (data.contains("Heat")) {
                        tooltip.add(Component.translatable("jade.thermalshock.heat", data.getInt("Heat")).withStyle(ChatFormatting.GOLD));
                    }
                    if (data.contains("Delta")) {
                        tooltip.add(Component.translatable("jade.thermalshock.delta", data.getInt("Delta")).withStyle(ChatFormatting.LIGHT_PURPLE));
                    }
                }

                // 3. 输入率 (Net Input) - Keep separate or combine with MaxBatch? Keep separate for clarity.
                if (data.contains("NetInput")) {
                    tooltip.add(Component.translatable("jade.thermalshock.net_input", data.getInt("NetInput")).withStyle(ChatFormatting.YELLOW));
                }

                // 4. 批处理与锁定 (Batch & Exact)
                if (data.contains("MaxBatch")) {
                     Component batchComp = Component.translatable("jade.thermalshock.max_batch", data.getInt("MaxBatch")).withStyle(ChatFormatting.WHITE);
                     
                     if (data.contains("Locked")) {
                         boolean locked = data.getBoolean("Locked");
                         Component lockComp = Component.translatable("jade.thermalshock.recipe_locked",
                                locked ? Component.translatable("tooltip.thermalshock.locked").withStyle(ChatFormatting.RED)
                                       : Component.translatable("tooltip.thermalshock.unlocked").withStyle(ChatFormatting.GRAY));
                         
                         tooltip.add(batchComp.copy().append("   ").append(lockComp));
                     } else {
                         tooltip.add(batchComp);
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
                data.putInt("MaxBatch", be.getMaxBatchSize());
                data.putBoolean("Locked", be.isRecipeLocked());
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
