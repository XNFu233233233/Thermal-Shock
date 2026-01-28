package com.xnfu.thermalshock.events;

import com.xnfu.thermalshock.ThermalShock;
import com.xnfu.thermalshock.util.StructureManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.level.PistonEvent;

@EventBusSubscriber(modid = ThermalShock.MODID)
public class CommonModEvents {

    // 1. 常规方块变动 (玩家挖掘、放置)
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!event.getLevel().isClientSide() && event.getLevel() instanceof ServerLevel sl) {
            StructureManager.checkActivity(sl, event.getPos(), false);
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!event.getLevel().isClientSide() && event.getLevel() instanceof ServerLevel sl) {
            StructureManager.checkActivity(sl, event.getPos(), false);
        }
    }

    // 2. 邻居更新 (流体、红石) - 核心
    @SubscribeEvent
    public static void onNeighborNotify(BlockEvent.NeighborNotifyEvent event) {
        if (!event.getLevel().isClientSide() && event.getLevel() instanceof ServerLevel sl) {
            StructureManager.checkActivity(sl, event.getPos(), false);
        }
    }

    // 3. [新增] 爆炸破坏结构
    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        if (!event.getLevel().isClientSide() && event.getLevel() instanceof ServerLevel sl) {
            // 遍历所有受影响的方块位置
            event.getAffectedBlocks().forEach(pos ->
                    StructureManager.checkActivity(sl, pos, false)
            );
        }
    }

    // 4. [新增] 活塞推拉干扰
    @SubscribeEvent
    public static void onPistonPre(PistonEvent.Pre event) {
        if (!event.getLevel().isClientSide() && event.getLevel() instanceof ServerLevel sl) {
            // 活塞臂位置和被推动的方块位置都需要检查
            StructureManager.checkActivity(sl, event.getPos(), false);
            if (event.getStructureHelper() != null) {
                event.getStructureHelper().getToDestroy().forEach(pos -> StructureManager.checkActivity(sl, pos, false));
                event.getStructureHelper().getToPush().forEach(pos -> StructureManager.checkActivity(sl, pos, false));
            }
        }
    }

    // 5. 实体进入 (物品被丢入)
    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (event.getEntity() instanceof ItemEntity itemEntity) {
            if (event.getLevel() instanceof ServerLevel sl) {
                // [优化] 只有当物品实际上已经存活一小段时间后才检测？
                // 或者更激进：直接调用，因为 StructureManager 内部有范围判断，开销尚可。
                // 这里的 true 参数非常重要，它告诉 Controller "只是物品变了"，从而避免触发 full validation
                StructureManager.checkActivity(sl, itemEntity.blockPosition(), true);
            }
        }
    }

    // 6. [新增] 关服/卸载时清理缓存，防止内存泄漏
    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (!event.getLevel().isClientSide() && event.getLevel() instanceof Level level) {
            StructureManager.clearCache(level.dimension().location().toString());
        }
    }
}