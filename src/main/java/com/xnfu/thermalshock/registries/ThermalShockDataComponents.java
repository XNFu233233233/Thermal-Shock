package com.xnfu.thermalshock.registries;

import com.xnfu.thermalshock.ThermalShock;
import com.xnfu.thermalshock.data.ClumpInfo;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ThermalShockDataComponents {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, ThermalShock.MODID);

    // 存储目标产物
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ClumpInfo>> TARGET_OUTPUT =
            DATA_COMPONENTS.register("target_output", () -> DataComponentType.<ClumpInfo>builder()
                    .persistent(ClumpInfo.CODEC)
                    .networkSynchronized(ClumpInfo.STREAM_CODEC)
                    .build());

    public static void register(IEventBus bus) {
        DATA_COMPONENTS.register(bus);
    }
}