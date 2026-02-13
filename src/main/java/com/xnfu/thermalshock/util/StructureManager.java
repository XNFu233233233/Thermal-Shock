package com.xnfu.thermalshock.util;

import com.xnfu.thermalshock.block.entity.SimulationChamberBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import com.xnfu.thermalshock.registries.*;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.level.ChunkPos;

public class StructureManager {
    public enum UpdateType {
        PLACE, BREAK, ITEM
    }
    // 维度 -> (控制器坐标 -> 结构包围盒)
    // 包围盒已预先 Inflate(1) 以包含外部热源
    private static final Map<String, Map<BlockPos, AABB>> STRUCTURE_REGISTRY = new ConcurrentHashMap<>();

    // 维度 -> (控制器坐标 -> 待成型扫描范围)
    // 用于未成型时监听更大范围的变动 (13x13x13)
    private static final Map<String, Map<BlockPos, AABB>> PENDING_REGISTRY = new ConcurrentHashMap<>();

    // [新增] 空间索引：维度 -> (分块坐标 -> 控制器集合)
    // 使 checkActivity 复杂度从 O(N) 降为 O(1) 或 O(当前分块机器数)
    private static final Map<String, Map<Long, Set<BlockPos>>> CHUNK_REGISTRY = new ConcurrentHashMap<>();

    // [新增] 反向索引：维度 -> (控制器 -> 分块集合)
    // 用于在结构移除时快速清理空间索引
    private static final Map<String, Map<BlockPos, Set<Long>>> REVERSE_LOOKUP = new ConcurrentHashMap<>();

    // [新增] 验证队列：防止大量机器同时验证导致瞬时卡顿 (TPS 稳定器)
    private static final Queue<BlockPos> VALIDATION_QUEUE = new LinkedList<>();
    private static final Set<BlockPos> PENDING_VALIDATIONS = new HashSet<>();

    public static void updateStructure(Level level, BlockPos controllerPos, AABB bounds) {
        if (level.isClientSide) return;
        String dim = level.dimension().location().toString();
        
        // 1. 更新包围盒
        AABB inflated = bounds.inflate(1.0);
        STRUCTURE_REGISTRY.computeIfAbsent(dim, k -> new ConcurrentHashMap<>())
                .put(controllerPos, inflated);
        
        // 2. 更新空间索引
        updateSpatialIndex(dim, controllerPos, inflated);

        // 3. 从未成型列表移除 (如果有)
        var pending = PENDING_REGISTRY.get(dim);
        if (pending != null) pending.remove(controllerPos);
    }

    public static void registerPending(Level level, BlockPos controllerPos, AABB searchBox) {
        if (level.isClientSide) return;
        String dim = level.dimension().location().toString();
        
        // 1. 注册到未成型列表
        PENDING_REGISTRY.computeIfAbsent(dim, k -> new ConcurrentHashMap<>())
                .put(controllerPos, searchBox);
        
        // 2. 更新空间索引
        updateSpatialIndex(dim, controllerPos, searchBox);

        // 3. 从成型列表移除
        var formed = STRUCTURE_REGISTRY.get(dim);
        if (formed != null) formed.remove(controllerPos);
    }

    public static void removeStructure(Level level, BlockPos controllerPos) {
        if (level == null || level.isClientSide) return;
        String dim = level.dimension().location().toString();
        
        var map = STRUCTURE_REGISTRY.get(dim);
        if (map != null) map.remove(controllerPos);
        
        var pending = PENDING_REGISTRY.get(dim);
        if (pending != null) pending.remove(controllerPos);

        // 清理索引
        removeFromSpatialIndex(dim, controllerPos);
    }

    public static void checkActivity(ServerLevel level, BlockPos targetPos, UpdateType type) {
        // [修复] 防御性检查：如果服务器正在停止，或者区块未加载，直接跳过。
        // 这防止了在服务器关闭过程中触发区块状态更新导致的 ConcurrentModificationException。
        if (!level.getServer().isRunning() || !level.isLoaded(targetPos)) return;
        
        checkActivity(level, targetPos, type, level.getBlockState(targetPos));
    }

    public static void checkActivity(ServerLevel level, BlockPos targetPos, UpdateType type, BlockState state) {
        String dim = level.dimension().location().toString();
        
        // 获取所在区块索引
        long chunkPosKey = ChunkPos.asLong(targetPos);
        var chunkMap = CHUNK_REGISTRY.get(dim);
        if (chunkMap == null) return;
        
        Set<BlockPos> candidates = chunkMap.get(chunkPosKey);
        if (candidates == null || candidates.isEmpty()) return;

        // 仅在机器相关方块变动或物品实体变动时才检查未成型列表
        boolean isMachine = isMachineRelated(state) || type == UpdateType.ITEM;

        // 仅检查当前区块内可能受影响的控制器 (O(1) ~ O(M))
        // [修复] 使用副本遍历，防止 notifyController 触发结构更新导致 candidates 集合并发修改
        for (BlockPos controllerPos : new HashSet<>(candidates)) {
            // 1. 检查已成型结构
            var formedMap = STRUCTURE_REGISTRY.get(dim);
            if (formedMap != null) {
                AABB bounds = formedMap.get(controllerPos);
                // 对于已成型结构，任何在其包围盒内的变动（即内部泥土或外壳破坏）都是相关的
                if (bounds != null && bounds.contains(targetPos.getX(), targetPos.getY(), targetPos.getZ())) {
                    notifyController(level, controllerPos, targetPos, type);
                    continue; 
                }
            }

            // 2. 检查未成型控制器
            // 只有当放置/破坏的是机器零件，或者是 clearing obstructions (BREAK dirt if we want to be aggressive, but user said NO)
            // 这里的优化：排除非机器块对未成型控制器的干扰
            if (isMachine) {
                var pendingMap = PENDING_REGISTRY.get(dim);
                if (pendingMap != null) {
                    AABB bounds = pendingMap.get(controllerPos);
                    if (bounds != null && bounds.contains(targetPos.getX(), targetPos.getY(), targetPos.getZ())) {
                        notifyController(level, controllerPos, targetPos, type);
                    }
                }
            }
        }
    }

    private static boolean isMachineRelated(BlockState state) {
        if (state == null || state.isAir()) return false;
        
        // 核心机器与组件
        if (state.is(ThermalShockBlocks.SIMULATION_CHAMBER_CONTROLLER.get()) ||
            state.is(ThermalShockBlocks.SIMULATION_CHAMBER_PORT.get()) ||
            state.is(ThermalShockBlocks.THERMAL_HEATER.get()) ||
            state.is(ThermalShockBlocks.THERMAL_FREEZER.get())) return true;
        
        // 数据映射关系块 (外壳、热源等)
        var holder = state.getBlockHolder();
        if (holder.getData(ThermalShockDataMaps.CASING_PROPERTY) != null) return true;
        if (holder.getData(ThermalShockDataMaps.HEAT_SOURCE_PROPERTY) != null) return true;
        
        // 标签检查
        if (state.is(ThermalShockTags.VENTS) || state.is(ThermalShockTags.CASING_ACCESS)) return true;
        
        return false;
    }

    private static void notifyController(ServerLevel level, BlockPos controllerPos, BlockPos targetPos, UpdateType type) {
        if (level.isLoaded(controllerPos)) {
            BlockEntity be = level.getBlockEntity(controllerPos);
            if (be instanceof SimulationChamberBlockEntity chamber) {
                chamber.onEnvironmentUpdate(targetPos, type);
            }
        }
    }

    /**
     * [新增] 提交验证请求到队列
     */
    public static void requestValidation(BlockPos pos) {
        if (!PENDING_VALIDATIONS.contains(pos)) {
            VALIDATION_QUEUE.add(pos);
            PENDING_VALIDATIONS.add(pos);
        }
    }

    /**
     * [新增] 每刻运行一次：处理队列中的验证请求
     * 限制每 tick 最多处理 5 个，防止突发流量导致卡顿
     */
    public static void tick(ServerLevel level) {
        int processed = 0;
        int maxPerTick = com.xnfu.thermalshock.Config.validationLimitPerTick;

        while (processed < maxPerTick && !VALIDATION_QUEUE.isEmpty()) {
            BlockPos pos = VALIDATION_QUEUE.poll();
            if (pos == null) break;
            PENDING_VALIDATIONS.remove(pos);

            if (level.isLoaded(pos)) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof SimulationChamberBlockEntity chamber) {
                    chamber.performValidation(null);
                }
            }
            processed++;
        }
    }

    public static void clearCache(String dimensionId) {
        STRUCTURE_REGISTRY.remove(dimensionId);
        PENDING_REGISTRY.remove(dimensionId);
        CHUNK_REGISTRY.remove(dimensionId);
        REVERSE_LOOKUP.remove(dimensionId);
    }

    // --- 内部索引维护 ---

    private static void updateSpatialIndex(String dim, BlockPos controllerPos, AABB bounds) {
        // 先清理旧索引
        removeFromSpatialIndex(dim, controllerPos);

        Set<Long> chunks = new HashSet<>();
        int minX = (int) Math.floor(bounds.minX) >> 4;
        int minZ = (int) Math.floor(bounds.minZ) >> 4;
        int maxX = (int) Math.floor(bounds.maxX) >> 4;
        int maxZ = (int) Math.floor(bounds.maxZ) >> 4;

        var dimChunkRegistry = CHUNK_REGISTRY.computeIfAbsent(dim, k -> new ConcurrentHashMap<>());

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                long key = ChunkPos.asLong(x, z);
                chunks.add(key);
                dimChunkRegistry.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet()).add(controllerPos);
            }
        }

        REVERSE_LOOKUP.computeIfAbsent(dim, k -> new ConcurrentHashMap<>()).put(controllerPos, chunks);
    }

    private static void removeFromSpatialIndex(String dim, BlockPos controllerPos) {
        var dimReverse = REVERSE_LOOKUP.get(dim);
        if (dimReverse == null) return;

        Set<Long> chunks = dimReverse.remove(controllerPos);
        if (chunks == null) return;

        var dimChunkRegistry = CHUNK_REGISTRY.get(dim);
        if (dimChunkRegistry == null) return;

        for (long chunkKey : chunks) {
            Set<BlockPos> controllers = dimChunkRegistry.get(chunkKey);
            if (controllers != null) {
                controllers.remove(controllerPos);
                if (controllers.isEmpty()) {
                    dimChunkRegistry.remove(chunkKey);
                }
            }
        }
    }
}