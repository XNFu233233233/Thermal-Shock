package com.xnfu.thermalshock.util;

import com.xnfu.thermalshock.block.entity.SimulationChamberBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class StructureManager {
    // 维度 -> (Chunk -> 控制器列表)
    private static final Map<String, Map<ChunkPos, Set<BlockPos>>> ALL_CONTROLLERS = new ConcurrentHashMap<>();

    public static void trackController(Level level, BlockPos pos) {
        if (level.isClientSide) return;
        String dim = level.dimension().location().toString();
        ChunkPos chunk = new ChunkPos(pos);
        ALL_CONTROLLERS.computeIfAbsent(dim, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(chunk, k -> new HashSet<>())
                .add(pos);
    }

    public static void untrackController(Level level, BlockPos pos) {
        if (level == null || level.isClientSide) return;
        String dim = level.dimension().location().toString();
        ChunkPos chunk = new ChunkPos(pos);
        var chunkMap = ALL_CONTROLLERS.get(dim);
        if (chunkMap != null) {
            var set = chunkMap.get(chunk);
            if (set != null) {
                set.remove(pos);
                if (set.isEmpty()) chunkMap.remove(chunk);
            }
        }
    }

    /**
     * 检查方块变动是否影响周围的控制器
     * 范围扩大 1 格以包含热源/冷源变动
     */
    public static void checkActivity(ServerLevel level, BlockPos targetPos, boolean isItemUpdate) {
        String dim = level.dimension().location().toString();
        var chunkMap = ALL_CONTROLLERS.get(dim);

        if (chunkMap == null || chunkMap.isEmpty()) return;

        ChunkPos centerChunk = new ChunkPos(targetPos);

        // 遍历 3x3 Chunk 寻找控制器
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                ChunkPos checkChunk = new ChunkPos(centerChunk.x + x, centerChunk.z + z);
                Set<BlockPos> controllers = chunkMap.get(checkChunk);

                if (controllers != null) {
                    for (BlockPos controllerPos : controllers) {
                        // [优化] 第一步：纯数学距离检查 (AABB check)
                        // 假设最大结构半径为 6 (5x5x5 + 1格缓冲)，如果距离太远直接跳过
                        // 这避免了不必要的 isLoaded 和 getBlockEntity 调用
                        if (!controllerPos.closerThan(targetPos, 16.0)) continue;

                        // 第二步：检查 Chunk 是否加载
                        if (level.isLoaded(controllerPos)) {
                            BlockEntity be = level.getBlockEntity(controllerPos);
                            if (be instanceof SimulationChamberBlockEntity chamber) {

                                // 第三步：精确范围检查
                                if (chamber.isFormed()) {
                                    BlockPos min = chamber.getMinPos();
                                    BlockPos max = chamber.getMaxPos();

                                    // 检测范围扩大 1 格以包含热源/冷源变动
                                    if (targetPos.getX() >= min.getX() - 1 && targetPos.getX() <= max.getX() + 1 &&
                                            targetPos.getY() >= min.getY() - 1 && targetPos.getY() <= max.getY() + 1 &&
                                            targetPos.getZ() >= min.getZ() - 1 && targetPos.getZ() <= max.getZ() + 1) {

                                        chamber.onEnvironmentUpdate(targetPos, isItemUpdate);
                                    }
                                } else {
                                    // 未成形时，只关心附近的变动 (尝试重新成形)
                                    if (controllerPos.distSqr(targetPos) < 64.0) { // 8格内
                                        chamber.onEnvironmentUpdate(targetPos, isItemUpdate);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public static void clearCache(String dimensionId) {
        ALL_CONTROLLERS.remove(dimensionId);
    }
}