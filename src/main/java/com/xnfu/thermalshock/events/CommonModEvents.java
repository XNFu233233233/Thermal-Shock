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
     * 优化后的过滤器：判断方块变动是否需要触发多方块检查
     */
    private static boolean isRelevantBlock(BlockState state) {
        if (state.isAir()) return false;

        Block block = state.getBlock();

        if (block == ThermalShockBlocks.SIMULATION_CHAMBER_CONTROLLER.get() ||
                block == ThermalShockBlocks.SIMULATION_CHAMBER_PORT.get() ||
                block == ThermalShockBlocks.THERMAL_HEATER.get() ||
                block == ThermalShockBlocks.THERMAL_FREEZER.get() ||
                block == ThermalShockBlocks.THERMAL_CONVERTER.get()) {
            return true;
        }

        // 2. DataMap 检查 (涵盖外壳、热源、冷源)
        var holder = state.getBlockHolder();
        if (holder.getData(ThermalShockDataMaps.CASING_PROPERTY) != null) return true;
        if (holder.getData(ThermalShockDataMaps.HEAT_SOURCE_PROPERTY) != null) return true;
        if (holder.getData(ThermalShockDataMaps.COLD_SOURCE_PROPERTY) != null) return true;

        // 3. 标签检查 (排气口、门/通道)
        if (state.is(ThermalShockTags.VENTS) || state.is(ThermalShockTags.CASING_ACCESS)) return true;

        // 4. 流体检查 (水作为冷源)
        if (state.getFluidState().is(FluidTags.WATER)) return true;

        return false;
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!event.getLevel().isClientSide() && event.getLevel() instanceof ServerLevel sl) {
            if (isRelevantBlock(event.getState())) {
                StructureManager.checkActivity(sl, event.getPos(), false);
            }
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!event.getLevel().isClientSide() && event.getLevel() instanceof ServerLevel sl) {
            if (isRelevantBlock(event.getState())) {
                StructureManager.checkActivity(sl, event.getPos(), false);
            }
        }
    }

    @SubscribeEvent
    public static void onNeighborNotify(BlockEvent.NeighborNotifyEvent event) {
        if (!event.getLevel().isClientSide() && event.getLevel() instanceof ServerLevel sl) {
            StructureManager.checkActivity(sl, event.getPos(), false);
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
            StructureManager.checkActivity(sl, event.getPos(), false);
            if (event.getStructureHelper() != null) {
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