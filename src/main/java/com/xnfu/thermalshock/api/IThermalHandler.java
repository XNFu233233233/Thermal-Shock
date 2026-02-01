package com.xnfu.thermalshock.api;

import net.neoforged.neoforge.capabilities.BlockCapability;
import net.minecraft.resources.ResourceLocation;
import com.xnfu.thermalshock.ThermalShock;
import net.minecraft.core.Direction;

/**
 * 模组热力能力接口。
 * 正数代表提供热量 (Heating)，负数代表提供冷量 (Cooling)。
 */
public interface IThermalHandler {
    
    /**
     * 定义 Block Capability。
     */
    BlockCapability<IThermalHandler, Direction> INTERFACE = BlockCapability.createSided(
            ResourceLocation.fromNamespaceAndPath(ThermalShock.MODID, "thermal_handler"),
            IThermalHandler.class
    );

    /**
     * 获取当前每刻产生的热量/冷量变化率。
     * @return 热量变化率 (H/t)。正数为热，负数为冷。
     */
    int getThermalRate();
}
