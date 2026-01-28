package com.xnfu.thermalshock.block.entity;

import com.xnfu.thermalshock.data.CasingData;
import com.xnfu.thermalshock.registries.ThermalShockDataMaps;
import com.xnfu.thermalshock.util.MultiblockValidator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ChamberStructure {
    private boolean isFormed = false;

    // 边界与体积
    private BlockPos minPos = BlockPos.ZERO;
    private BlockPos maxPos = BlockPos.ZERO;
    private int volume = 0;

    // 外壳属性
    private int minTemp = 0;
    private int maxTemp = 0;
    private float efficiency = 1.0f;

    // 产量乘区
    private int yieldMultiplier = 1;

    // 组件缓存
    private BlockState camouflageState = Blocks.AIR.defaultBlockState();
    private final Set<BlockPos> connectedPorts = new HashSet<>();
    private final List<BlockPos> vents = new ArrayList<>();

    // --- 逻辑方法 ---

    // 修改 update 方法，让它调用新的私有计算方法
    public void update(Level level, BlockPos controllerPos, BlockState controllerState, MultiblockValidator.ValidationResult result) {
        if (this.isFormed) {
            unbindPorts(level);
        }

        this.isFormed = result.isValid();

        if (this.isFormed) {
            this.minPos = result.minPos();
            this.maxPos = result.maxPos();

            this.connectedPorts.clear();
            this.connectedPorts.addAll(result.portPositions());
            this.vents.clear();
            this.vents.addAll(result.ventPositions());

            // 设置伪装状态
            BlockState newCamo = result.casingBlock().defaultBlockState();
            setCamouflageAndBind(level, newCamo, controllerPos);

            // [核心修复] 调用计算逻辑
            recalculateStats(newCamo.getBlock());
        } else {
            reset(level);
        }
    }

    /**
     * [新增] 根据当前的坐标和外壳方块，重新计算体积、效率和倍率。
     * 用于 update 时或 onLoad 恢复数据时。
     */
    public void recalculateStats(net.minecraft.world.level.block.Block casingBlock) {
        int sizeX = maxPos.getX() - minPos.getX() + 1;
        int sizeY = maxPos.getY() - minPos.getY() + 1;
        int sizeZ = maxPos.getZ() - minPos.getZ() + 1;

        this.volume = Math.max(0, (sizeX - 2) * (sizeY - 2) * (sizeZ - 2));
        this.yieldMultiplier = calculateTierMultiplier(Math.max(sizeX, Math.max(sizeY, sizeZ)));

        updateCasingStats(casingBlock);

        // 效率 = 材质效率 * 尺寸倍率
        this.efficiency = this.efficiency * this.yieldMultiplier;
    }

    private int calculateTierMultiplier(int size) {
        if (size % 2 == 0) size--;

        // 映射表: 3x3->1, 5x5->2, 7x7->4, 9->8, 11->16, 13->32
        // 公式: 2 ^ ((size - 3) / 2)
        return switch (size) {
            case 3 -> 1; // 基准 (1x)
            case 5 -> 2;
            case 7 -> 4;
            case 9 -> 8;
            case 11 -> 16;
            case 13 -> 32;
            default -> 1;
        };
    }

    private void updateCasingStats(net.minecraft.world.level.block.Block block) {
        var holder = BuiltInRegistries.BLOCK.wrapAsHolder(block);
        CasingData data = holder.getData(ThermalShockDataMaps.CASING_PROPERTY);
        if (data != null) {
            this.minTemp = data.minTemp();
            this.maxTemp = data.maxTemp();
            this.efficiency = data.efficiency();
        } else {
            this.minTemp = 0;
            this.maxTemp = 0;
            this.efficiency = 1.0f;
        }
    }

    public void reset(Level level) {
        if (this.isFormed) unbindPorts(level);
        this.isFormed = false;
        this.minPos = BlockPos.ZERO;
        this.maxPos = BlockPos.ZERO;
        this.minTemp = 0;
        this.maxTemp = 0;
        this.efficiency = 0.0f;
        this.yieldMultiplier = 1;
        this.volume = 0;
        this.connectedPorts.clear();
        this.vents.clear();
    }

    private void setCamouflageAndBind(Level level, BlockState state, BlockPos controllerPos) {
        this.camouflageState = state;
        for (BlockPos pos : connectedPorts) {
            if (level.getBlockEntity(pos) instanceof SimulationPortBlockEntity port) {
                port.setCamouflageState(state);
                port.bindController(controllerPos);
            }
        }
    }

    private void unbindPorts(Level level) {
        for (BlockPos pos : connectedPorts) {
            if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof SimulationPortBlockEntity port) {
                port.unbindController();
                port.setCamouflageState(net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            }
        }
    }

    /**
     * 判断坐标是否属于结构整体（包含外壳）
     */
    public boolean contains(BlockPos pos) {
        if (!isFormed) return false;
        return pos.getX() >= minPos.getX() && pos.getX() <= maxPos.getX() &&
                pos.getY() >= minPos.getY() && pos.getY() <= maxPos.getY() &&
                pos.getZ() >= minPos.getZ() && pos.getZ() <= maxPos.getZ();
    }

    /**
     * 判断坐标是否在机器内部（不含外壳）
     * 用于 StructureManager 判定是否仅仅是原料变动
     */
    public boolean isInterior(BlockPos pos) {
        if (!isFormed) return false;
        return pos.getX() > minPos.getX() && pos.getX() < maxPos.getX() &&
                pos.getY() > minPos.getY() && pos.getY() < maxPos.getY() &&
                pos.getZ() > minPos.getZ() && pos.getZ() < maxPos.getZ();
    }

    // --- NBT ---
    public void save(CompoundTag tag) {
        tag.putBoolean("IsFormed", isFormed);
        tag.put("MinPos", NbtUtils.writeBlockPos(minPos));
        tag.put("MaxPos", NbtUtils.writeBlockPos(maxPos));
        if (camouflageState != null) {
            tag.put("Camouflage", NbtUtils.writeBlockState(camouflageState));
        }

        // [核心修复] 保存端口列表 (转为 LongArray 以节省空间)
        if (!connectedPorts.isEmpty()) {
            long[] portsArray = connectedPorts.stream().mapToLong(BlockPos::asLong).toArray();
            tag.putLongArray("ConnectedPorts", portsArray);
        }

        // [建议] 同时保存排气口列表，确保进服后特效位置正确
        if (!vents.isEmpty()) {
            long[] ventsArray = vents.stream().mapToLong(BlockPos::asLong).toArray();
            tag.putLongArray("Vents", ventsArray);
        }
    }

    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        this.isFormed = tag.getBoolean("IsFormed");
        if (tag.contains("MinPos")) this.minPos = NbtUtils.readBlockPos(tag, "MinPos").orElse(BlockPos.ZERO);
        if (tag.contains("MaxPos")) this.maxPos = NbtUtils.readBlockPos(tag, "MaxPos").orElse(BlockPos.ZERO);

        if (tag.contains("Camouflage")) {
            this.camouflageState = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), tag.getCompound("Camouflage"));
        }

        // [核心修复] 恢复端口列表
        this.connectedPorts.clear();
        if (tag.contains("ConnectedPorts")) {
            long[] portsArray = tag.getLongArray("ConnectedPorts");
            for (long p : portsArray) {
                this.connectedPorts.add(BlockPos.of(p));
            }
        }

        // [建议] 恢复排气口列表
        this.vents.clear();
        if (tag.contains("Vents")) {
            long[] ventsArray = tag.getLongArray("Vents");
            for (long p : ventsArray) {
                this.vents.add(BlockPos.of(p));
            }
        }
    }

    // --- Getters ---
    public boolean isFormed() { return isFormed; }
    public BlockPos getMinPos() { return minPos; }
    public BlockPos getMaxPos() { return maxPos; }
    public int getVolume() { return volume; }
    public int getMinTemp() { return minTemp; }
    public int getMaxTemp() { return maxTemp; }
    public float getEfficiency() { return efficiency; }
    public BlockState getCamouflageState() { return camouflageState; }
    public Set<BlockPos> getPorts() { return connectedPorts; }
    public List<BlockPos> getVents() { return vents; }
    public int getYieldMultiplier() { return yieldMultiplier; }
    public void setCamouflageStateOnly(BlockState state) { this.camouflageState = state; }
}