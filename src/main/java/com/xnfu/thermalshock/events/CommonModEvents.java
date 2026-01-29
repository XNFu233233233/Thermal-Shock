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
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.level.PistonEvent;

@EventBusSubscriber(modid = ThermalShock.MODID)
public class CommonModEvents {

    /**
     * 快速过滤器：判断方块是否值得触发多方块检查
     * 只有 外壳、热源、流体、接口 才是“相关”的
     */
    private static boolean isRelevantBlock(BlockState state) {
        if (state.isAir()) return false;
        Block block = state.getBlock();
        var holder = state.getBlockHolder();

        // 1. 模组方块
        if (block == ThermalShockBlocks.SIMULATION_CHAMBER_CONTROLLER.get() ||
                block == ThermalShockBlocks.SIMULATION_CHAMBER_PORT.get() ||
                block == ThermalShockBlocks.THERMAL_HEATER.get() ||
                block == ThermalShockBlocks.THERMAL_FREEZER.get()) {
            return true;
        }

        // 2. DataMap 数据 (外壳/热源/冷源)
        if (holder.getData(ThermalShockDataMaps.CASING_PROPERTY) != null) return true;
        if (holder.getData(ThermalShockDataMaps.HEAT_SOURCE_PROPERTY) != null) return true;
        if (holder.getData(ThermalShockDataMaps.COLD_SOURCE_PROPERTY) != null) return true;

        // 3. 标签 (通风口/门)
        if (state.is(ThermalShockTags.VENTS) || state.is(ThermalShockTags.CASING_ACCESS)) return true;

        // 4. 流体 (水是冷源)
        if (state.getFluidState().is(FluidTags.WATER)) return true;

        return false;
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!event.getLevel().isClientSide() && event.getLevel() instanceof ServerLevel sl) {
            // 破坏前检查 state
            if (isRelevantBlock(event.getState())) {
                StructureManager.checkActivity(sl, event.getPos(), false);
            }
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!event.getLevel().isClientSide() && event.getLevel() instanceof ServerLevel sl) {
            // 放置后检查 placedBlock
            if (isRelevantBlock(event.getState())) {
                StructureManager.checkActivity(sl, event.getPos(), false);
            }
        }
    }

    @SubscribeEvent
    public static void onNeighborNotify(BlockEvent.NeighborNotifyEvent event) {
        if (!event.getLevel().isClientSide() && event.getLevel() instanceof ServerLevel sl) {
            // 邻居更新比较敏感，通常涉及红石或流体流动
            // 这里不过滤太死，否则可能漏掉红石信号
            StructureManager.checkActivity(sl, event.getPos(), false);
        }
    }

    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        if (!event.getLevel().isClientSide() && event.getLevel() instanceof ServerLevel sl) {
            for (BlockPos pos : event.getAffectedBlocks()) {
                // 爆炸波及的方块，如果是相关的，触发检查
                if (isRelevantBlock(sl.getBlockState(pos))) {
                    StructureManager.checkActivity(sl, pos, false);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPistonPre(PistonEvent.Pre event) {
        if (!event.getLevel().isClientSide() && event.getLevel() instanceof ServerLevel sl) {
            StructureManager.checkActivity(sl, event.getPos(), false);
            // 活塞推拉涉及的方块如果包含相关方块，也触发
            if (event.getStructureHelper() != null) {
                // 这里为了保险起见，全检查，活塞事件频率不高
                event.getStructureHelper().getToDestroy().forEach(pos -> StructureManager.checkActivity(sl, pos, false));
                event.getStructureHelper().getToPush().forEach(pos -> StructureManager.checkActivity(sl, pos, false));
            }
        }
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (event.getEntity() instanceof ItemEntity itemEntity) {
            if (event.getLevel() instanceof ServerLevel sl) {
                // 物品进入世界，可能是玩家丢弃或炸出的
                // 标记为 itemUpdate=true，只唤醒内部扫描，不触发结构验证
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