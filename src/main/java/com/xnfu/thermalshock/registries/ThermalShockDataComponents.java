package com.xnfu.thermalshock.registries;

import com.xnfu.thermalshock.ThermalShock;
import com.xnfu.thermalshock.data.ClumpInfo;
import com.xnfu.thermalshock.block.entity.MachineMode;
import com.xnfu.thermalshock.block.entity.PortMode;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ThermalShockDataComponents {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, ThermalShock.MODID);

    // 存储目标产物 (Clump 专用)
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ClumpInfo>> TARGET_OUTPUT =
            DATA_COMPONENTS.register("target_output", () -> DataComponentType.<ClumpInfo>builder()
                    .persistent(ClumpInfo.CODEC)
                    .networkSynchronized(ClumpInfo.STREAM_CODEC)
                    .build());

    // 机器模式
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<MachineMode>> MACHINE_MODE =
            DATA_COMPONENTS.register("machine_mode", () -> DataComponentType.<MachineMode>builder()
                    .persistent(MachineMode.CODEC)
                    .networkSynchronized(MachineMode.STREAM_CODEC)
                    .build());

    // 当前热量
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> HEAT_LEVEL =
            DATA_COMPONENTS.register("heat_level", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());

    // 选中的配方 ID
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ResourceLocation>> SELECTED_RECIPE =
            DATA_COMPONENTS.register("selected_recipe", () -> DataComponentType.<ResourceLocation>builder()
                    .persistent(ResourceLocation.CODEC)
                    .networkSynchronized(ResourceLocation.STREAM_CODEC)
                    .build());

    // 锁定状态
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> IS_LOCKED =
            DATA_COMPONENTS.register("is_locked", () -> DataComponentType.<Boolean>builder()
                    .persistent(Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL)
                    .build());

    // 端口模式
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<PortMode>> PORT_MODE =
            DATA_COMPONENTS.register("port_mode", () -> DataComponentType.<PortMode>builder()
                    .persistent(PortMode.CODEC)
                    .networkSynchronized(PortMode.STREAM_CODEC)
                    .build());

    public static void register(IEventBus bus) {
        DATA_COMPONENTS.register(bus);
    }
}