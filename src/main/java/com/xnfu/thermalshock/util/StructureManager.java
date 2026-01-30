package com.xnfu.thermalshock.util;

import com.xnfu.thermalshock.block.entity.SimulationChamberBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StructureManager {
    // 维度 -> (控制器坐标 -> 结构包围盒)
    // 包围盒已预先 Inflate(1) 以包含外部热源
    private static final Map<String, Map<BlockPos, AABB>> STRUCTURE_REGISTRY = new ConcurrentHashMap<>();

    public static void updateStructure(Level level, BlockPos controllerPos, AABB bounds) {
        if (level.isClientSide) return;
        String dim = level.dimension().location().toString();
        STRUCTURE_REGISTRY.computeIfAbsent(dim, k -> new ConcurrentHashMap<>())
                .put(controllerPos, bounds.inflate(1.0));
    }

    public static void removeStructure(Level level, BlockPos controllerPos) {
        if (level == null || level.isClientSide) return;
        String dim = level.dimension().location().toString();
        var map = STRUCTURE_REGISTRY.get(dim);
        if (map != null) map.remove(controllerPos);
    }

    public static void checkActivity(ServerLevel level, BlockPos targetPos, boolean isItemUpdate) {
        String dim = level.dimension().location().toString();
        var map = STRUCTURE_REGISTRY.get(dim);
        if (map == null || map.isEmpty()) return;

        // O(N) 遍历，但 N 通常很小 (服务器内加载的结构数量)
        // 相比 Chunk 遍历 + TileEntity 获取 + 距离计算，纯内存 AABB 检测极快
        map.forEach((controllerPos, bounds) -> {
            if (bounds.contains(targetPos.getX(), targetPos.getY(), targetPos.getZ())) {
                if (level.isLoaded(controllerPos)) {
                    BlockEntity be = level.getBlockEntity(controllerPos);
                    if (be instanceof SimulationChamberBlockEntity chamber) {
                        chamber.onEnvironmentUpdate(targetPos, isItemUpdate);
                    }
                }
            }
        });
    }

    public static void clearCache(String dimensionId) {
        STRUCTURE_REGISTRY.remove(dimensionId);
    }
}