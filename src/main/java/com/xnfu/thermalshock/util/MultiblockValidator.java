package com.xnfu.thermalshock.util;

import com.xnfu.thermalshock.registries.ThermalShockBlocks;
import com.xnfu.thermalshock.registries.ThermalShockDataMaps;
import com.xnfu.thermalshock.registries.ThermalShockTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MultiblockValidator {

    public record ValidationResult(
            boolean isValid,
            BlockPos minPos,
            BlockPos maxPos,
            Block casingBlock,
            int ventCount,
            Component errorMessage,
            BlockPos errorPos,
            List<BlockPos> portPositions,
            List<BlockPos> ventPositions
    ) {}

    // 置信度阈值：骨架完整度超过 80% 才认为是有效尝试
    private static final float CONFIDENCE_THRESHOLD = 0.80f;

    public static ValidationResult validate(Level level, BlockPos controllerPos, Direction controllerFacing) {
        Direction backDir = controllerFacing.getOpposite();
        Direction upDir = Direction.UP;
        Direction rightDir = controllerFacing.getCounterClockWise();

        int bestSize = 0;
        BlockPos bestTopLeft = null;
        float bestScore = -1.0f;

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        // 1. 骨架扫描 (Wireframe Scoring)
        // 倒序遍历尺寸 (13 -> 3)
        for (int size = 13; size >= 3; size--) {
            // 只遍历控制器可能存在的棱位
            for (int u = 0; u < size; u++) {
                int[] vCandidates;
                if (u == 0 || u == size - 1) {
                    vCandidates = new int[size];
                    for(int k=0; k<size; k++) vCandidates[k] = k;
                } else {
                    vCandidates = new int[]{0, size - 1};
                }

                for (int v : vCandidates) {
                    cursor.set(controllerPos)
                            .move(rightDir, -u)
                            .move(upDir, v);

                    BlockPos topLeft = cursor.immutable();

                    float score = calculateFrameConfidence(level, topLeft, size, rightDir, Direction.DOWN, backDir);

                    if (score > bestScore) {
                        bestScore = score;
                        bestSize = size;
                        bestTopLeft = topLeft;
                    }

                    if (score >= 0.99f) break;
                }
                if (bestScore >= 0.99f) break;
            }
            if (bestScore >= 0.99f) break;
        }

        // 2. 决策
        if (bestScore < CONFIDENCE_THRESHOLD || bestTopLeft == null) {
            return fail(Component.translatable("message.thermalshock.incomplete"), null);
        }

        return performStrictScan(level, bestTopLeft, bestSize, rightDir, Direction.DOWN, backDir, controllerPos);
    }

    private static float calculateFrameConfidence(Level level, BlockPos topLeft, int size, Direction right, Direction down, Direction back) {
        int validBlocks = 0;
        int d = size - 1;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();

        validBlocks += scanLine(level, topLeft, back, size);
        validBlocks += scanLine(level, p.set(topLeft).move(right, d), back, size);
        validBlocks += scanLine(level, p.set(topLeft).move(down, d), back, size);
        validBlocks += scanLine(level, p.set(topLeft).move(right, d).move(down, d), back, size);

        int innerLen = Math.max(0, size - 2);
        if (innerLen > 0) {
            validBlocks += scanLine(level, p.set(topLeft).move(right, 1), right, innerLen);
            validBlocks += scanLine(level, p.set(topLeft).move(right, 1).move(back, d), right, innerLen);
            validBlocks += scanLine(level, p.set(topLeft).move(right, 1).move(down, d), right, innerLen);
            validBlocks += scanLine(level, p.set(topLeft).move(right, 1).move(down, d).move(back, d), right, innerLen);

            validBlocks += scanLine(level, p.set(topLeft).move(down, 1), down, innerLen);
            validBlocks += scanLine(level, p.set(topLeft).move(down, 1).move(right, d), down, innerLen);
            validBlocks += scanLine(level, p.set(topLeft).move(down, 1).move(back, d), down, innerLen);
            validBlocks += scanLine(level, p.set(topLeft).move(down, 1).move(right, d).move(back, d), down, innerLen);
        }

        int totalExpected = 4 * size + 8 * innerLen;
        return (float) validBlocks / totalExpected;
    }

    private static int scanLine(Level level, BlockPos start, Direction dir, int length) {
        int count = 0;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos().set(start);
        for (int i = 0; i < length; i++) {
            if (isFrameCandidate(level.getBlockState(p))) {
                count++;
            }
            p.move(dir);
        }
        return count;
    }

    private static boolean isFrameCandidate(BlockState state) {
        if (state.isAir()) return false;
        return state.getBlockHolder().getData(ThermalShockDataMaps.CASING_PROPERTY) != null
                || state.is(ThermalShockBlocks.SIMULATION_CHAMBER_CONTROLLER.get())
                || state.is(ThermalShockBlocks.SIMULATION_CHAMBER_PORT.get());
    }

    private static ValidationResult performStrictScan(Level level, BlockPos topLeft, int size, Direction right, Direction down, Direction back, BlockPos controllerPos) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        Block validCasingBlock = null;
        int ventCount = 0;
        int access = 0;
        List<BlockPos> ports = new ArrayList<>();
        List<BlockPos> vents = new ArrayList<>();

        BlockPos maxPos = topLeft.relative(right, size - 1).relative(down, size - 1).relative(back, size - 1);
        BlockPos minResult = new BlockPos(
                Math.min(topLeft.getX(), maxPos.getX()),
                Math.min(topLeft.getY(), maxPos.getY()),
                Math.min(topLeft.getZ(), maxPos.getZ())
        );
        BlockPos maxResult = new BlockPos(
                Math.max(topLeft.getX(), maxPos.getX()),
                Math.max(topLeft.getY(), maxPos.getY()),
                Math.max(topLeft.getZ(), maxPos.getZ())
        );

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                for (int k = 0; k < size; k++) {
                    cursor.set(topLeft).move(right, i).move(down, j).move(back, k);

                    // 忽略控制器自身位置
                    if (cursor.equals(controllerPos)) continue;

                    BlockState state = level.getBlockState(cursor);

                    // ====================================================
                    // 1. 全局唯一性检查 (Global Uniqueness)
                    // ====================================================
                    // 无论是在外壳还是内部，都不允许出现第二个控制器，避免多方块逻辑冲突
                    if (state.is(ThermalShockBlocks.SIMULATION_CHAMBER_CONTROLLER.get())) {
                        return fail(Component.translatable("message.thermalshock.multiple_controllers"), cursor.immutable());
                    }

                    // ====================================================
                    // 2. 内部检查 (Interior) - 宽松模式
                    // ====================================================
                    boolean isX = (i == 0 || i == size - 1);
                    boolean isY = (j == 0 || j == size - 1);
                    boolean isZ = (k == 0 || k == size - 1);
                    int edgeCount = (isX ? 1 : 0) + (isY ? 1 : 0) + (isZ ? 1 : 0);

                    // edgeCount == 0 代表内部空间
                    if (edgeCount == 0) {
                        // [核心修改] 只要不是控制器（上面已检查），允许任何方块存在
                        // 这样配方所需的输入方块（如圆石）或杂物都不会破坏结构完整性
                        continue;
                    }

                    // ====================================================
                    // 3. 外部检查 (Shell) - 严格模式
                    // ====================================================
                    BlockCheckResult check = checkBlockType(state);

                    // Frame (棱)
                    if (edgeCount >= 2) {
                        if (!check.isFrameValid) {
                            return fail(Component.translatable("message.thermalshock.incomplete"), cursor.immutable());
                        }
                    }
                    // Face (面)
                    else {
                        if (!check.isFaceValid) {
                            return fail(Component.translatable("message.thermalshock.incomplete"), cursor.immutable());
                        }
                    }

                    // 统计组件
                    if (check.isVent) {
                        ventCount++;
                        vents.add(cursor.immutable());
                    }
                    if (check.isAccess) access++;
                    if (state.is(ThermalShockBlocks.SIMULATION_CHAMBER_PORT.get())) ports.add(cursor.immutable());

                    // 检查外壳材质一致性
                    if (check.isCasing) {
                        if (validCasingBlock == null) {
                            validCasingBlock = state.getBlock();
                        } else if (state.getBlock() != validCasingBlock) {
                            return fail(Component.translatable("message.thermalshock.inconsistent_outer_shell"), cursor.immutable());
                        }
                    }
                }
            }
        }

        if (validCasingBlock == null) return fail(Component.translatable("message.thermalshock.incomplete"), controllerPos);
        if (ventCount < 1) return fail(Component.translatable("message.thermalshock.missing_vent"), controllerPos);
        if (ventCount > 9) return fail(Component.translatable("message.thermalshock.too_many_vents"), controllerPos);
        if (access > 4) return fail(Component.translatable("message.thermalshock.too_many_access"), controllerPos);
        if (ports.size() > 16) return fail(Component.translatable("message.thermalshock.too_many_port"), controllerPos);

        return new ValidationResult(true, minResult, maxResult, validCasingBlock, ventCount, Component.translatable("message.thermalshock.complete"), null, ports, vents);
    }

    private static ValidationResult fail(Component msg, BlockPos errorPos) {
        return new ValidationResult(false, BlockPos.ZERO, BlockPos.ZERO, null, 0, msg, errorPos, Collections.emptyList(), Collections.emptyList());
    }

    private record BlockCheckResult(boolean isFrameValid, boolean isFaceValid, boolean isCasing, boolean isVent, boolean isAccess) {}

    private static BlockCheckResult checkBlockType(BlockState state) {
        boolean isController = state.is(ThermalShockBlocks.SIMULATION_CHAMBER_CONTROLLER.get());
        boolean isPort = state.is(ThermalShockBlocks.SIMULATION_CHAMBER_PORT.get());
        boolean isCasing = state.getBlockHolder().getData(ThermalShockDataMaps.CASING_PROPERTY) != null;
        boolean isVent = state.is(ThermalShockTags.VENTS);
        boolean isAccess = state.is(ThermalShockTags.CASING_ACCESS);

        boolean isFrameValid = isController || isPort || isCasing;
        boolean isFaceValid = isFrameValid || isVent || isAccess;

        return new BlockCheckResult(isFrameValid, isFaceValid, isCasing, isVent, isAccess);
    }
}