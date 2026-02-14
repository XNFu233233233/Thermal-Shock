package com.xnfu.thermalshock;

import com.xnfu.thermalshock.events.CommonModEvents;
import com.xnfu.thermalshock.events.ModBusEvents;
import com.xnfu.thermalshock.network.*;
import com.xnfu.thermalshock.registries.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

@Mod(ThermalShock.MODID)
public class ThermalShock {
    public static final String MODID = "thermalshock";
    private static final Logger LOGGER = LogUtils.getLogger();

    public ThermalShock(IEventBus modEventBus) {
        LOGGER.info("Thermal Shock is initializing...");

        // =========================================
        // 1. 内容注册 (Registries)
        // =========================================
        ThermalShockBlocks.register(modEventBus);           // 方块 + 物品
        ThermalShockItems.register(modEventBus);            // 物品
        ThermalShockDataComponents.register(modEventBus);   // 数据组件
        ThermalShockBlockEntities.register(modEventBus);    // BE
        ThermalShockCreativeTabs.register(modEventBus);     // 创造模式标签
        ThermalShockDataMaps.register(modEventBus);         // Data Maps
        ThermalShockRecipes.register(modEventBus);          // 配方类型

        ThermalShockMenus.register(modEventBus);

        // =========================================
        // 2. 模组生命周期事件 (Mod Event Bus)
        // =========================================
        modEventBus.register(Config.class);

        // 功能能力 (Capabilities) - 使用方法引用注册
        modEventBus.addListener(ModBusEvents::registerCapabilities);

        // 注册网络包
        modEventBus.addListener(this::registerPackets);

        // =========================================
        // 3. 游戏内逻辑事件 (NeoForge Event Bus)
        // =========================================
        // 全局游戏事件 (破坏方块、Tick) - 只有服务端逻辑需要放这里
        NeoForge.EVENT_BUS.register(CommonModEvents.class);

        LOGGER.info("Thermal Shock setup complete!");
    }

    private void registerPackets(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");
        
        // --- 仅发送给服务端的消息 (Player Actions) ---
        registrar.playToServer(
                PacketSelectRecipe.TYPE,
                PacketSelectRecipe.STREAM_CODEC,
                PacketSelectRecipe::handle
        );
        registrar.playToServer(
                PacketToggleLock.TYPE,
                PacketToggleLock.STREAM_CODEC,
                PacketToggleLock::handle
        );
        registrar.playToServer(
                PacketToggleMode.TYPE,
                PacketToggleMode.STREAM_CODEC,
                PacketToggleMode::handle
        );
        registrar.playToServer(
                PacketTogglePortMode.TYPE,
                PacketTogglePortMode.STREAM_CODEC,
                PacketTogglePortMode::handle
        );
        registrar.playToServer(
                PacketSetTargetHeat.TYPE,
                PacketSetTargetHeat.STREAM_CODEC,
                PacketSetTargetHeat::handle
        );

        // --- 仅发送给客户端的消息 (Machine Sync) ---
        registrar.playToClient(
                PacketSyncMachineStatus.TYPE,
                PacketSyncMachineStatus.STREAM_CODEC,
                PacketSyncMachineStatus::handle
        );
    }
}