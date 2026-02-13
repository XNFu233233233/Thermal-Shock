package com.xnfu.thermalshock.events;

import com.xnfu.thermalshock.ThermalShock;
import com.xnfu.thermalshock.util.StructureManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.level.PistonEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.minecraft.world.item.BucketItem;

@EventBusSubscriber(modid = ThermalShock.MODID)
public class CommonModEvents {

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!event.getLevel().isClientSide() && event.getLevel() instanceof ServerLevel sl) {
            // 传入被破坏的方块状态进行智能过滤
            StructureManager.checkActivity(sl, event.getPos(), StructureManager.UpdateType.BREAK, event.getState());
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!event.getLevel().isClientSide() && event.getLevel() instanceof ServerLevel sl) {
            // 传入新放置的方块状态进行智能过滤
            StructureManager.checkActivity(sl, event.getPos(), StructureManager.UpdateType.PLACE, event.getPlacedBlock());
        }
    }

    /**
     * [新增] 专门监听物品右键事件 (处理桶倒水)
     */
    @SubscribeEvent
    public static void onItemUse(PlayerInteractEvent.RightClickItem event) {
        if (!event.getLevel().isClientSide && event.getItemStack().getItem() instanceof BucketItem) {
            // 玩家倒桶时，我们尝试获取其视线指向的位置
            if (event.getLevel() instanceof ServerLevel sl) {
                // 使用 entity.pick 获取视线碰撞
                var hit = event.getEntity().pick(5.0D, 0.0F, true);
                if (hit instanceof net.minecraft.world.phys.BlockHitResult blockHit) {
                    BlockPos targetPos = blockHit.getBlockPos().relative(blockHit.getDirection());
                    StructureManager.checkActivity(sl, targetPos, StructureManager.UpdateType.PLACE);
                }
            }
        }
    }

    /**
     * [新增] 更加通用的流体变动监听：当方块状态由于流体而改变时 (如流体蔓延、黑曜石生成等)
     */
    @SubscribeEvent
    public static void onFluidPlace(BlockEvent.FluidPlaceBlockEvent event) {
        if (!event.getLevel().isClientSide() && event.getLevel() instanceof ServerLevel sl) {
            StructureManager.checkActivity(sl, event.getPos(), StructureManager.UpdateType.PLACE, event.getNewState());
        }
    }

    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        if (!event.getLevel().isClientSide() && event.getLevel() instanceof ServerLevel sl) {
            for (BlockPos pos : event.getAffectedBlocks()) {
                // 爆炸通常伴随核心方块变动，此处暂时保留默认检查或获取快照
                StructureManager.checkActivity(sl, pos, StructureManager.UpdateType.BREAK, sl.getBlockState(pos));
            }
        }
    }

    @SubscribeEvent
    public static void onPistonPre(PistonEvent.Pre event) {
        if (!event.getLevel().isClientSide() && event.getLevel() instanceof ServerLevel sl) {
            // 活塞本体
            StructureManager.checkActivity(sl, event.getPos(), StructureManager.UpdateType.PLACE);

            if (event.getStructureHelper() != null) {
                // 活塞推拉视为 BREAK (原位置) 和 PLACE (新位置)
                // 简化处理：任何变动都触发一圈
                event.getStructureHelper().getToDestroy().forEach(pos -> {
                    StructureManager.checkActivity(sl, pos, StructureManager.UpdateType.BREAK);
                });
                event.getStructureHelper().getToPush().forEach(pos -> {
                    StructureManager.checkActivity(sl, pos, StructureManager.UpdateType.PLACE);
                });
            }
        }
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (event.getEntity() instanceof ItemEntity itemEntity) {
            if (event.getLevel() instanceof ServerLevel sl && sl.getServer().isRunning()) {
                StructureManager.checkActivity(sl, itemEntity.blockPosition(), StructureManager.UpdateType.ITEM);
            }
        }
    }

    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (event.getEntity() instanceof ItemEntity itemEntity) {
            if (event.getLevel() instanceof ServerLevel sl && sl.getServer().isRunning()) {
                StructureManager.checkActivity(sl, itemEntity.blockPosition(), StructureManager.UpdateType.ITEM);
            }
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (!event.getLevel().isClientSide() && event.getLevel() instanceof Level level) {
            StructureManager.clearCache(level.dimension().location().toString());
        }
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel sl) {
            StructureManager.tick(sl);
        }
    }
}