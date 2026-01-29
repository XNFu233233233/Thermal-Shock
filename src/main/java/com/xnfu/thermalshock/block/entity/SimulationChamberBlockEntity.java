package com.xnfu.thermalshock.block.entity;

import com.xnfu.thermalshock.ThermalShock;
import com.xnfu.thermalshock.block.SimulationChamberBlock;
import com.xnfu.thermalshock.client.gui.SimulationChamberMenu;
import com.xnfu.thermalshock.item.SimulationUpgradeItem;
import com.xnfu.thermalshock.registries.ThermalShockBlockEntities;
import com.xnfu.thermalshock.registries.ThermalShockBlocks;
import com.xnfu.thermalshock.registries.ThermalShockDataMaps;
import com.xnfu.thermalshock.util.MultiblockValidator;
import com.xnfu.thermalshock.util.StructureManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class SimulationChamberBlockEntity extends BlockEntity implements MenuProvider {

    // === 核心组件 ===
    private final ChamberStructure structure = new ChamberStructure();
    private final ChamberThermodynamics thermo = new ChamberThermodynamics();
    public final ChamberPerformance performance = new ChamberPerformance();
    private final ChamberProcess process = new ChamberProcess(this);

    // === 库存管理 ===
    // Slot 0: 催化剂 | Slot 1: 模拟升级
    private final ItemStackHandler itemHandler = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            wakeUp(); // [新增] 库存变动唤醒机器
            if (slot == 0) {
                markCatalystDirty();
            } else if (slot == 1) {
                // 升级卡变动，立即更新性能参数
                updatePerformance();
                // 升级卡可能改变虚拟化状态，导致端口逻辑变化
                portsDirty = true;
                // 升级卡变动可能解锁配方限制，唤醒处理逻辑
                markEntityCacheDirty();
            }
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == 0) {
                return BuiltInRegistries.ITEM.wrapAsHolder(stack.getItem())
                        .getData(ThermalShockDataMaps.CATALYST_PROPERTY) != null;
            } else if (slot == 1) {
                return stack.getItem() instanceof SimulationUpgradeItem;
            }
            return super.isItemValid(slot, stack);
        }

        @Override
        public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
            super.deserializeNBT(provider, nbt);
            // 加载完成后手动触发一次检查，确保升级卡效果生效
            if (level != null && !level.isClientSide) {
                updatePerformance();
            }
        }
    };

    // === 状态机与配置 ===
    private MachineMode mode = MachineMode.OVERHEATING;
    private ResourceLocation selectedRecipeId = null;
    private boolean recipeLocked = false;

    // [重构] 休眠与唤醒标记
    private boolean isSleeping = true;
    private boolean pendingProcess = false;

    // [关键修复] 验证逻辑懒加载
    private boolean validationPending = false;

    // === 缓存与脏标记 (Granular Dirty Flags) ===
    private BlockPos errorPos = null;

    // 逻辑执行相关
    private boolean blockCacheDirty = true;  // 内部方块变动
    private boolean entityCacheDirty = true; // 内部实体/接口库存变动

    // 辅助缓存
    private boolean portsDirty = true;
    private boolean catalystDirty = true;

    // === 红石状态 (核心修复) ===
    private boolean isPowered = false;    // 持续信号
    private boolean lastPowered = false;  // 上一刻信号
    private boolean isPulseFrame = false; // 脉冲触发帧

    // === 视觉 ===
    private int litTimer = 0;

    // === 接口缓存列表 ===
    private final List<BlockPos> cachedInputPorts = new ArrayList<>();
    private final List<BlockPos> cachedOutputPorts = new ArrayList<>();
    private final List<BlockPos> cachedCatalystPorts = new ArrayList<>();

    // === 数值累积 ===
    private float catalystBuffer = 0.0f;
    private float currentBonusYield = 0.0f;
    private float accumulatedYield = 0.0f;

    // === GUI 数据同步 ===
    // === GUI 数据同步 ===
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            if (level != null && !level.isClientSide) {
                thermo.updateLazy(level);
            }

            return switch (index) {
                case 0 -> mode == MachineMode.OVERHEATING ? thermo.getHeatStoredRaw() : thermo.getCurrentDelta();
                case 1 -> thermo.getMaxHeatCapacity();
                case 2 -> structure.getMaxColdRate();
                case 3 -> structure.getMaxHeatRate();
                case 4 -> (int) (currentBonusYield * 100);
                case 5 -> (int) (performance.getYieldMultiplier() * 100);
                case 6 -> (int) (catalystBuffer * 10);
                case 7 -> mode == MachineMode.OVERHEATING ?
                        thermo.getLastInputRate() :
                        thermo.getCurrentHighTemp();
                case 8 -> structure.getVolume();
                case 9 -> structure.isFormed() ? 1 : 0;
                case 10 -> recipeLocked ? 1 : 0;
                case 11 -> performance.getBatchSize();
                case 12 -> (int) (accumulatedYield * 100);
                case 13 -> mode.ordinal();
                case 14 -> thermo.getCurrentLowTemp();
                case 15 -> (int) (performance.getEfficiency() * 100);
                case 16 -> performance.isVirtual() ? performance.getBatchSize() : structure.getVolume();

                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 13) {
                mode = MachineMode.values()[value % MachineMode.values().length];
            }
        }

        @Override
        public int getCount() { return 17; }
    };

    public SimulationChamberBlockEntity(BlockPos pos, BlockState blockState) {
        super(ThermalShockBlockEntities.CHAMBER_CONTROLLER_BE.get(), pos, blockState);
    }

    // =========================================================
    // 1. 核心循环 (Tick Logic) - 极致优化版
    // =========================================================


    // [新增] 唤醒机制：当有外部输入或变动时调用
    public void wakeUp() {
        if (this.isSleeping) {
            this.isSleeping = false;
            this.pendingProcess = true; // 标记需要执行一次处理
            setChanged(); // 确保状态变化被保存
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, SimulationChamberBlockEntity be) {
        if (level.isClientSide) return;

        // 1. 安全性优先：处理延迟的结构验证
        if (be.validationPending) {
            be.performValidation(null);
            be.validationPending = false;
        }

        // 2. 红石状态机
        boolean currentSignal = level.hasNeighborSignal(pos);
        if (currentSignal != be.lastPowered) {
            be.isPulseFrame = currentSignal && !be.lastPowered;
            be.isPowered = currentSignal;
            be.updatePoweredState(currentSignal); // 唤醒
        } else {
            be.isPulseFrame = false;
        }

        // 3. 休眠检查
        // 条件：处于休眠态 && 无待处理任务 && 非发光状态 && 非脉冲帧
        if (be.isSleeping && !be.pendingProcess && be.litTimer <= 0 && !be.isPulseFrame) {
            return;
        }

        // 4. 业务逻辑执行
        if (be.structure.isFormed()) {
            // 基础更新 (无论是否有电都要算热量和催化剂)
            be.thermo.updateLazy(level);

            if (be.catalystDirty) {
                be.processCatalyst();
            }

            if (be.portsDirty) {
                be.refreshPortCache();
            }

            // [修复] 红石门控 (Redstone Gate)
            // 只有在 持续通电 或 收到脉冲 时才执行配方处理
            if (be.isPowered || be.isPulseFrame) {
                be.process.tick(level);
            }
        }

        // 5. 状态收尾
        if (be.litTimer > 0) {
            be.litTimer--;
            if (be.litTimer == 0) {
                be.updateLitBlockState(false);
            }
        }

        // 6. 自动进入休眠判定
        // 条件 A: 没有电源且不是脉冲帧 (无电强行休眠)
        // 条件 B: 工作已完成 (无 Dirty 标记)
        boolean lackPower = !be.isPowered && !be.isPulseFrame;
        boolean workDone = !be.isInputsDirty();

        if ((lackPower || workDone) && be.litTimer <= 0) {
            be.isSleeping = true;
            be.pendingProcess = false;
        }

        be.lastPowered = be.isPowered;
    }

    // =========================================================
    // 2. 事件响应与脏标记 (Event Driven API)
    // =========================================================

    /**
     * 智能环境更新响应 (由 StructureManager 调用)
     *
     * @param targetPos    发生变动的坐标
     * @param isItemUpdate 是否仅为物品/实体变动
     */
    public void onEnvironmentUpdate(BlockPos targetPos, boolean isItemUpdate) {
        // 1. 如果只是物品/实体变动 -> 标记实体脏并唤醒
        if (isItemUpdate) {
            markEntityCacheDirty();
            wakeUp();
            return;
        }

        // 2. 如果结构尚未成型 -> 标记验证并唤醒 (验证逻辑在 tick 开头)
        if (!structure.isFormed()) {
            this.validationPending = true;
            wakeUp();
            return;
        }

        // 3. 判断变动位置类型
        boolean isInternal = structure.isInterior(targetPos);

        if (!isInternal) {
            // 如果是结构的一部分(含外壳) -> 标记验证并唤醒
            if (structure.contains(targetPos)) {
                this.validationPending = true;
                wakeUp();
            } else {
                // 否则是外部热源变动 -> 重建热力缓存并唤醒
                thermo.rebuildCache(level, structure);
                wakeUp();
            }
        } else {
            // 是内部方块变动 -> 标记方块缓存脏并唤醒
            markBlockCacheDirty();
            wakeUp();
        }
    }

    public void markBlockCacheDirty() {
        this.blockCacheDirty = true;
        wakeUp(); // [新增]
    }

    public void markEntityCacheDirty() {
        this.entityCacheDirty = true;
        wakeUp(); // [新增]
    }

    public void markPortsDirty() {
        this.portsDirty = true;
        // 端口变动通常意味着库存能力变动，也应唤醒实体处理
        this.entityCacheDirty = true;
        wakeUp(); // [新增]
    }

    public void markCatalystDirty() {
        this.catalystDirty = true;
        wakeUp(); // [新增]
    }

    public void updatePoweredState(boolean newSignal) {
        this.isPowered = newSignal;
        // 红石信号变化可能触发脉冲配方，强制唤醒实体检测
        if (this.isPowered != this.lastPowered) {
            markEntityCacheDirty();
        }
    }

    /**
     * 更新性能参数 (仅在结构成型或升级卡变动时调用)
     */
    public void updatePerformance() {
        ItemStack upgradeStack = itemHandler.getStackInSlot(1);
        int count = upgradeStack.isEmpty() ? 0 : upgradeStack.getCount();

        this.performance.update(this.structure, count);

        // 更新热容上限
        int newCapacity;
        if (this.performance.isVirtual()) {
            newCapacity = 10000 + (count * 50000);
        } else {
            int vol = this.structure.isFormed() ? this.structure.getVolume() : 0;
            newCapacity = 10000 + (vol * 1000);
        }
        this.thermo.setMaxHeatCapacity(newCapacity);
    }

    // =========================================================
    // 3. 结构与验证逻辑
    // =========================================================

    /**
     * 惰性破坏通知
     * 当组件(接口/外壳)被破坏时调用。
     * 仅标记结构失效，不执行扫描。如果在世界卸载期间调用，tick 永远不会运行，从而避免死锁。
     */
    public void notifyStructureBroken() {
        if (this.structure.isFormed()) {
            // 1. 立即逻辑失效，停止配方处理
            this.structure.reset(this.level);
            // 2. 标记需要验证，但等到下一次 Tick 且世界稳定时才执行
            this.validationPending = true;
            // 3. 强制标记所有缓存脏，防止复用旧数据
            this.blockCacheDirty = true;
            this.entityCacheDirty = true;
            this.portsDirty = true;
            this.catalystDirty = true;
        }
    }

    public void performValidation(@Nullable Player player) {
        if (level == null || level.isClientSide) return;
        BlockState state = getBlockState();
        if (!state.is(ThermalShockBlocks.SIMULATION_CHAMBER_CONTROLLER.get())) return;

        structure.reset(level);
        var result = MultiblockValidator.validate(level, worldPosition, state.getValue(SimulationChamberBlock.FACING));
        structure.update(level, worldPosition, state, result);

        if (structure.isFormed()) {
            errorPos = null;
            // 结构成型，重建所有缓存
            thermo.rebuildCache(level, structure);
            updatePerformance();
            refreshPortCache();
            // 标记所有输入脏，确保立即扫描一次
            markBlockCacheDirty();
            markEntityCacheDirty();

            if (player != null)
                player.displayClientMessage(Component.translatable("message.thermalshock.complete"), true);
        } else {
            updatePerformance(); // 重置为0效率
            errorPos = result.errorPos();
            if (player != null)
                player.displayClientMessage(Component.translatable("message.thermalshock.invalid").append(result.errorMessage()), true);
        }
        setChanged();
        syncData();
    }

    private void refreshPortCache() {
        cachedInputPorts.clear();
        cachedOutputPorts.clear();
        cachedCatalystPorts.clear();

        if (level == null || !structure.isFormed()) return;

        for (BlockPos pos : structure.getPorts()) {
            if (level.getBlockEntity(pos) instanceof SimulationPortBlockEntity port) {
                switch (port.getPortMode()) {
                    case INPUT -> cachedInputPorts.add(pos);
                    case OUTPUT -> cachedOutputPorts.add(pos);
                    case CATALYST -> cachedCatalystPorts.add(pos);
                }
            }
        }
        this.portsDirty = false;
        // 端口缓存刷新后，可能解锁了新的物品输入路径，标记实体脏
        this.entityCacheDirty = true;
    }

    // =========================================================
    // 4. 催化剂逻辑 (优化版)
    // =========================================================

    public void processCatalyst() {
        if (!this.structure.isFormed()) return;

        // 只要 Buffer 没满且有脏标记，就尝试循环填充
        if (catalystBuffer < 1000.0f && this.catalystDirty) {

            // 1. 循环填充：从接口 (直到满或没东西)
            boolean keepRefilling = true;
            while (keepRefilling && catalystBuffer < 1000.0f) {
                keepRefilling = refillBufferFromPorts();
            }

            // 2. 循环填充：从内部 Slot
            while (catalystBuffer <= 90.0f) { // 留一点余量防止溢出浪费
                ItemStack s = itemHandler.getStackInSlot(0);
                if (s.isEmpty()) break;

                var d = BuiltInRegistries.ITEM.wrapAsHolder(s.getItem())
                        .getData(ThermalShockDataMaps.CATALYST_PROPERTY);
                if (d != null) {
                    currentBonusYield = d.bonusYield();
                    catalystBuffer += d.bufferAmount();
                    s.shrink(1);
                    setChanged();
                } else {
                    break;
                }
            }

            // 处理完毕，清除标记
            this.catalystDirty = false;
        }
    }

    private boolean refillBufferFromPorts() {
        for (BlockPos pos : cachedCatalystPorts) {
            if (level.getBlockEntity(pos) instanceof SimulationPortBlockEntity port) {
                IItemHandler handler = port.getItemHandler();
                for (int i = 0; i < handler.getSlots(); i++) {
                    ItemStack stack = handler.extractItem(i, 1, true); // Simulate
                    if (!stack.isEmpty()) {
                        var d = BuiltInRegistries.ITEM.wrapAsHolder(stack.getItem())
                                .getData(ThermalShockDataMaps.CATALYST_PROPERTY);
                        if (d != null) {
                            handler.extractItem(i, 1, false); // Execute
                            this.catalystBuffer += d.bufferAmount();
                            this.currentBonusYield = d.bonusYield();
                            setChanged();
                            return true; // 吃到一个，返回 true
                        }
                    }
                }
            }
        }
        return false; // 遍历所有接口都没吃到，返回 false
    }

    public float calculateCatalystBonus(float efficiency) {
        if (currentBonusYield <= 0 || efficiency <= 0) return 0.0f;
        return catalystBuffer > 0 ? currentBonusYield : 0.0f;
    }

    public void consumeCatalystBuffer(int batchSize, float efficiency) {
        if (efficiency <= 0) return;
        float costPerItem = 1.0f / efficiency;
        float totalCost = costPerItem * batchSize;
        this.catalystBuffer = Math.max(0, this.catalystBuffer - totalCost);
        setChanged();
    }

    // =========================================================
    // 5. 辅助方法与 Getters/Setters
    // =========================================================

    public void onRecipeSuccess() {
        // 未锁定模式：单次运行后清除配方
        if (!this.recipeLocked && this.selectedRecipeId != null) {
            this.selectedRecipeId = null;
            this.setChanged();
            if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
        // 成功运行后，清除缓存脏标记 (Process 会处理，但此处兜底)
        this.blockCacheDirty = false;
        this.entityCacheDirty = false;
    }

    public void setLitState(boolean lit) {
        if (lit) {
            this.litTimer = 20;
            updateLitBlockState(true);
        } else {
            this.litTimer = 0;
            updateLitBlockState(false);
        }
    }

    private void updateLitBlockState(boolean isLit) {
        BlockState s = getBlockState();
        if (s.hasProperty(SimulationChamberBlock.LIT) && s.getValue(SimulationChamberBlock.LIT) != isLit) {
            level.setBlock(worldPosition, s.setValue(SimulationChamberBlock.LIT, isLit), 3);
        }
    }

    // --- Accessors for Process ---
    public ChamberStructure getStructure() {
        return structure;
    }

    public ChamberThermodynamics getThermo() {
        return thermo;
    }

    public boolean isPowered() {
        return isPowered;
    }

    public boolean isRisingEdge() {
        return isPowered && !lastPowered;
    }

    public MachineMode getMachineMode() {
        return mode;
    }

    public ResourceLocation getSelectedRecipeId() {
        return selectedRecipeId;
    }

    public IItemHandler getItemHandler() {
        return itemHandler;
    }

    public boolean isPulseFrame() {
        return isPulseFrame;
    }

    public boolean isInputsDirty() {
        return blockCacheDirty || entityCacheDirty;
    }

    /**
     * 统一设置输入脏标记
     * process 调用 setInputsDirty(true) 时，同时标记方块和实体脏，强制重扫描
     */
    public void setInputsDirty(boolean dirty) {
        this.blockCacheDirty = dirty;
        this.entityCacheDirty = dirty;
    }

    public boolean isLocked() {
        return recipeLocked;
    }

    // Process 需要读取这些脏标记来决定是否重建缓存
    public boolean isBlockCacheDirty() {
        return blockCacheDirty;
    }

    public boolean isEntityCacheDirty() {
        return entityCacheDirty;
    }

    // Process 处理完后调用此方法清除脏标记
    public void clearCacheDirtyFlags() {
        this.blockCacheDirty = false;
        this.entityCacheDirty = false;
    }

    public void addAccumulatedYield(float amount) {
        this.accumulatedYield += amount;
        setChanged();
    }

    public int popAccumulatedYield() {
        int count = (int) accumulatedYield;
        accumulatedYield -= count;
        setChanged();
        return count;
    }

    // --- Accessors for External ---
    public BlockPos getMinPos() {
        return structure.getMinPos();
    }

    public BlockPos getMaxPos() {
        return structure.getMaxPos();
    }

    public BlockPos getErrorPos() {
        return errorPos;
    }

    public List<BlockPos> getCachedInputPorts() {
        return cachedInputPorts;
    }

    public List<BlockPos> getCachedOutputPorts() {
        return cachedOutputPorts;
    }

    public boolean isFormed() {
        return structure.isFormed();
    }


    public BlockState getCamouflageState() {
        return structure.getCamouflageState();
    }

    public void setCamouflageState(BlockState s) {
        this.structure.setCamouflageStateOnly(s);
        setChanged();
        syncData();
        wakeUp(); // [新增]
    }

    // =========================================================
    // 6. 交互与网络同步
    // =========================================================

    public void requestModeChange() {
        this.mode = this.mode.next();
        this.thermo.clearHeat(); // [修改] 使用 clearHeat()
        setChanged();
        syncData();
    }

    public void toggleLock() {
        recipeLocked = !recipeLocked;
        setChanged();
        syncData();
    }

    public void setSelectedRecipe(ResourceLocation id) {
        this.selectedRecipeId = id;
        if (id != null) {
            // 切换配方时，强制唤醒检查
            markEntityCacheDirty();
            markBlockCacheDirty();
        }
        setChanged();
        syncData();
    }

    private void syncData() {
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    // =========================================================
    // 7. NBT 数据存储
    // =========================================================

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider r) {
        super.saveAdditional(tag, r);
        structure.save(tag);
        thermo.save(tag);
        tag.put("Inventory", itemHandler.serializeNBT(r));
        tag.putString("Mode", mode.name());
        tag.putFloat("CatalystBuffer", catalystBuffer);
        tag.putFloat("CurrentBonusYield", currentBonusYield);
        tag.putFloat("AccumulatedYield", accumulatedYield);
        if (selectedRecipeId != null) tag.putString("SelectedRecipe", selectedRecipeId.toString());
        tag.putBoolean("RecipeLocked", recipeLocked);
        if (errorPos != null) tag.putLong("ErrorPos", errorPos.asLong());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider r) {
        super.loadAdditional(tag, r);
        structure.load(tag, r);
        thermo.load(tag);
        itemHandler.deserializeNBT(r, tag.getCompound("Inventory"));
        if (tag.contains("Mode")) mode = MachineMode.valueOf(tag.getString("Mode"));
        catalystBuffer = tag.getFloat("CatalystBuffer");
        if (tag.contains("CurrentBonusYield")) currentBonusYield = tag.getFloat("CurrentBonusYield");
        accumulatedYield = tag.getFloat("AccumulatedYield");
        selectedRecipeId = tag.contains("SelectedRecipe") ?
                ResourceLocation.tryParse(tag.getString("SelectedRecipe")) : null;
        recipeLocked = tag.getBoolean("RecipeLocked");
        if (tag.contains("ErrorPos")) errorPos = BlockPos.of(tag.getLong("ErrorPos"));
        else errorPos = null;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider r) {
        CompoundTag t = new CompoundTag();
        saveAdditional(t, r);
        return t;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider r) {
        loadAdditional(pkt.getTag(), r);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            StructureManager.trackController(level, worldPosition);

            // [核心修复] 进服/区块加载时，恢复内存中的临时数据
            if (structure.isFormed()) {
                // 1. 恢复结构属性 (效率、体积等)，依赖 NBT 加载的 camouflageState
                structure.recalculateStats(structure.getCamouflageState().getBlock());

                // 2. 恢复热力环境缓存 (扫描周围热源)
                thermo.rebuildCache(level, structure);

                // 3. 恢复性能参数 (升级卡影响)
                updatePerformance();

                // 4. 强制刷新端口缓存 (因为 cachedInputPorts 列表是空的)
                this.portsDirty = true;
            }

            // 延迟一刻验证，防止区块加载时的顺序问题 (保留原有逻辑，作为双重保险)
            level.scheduleTick(worldPosition, getBlockState().getBlock(), 1);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && !level.isClientSide) {
            StructureManager.untrackController(level, worldPosition);
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.thermalshock.simulation_chamber_controller");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int i, Inventory inv, Player p) {
        return new SimulationChamberMenu(i, inv, this, this.data);
    }
}