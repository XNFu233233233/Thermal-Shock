package com.xnfu.thermalshock.events;

import com.xnfu.thermalshock.ThermalShock;
import com.xnfu.thermalshock.registries.ThermalShockBlocks;
import com.xnfu.thermalshock.registries.ThermalShockDataMaps;
import com.xnfu.thermalshock.registries.ThermalShockTags;
import com.xnfu.thermalshock.util.StructureManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.level.PistonEvent;

@EventBusSubscriber(modid = ThermalShock.MODID)
public class CommonModEvents {

    /**
     * 核心过滤器：判断方块是否为多方块结构的关键组件
     * 用于决定是否触发 StructureManager 的检查
     */
    private static boolean isRelevantBlock(BlockState state) {
        if (state.isAir()) return false;

        Block block = state.getBlock();

        // 1. 核心机器组件
        if (block == ThermalShockBlocks.SIMULATION_CHAMBER_CONTROLLER.get() ||
                block == ThermalShockBlocks.SIMULATION_CHAMBER_PORT.get() ||
                block == ThermalShockBlocks.THERMAL_HEATER.get() ||
                block == ThermalShockBlocks.THERMAL_FREEZER.get()) {
            return true;
        }

        // 2. DataMap 检查 (外壳材质、热源、冷源)
        var holder = state.getBlockHolder();
        if (holder.getData(ThermalShockDataMaps.CASING_PROPERTY) != null) return true;
        if (holder.getData(ThermalShockDataMaps.HEAT_SOURCE_PROPERTY) != null) return true;
        if (holder.getData(ThermalShockDataMaps.COLD_SOURCE_PROPERTY) != null) return true;

        // 3. 标签检查 (排气口、密封门)
        if (state.is(ThermalShockTags.VENTS) || state.is(ThermalShockTags.CASING_ACCESS)) return true;

        // 4. 特殊流体 (水作为冷源)
        if (state.getFluidState().is(FluidTags.WATER)) return true;

        return false;
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!event.getLevel().isClientSide() && event.getLevel() instanceof ServerLevel sl) {
            // 只有破坏了机器零件才检查，破坏普通方块(如内部原料)不影响
            if (isRelevantBlock(event.getState())) {
                StructureManager.checkActivity(sl, event.getPos(), false);
            }
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!event.getLevel().isClientSide() && event.getLevel() instanceof ServerLevel sl) {
            // 只有放置了机器零件才检查，放置普通方块视为内部填充
            if (isRelevantBlock(event.getState())) {
                StructureManager.checkActivity(sl, event.getPos(), false);
            }
        }
    }

    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        if (!event.getLevel().isClientSide() && event.getLevel() instanceof ServerLevel sl) {
            for (BlockPos pos : event.getAffectedBlocks()) {
                if (isRelevantBlock(sl.getBlockState(pos))) {
                    StructureManager.checkActivity(sl, pos, false);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPistonPre(PistonEvent.Pre event) {
        if (!event.getLevel().isClientSide() && event.getLevel() instanceof ServerLevel sl) {
            // 活塞本体
            StructureManager.checkActivity(sl, event.getPos(), false);

            if (event.getStructureHelper() != null) {
                // 智能过滤：只有当活塞推/拉的是“机器零件”时，才触发结构检查
                // 这样允许活塞向机器内部推入圆石等原料而不破坏结构
                event.getStructureHelper().getToDestroy().forEach(pos -> {
                    if (isRelevantBlock(sl.getBlockState(pos))) StructureManager.checkActivity(sl, pos, false);
                });
                event.getStructureHelper().getToPush().forEach(pos -> {
                    if (isRelevantBlock(sl.getBlockState(pos))) StructureManager.checkActivity(sl, pos, false);
                });
            }
        }
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (event.getEntity() instanceof ItemEntity itemEntity) {
            if (event.getLevel() instanceof ServerLevel sl) {
                StructureManager.checkActivity(sl, itemEntity.blockPosition(), true);
            }
        }
    }

    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (event.getEntity() instanceof ItemEntity itemEntity) {
            if (event.getLevel() instanceof ServerLevel sl) {
                StructureManager.checkActivity(sl, itemEntity.blockPosition(), true);
            }
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (!event.getLevel().isClientSide() && event.getLevel() instanceof Level level) {
            StructureManager.clearCache(level.dimension().location().toString());
        }
    }
}