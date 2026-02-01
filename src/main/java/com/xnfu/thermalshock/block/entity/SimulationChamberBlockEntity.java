package com.xnfu.thermalshock.block.entity;

import com.xnfu.thermalshock.block.SimulationChamberBlock;
import com.xnfu.thermalshock.client.gui.SimulationChamberMenu;
import com.xnfu.thermalshock.recipe.AbstractSimulationRecipe;
import com.xnfu.thermalshock.item.SimulationUpgradeItem;
import com.xnfu.thermalshock.registries.*;
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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
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
            if (slot == 0) {
                markCatalystDirty();
            } else if (slot == 1) {
                // 升级卡变动，立即更新性能参数
                updatePerformance();
                // 升级卡可能改变虚拟化状态，导致端口逻辑变化
                portsDirty = true;
                // 升级卡变动可能解锁配方限制，唤醒处理逻辑
                markEntityCacheDirty();
                wakeUp();
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

    // [重构] 已移除 pendingProcess，使用 scheduleTick 事件驱动

    // [关键修复] 验证逻辑懒加载
    private boolean validationPending = false;
    private boolean forceRevalidate = false; // [新增]
    private AbstractSimulationRecipe selectedRecipe = null; // [新增]
    private ResourceLocation matchedRecipeId = null; // [新增] 实时匹配成功的配方 ID
    private AbstractSimulationRecipe matchedRecipe = null; // [新增] 实时匹配成功的配方实例

    // === 缓存与脏标记 (Granular Dirty Flags) ===
    private BlockPos errorPos = null;

    // 逻辑执行相关
    private boolean blockCacheDirty = true;  // 内部方块变动
    private boolean entityCacheDirty = true; // 内部实体/接口库存变动

    // 内部快照缓存 (Snapshot)
    private final List<ChamberProcess.FoundMaterial> internalBlockCache = new ArrayList<>();
    private final List<ChamberProcess.FoundMaterial> internalEntityCache = new ArrayList<>();

    // 辅助缓存
    private boolean heatDirty = true; // [新增] 热量脏标记
    private boolean portsDirty = true;
    private boolean catalystDirty = true;

    // === 红石状态 (核心修复) ===
    private boolean isPowered = false;    // 持续信号
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
            // [修复] 移除主动计算，只读取缓存值，避免 GUI 打开时的高频计算
            // 热量计算现在由 tick 中的 heatDirty 驱动

            return switch (index) {
                case 0 -> mode == MachineMode.OVERHEATING ? thermo.getCurrentHeat() : thermo.getDeltaT();
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

    public void wakeUp() {
        // [重构] 事件驱动：使用 scheduleTick 唤醒而非标记
        if (level != null && !level.isClientSide) {
            // 使用 scheduleTick 在下一刻调度执行
            level.scheduleTick(worldPosition, getBlockState().getBlock(), 1);
        }
    }

    /**
     * [新增] 重建内部缓存
     * 只有在 blockCacheDirty 或 entityCacheDirty 为 true 时才会被调用
     */
    public void rebuildInternalCache() {
        if (level == null || !structure.isFormed()) return;

        // 1. 重建方块缓存
        if (this.blockCacheDirty) {
            this.internalBlockCache.clear();
            BlockPos min = structure.getMinPos();
            BlockPos max = structure.getMaxPos();
            // 扫描内部 (排除外壳)
            BlockPos.betweenClosed(min.offset(1, 1, 1), max.offset(-1, -1, -1))
                    .forEach(p -> {
                        BlockState s = level.getBlockState(p);
                        if (!s.isAir()) {
                            ItemStack stack;
                            // [流体支持] 如果是液体方块，转换为对应的桶物品存入缓存，以便配方匹配
                            if (!s.getFluidState().isEmpty() && s.getFluidState().isSource()) {
                                stack = new ItemStack(s.getFluidState().getType().getBucket());
                            } else {
                                stack = new ItemStack(s.getBlock());
                            }
                            
                            if (!stack.isEmpty()) {
                                // 存入快照
                                this.internalBlockCache.add(new ChamberProcess.FoundMaterial(p.immutable(), stack));
                            }
                        }
                    });
            this.blockCacheDirty = false;
        }

        // 2. 重建实体缓存
        if (this.entityCacheDirty) {
            this.internalEntityCache.clear();
            AABB box = AABB.encapsulatingFullBlocks(structure.getMinPos(), structure.getMaxPos());
            List<ItemEntity> entities = level.getEntitiesOfClass(ItemEntity.class, box);
            for (ItemEntity ie : entities) {
                if (ie.isAlive()) {
                    this.internalEntityCache.add(new ChamberProcess.FoundMaterial(ie, ie.getItem()));
                }
            }
            this.entityCacheDirty = false;
        }
    }

    /**
     * [重构] 事件驱动核心方法
     * 由 Block.tick() (scheduleTick 回调) 调用
     * 不再使用每刻 Ticker，完全按需执行
     */
    public void onScheduledTick() {
        if (level == null || level.isClientSide) return;

        // === 优先级 1: 结构验证 ===
        if (validationPending) {
            StructureManager.requestValidation(worldPosition);
            validationPending = false;
            return; 
        }

        // === 优先级 2: 环境状态检测 (Redstone) ===
        boolean currentSignal = level.hasNeighborSignal(worldPosition);
        isPulseFrame = currentSignal && !isPowered;
        
        if (currentSignal != isPowered) {
            isPowered = currentSignal;
            updatePoweredState(isPowered);
        }

        // === 优先级 3: 环境热量计算 (Recalculate) ===
        if (structure.isFormed() && heatDirty) {
            thermo.recalculate(level, structure);
            heatDirty = false;
        }

        boolean shouldLoopFast = false;

        // === 优先级 4: 热量动态更新 (Accumulation) ===
        if (structure.isFormed()) {
            int netInput = thermo.getLastInputRate();
            int currentHeat = thermo.getCurrentHeat();
            int maxHeat = thermo.getMaxHeatCapacity();

            // [核心优化] 严格限流：只有在热量确实会发生变化时（未满且输入>0，或未空且输入<0），才执行累积并维持高频更新
            if (netInput > 0 && currentHeat < maxHeat) {
                thermo.addHeat(netInput);
                shouldLoopFast = true;
            } else if (netInput < 0 && currentHeat > 0) {
                thermo.addHeat(netInput);
                shouldLoopFast = true;
            }
        }

        // === 优先级 5: 配方逻辑处理 ===
        boolean hasPermission = isPulseFrame || isPowered;
        if (structure.isFormed() && hasPermission) {
            // 同步所有缓存
            if (catalystDirty) processCatalyst();
            if (portsDirty) refreshPortCache();
            if (blockCacheDirty || entityCacheDirty) rebuildInternalCache();

            // 执行核心配方逻辑
            boolean success = process.tick(level);
            if (success) {
                shouldLoopFast = true; // 配方运行中，维持高频更新
            }
        }

        // === 优先级 6: 视觉状态管理 ===
        if (litTimer > 0) {
            litTimer--;
            if (litTimer == 0) {
                updateLitBlockState(false);
            } else {
                shouldLoopFast = true; // 视觉动画进行中，维持高频更新
            }
        }

        // === 总结决策：是否需要维持链式唤醒？ ===
        if (shouldLoopFast) {
            level.scheduleTick(worldPosition, getBlockState().getBlock(), 1);
        } else if (isPowered && recipeLocked && selectedRecipeId != null) {
            // [优化] 如果只是供电中但暂时无事可做（如配方锁定了但没材料），此时不使用 1t 更新。
            // 使用较低频的 10t 心跳检查新输入的掉落物，节省性能
            level.scheduleTick(worldPosition, getBlockState().getBlock(), 10);
        }
    }

    // =========================================================
    // 2. 事件响应与脏标记 (Event Driven API)
    // =========================================================

    public void onEnvironmentUpdate(BlockPos targetPos, StructureManager.UpdateType type) {
        // 1. 如果只是物品/实体变动 -> 标记实体脏并唤醒
        if (type == StructureManager.UpdateType.ITEM) {
            markEntityCacheDirty();
            wakeUp();
            return;
        }

        // 2. 如果结构尚未成型 -> 标记验证并唤醒
        if (!structure.isFormed()) {
            this.validationPending = true;
            wakeUp();
            return;
        }

        // [核心修复] 结构成型时的智能触发
        if (structure.contains(targetPos)) {
            // A. 破坏方块 (BREAK) -> 如果是外壳/组件，必须重扫描
            if (type == StructureManager.UpdateType.BREAK) {
                if (structure.isInterior(targetPos)) {
                    markBlockCacheDirty();
                } else {
                    this.validationPending = true;
                    wakeUp();
                }
            }
            // B. 放置方块 (PLACE) -> 执行完整性校验
            else if (type == StructureManager.UpdateType.PLACE) {
                if (performIntegrityCheck(targetPos)) {
                    markBlockCacheDirty();
                } else {
                    this.validationPending = true;
                    wakeUp();
                }
            }
            return;
        }

        // 3. 外部环境变动 (热源等) -> 重建热力缓存
        this.heatDirty = true;
        wakeUp();
    }

    public void markBlockCacheDirty() {
        this.blockCacheDirty = true;
        wakeUp(); // 物理输入变动，必须唤醒
    }

    public void markEntityCacheDirty() {
        this.entityCacheDirty = true;
        wakeUp(); // 物理输入变动，必须唤醒
    }

    public void markPortsDirty() {
        this.portsDirty = true;
        // 端口变动可能涉及输入/输出变化，唤醒检查
        wakeUp();
    }

    public void markCatalystDirty() {
        this.catalystDirty = true;
        wakeUp(); // 催化剂变动建议唤醒以填充 Buffer
    }

    public void updatePoweredState(boolean newSignal) {
        // 红石信号变化可能触发脉冲配方，强制唤醒实体检测
        markEntityCacheDirty();
        wakeUp();
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
        
        // [新增] 性能变动可能改变速率限制，标记热脏并唤醒
        this.heatDirty = true;
        wakeUp();
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
            // 1. 立即逻辑失效
            StructureManager.removeStructure(level, worldPosition);
            this.structure.reset(this.level);
            // 2. 标记验证，并注册到未成型列表监听后续变化
            this.validationPending = true;
            StructureManager.registerPending(level, worldPosition, getSearchBox());
            
            this.blockCacheDirty = true;
            this.entityCacheDirty = true;
            this.portsDirty = true;
            this.catalystDirty = true;
        }
    }

    /**
     * [新增] 获取 13x13x13 的扫描监听范围
     */
    public AABB getSearchBox() {
        // 根据控制器朝向，它总是位于结构的一条棱上（通常是后端或侧端）
        // 这里提供一个包含最大可能结构的保守 AABB (13x13x13)
        // 由于控制器在侧棱，我们向所有方向扩展 12 格以确保覆盖
        return new AABB(worldPosition).inflate(12);
    }

    /**
     * [新增] 完整性校验
     * @return true 如果结构依然被认为是完整的，false 需要全量重扫描
     */
    private boolean performIntegrityCheck(BlockPos pos) {
        if (level == null || !structure.isFormed()) return false;
        BlockState state = level.getBlockState(pos);
        
        // 如果是空气，绝对破坏了完整性
        if (state.isAir()) return false;
        
        // 如果是内部空间变动，不影响结构完整性
        if (structure.isInterior(pos)) return true;
        
        // 核心规则：如果是外壳，检查它是否仍然是合法的组件且材质一致
        if (state.is(ThermalShockBlocks.SIMULATION_CHAMBER_CONTROLLER.get())) return true;
        if (state.is(ThermalShockBlocks.SIMULATION_CHAMBER_PORT.get())) return true;
        if (state.is(ThermalShockTags.VENTS)) return true;
        
        // 检查外壳方块一致性 (解决用户提到的“放置不同方块不更新”问题)
        return state.is(structure.getCamouflageState().getBlock());
    }

    /**
     * [新增] 惰性验证 - 用于打开 GUI 前的快速检查
     * 如果结构未成型或有挂起的验证，则标记验证并唤醒
     */
    public void performLazyValidation() {
        if (!this.structure.isFormed() || this.validationPending) {
            this.validationPending = true;
            wakeUp();
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
            // 结构成型，标记热量脏以便重算
            this.heatDirty = true;
            updatePerformance();
            refreshPortCache();

            // [新增] 注册结构包围盒 (用于事件驱动)
            AABB bounds = AABB.encapsulatingFullBlocks(structure.getMinPos(), structure.getMaxPos());
            StructureManager.updateStructure(level, worldPosition, bounds);

            // 标记所有输入脏，确保立即扫描一次
            markBlockCacheDirty();
            markEntityCacheDirty();

            if (player != null)
                player.displayClientMessage(Component.translatable("message.thermalshock.complete"), true);
        } else {
            updatePerformance(); // 重置为0效率
            errorPos = result.errorPos();
            
            // [新增] 注册到未成型列表，监听 13x13x13 范围内的变动
            StructureManager.registerPending(level, worldPosition, getSearchBox());

            // [修复] 验证失败时必须同步 errorPos 到客户端，否则无法渲染红框
            setChanged();
            if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);

            if (player != null)
                player.displayClientMessage(Component.translatable("message.thermalshock.invalid").append(result.errorMessage()), true);
        }
        setChanged();
        syncData();
    }

    public MachineMode getMode() { return mode; }
    public int getHeat() { return thermo.getCurrentHeat(); }
    public int getDelta() { return thermo.getDeltaT(); }
    public int getNetInputRate() { 
        return mode == MachineMode.OVERHEATING ? thermo.getLastInputRate() : (int) thermo.getCurrentHighTemp(); 
    }
    public int getMaxBatchSize() { return performance.getBatchSize(); }
    public boolean isRecipeLocked() { return recipeLocked; }

    // [新增] 检查是否有效且有变更 (Dirty Flag Check)
    // 用于 ChamberProcess 决定是否需要重新扫描配方
    public boolean isValidAndChanged() {
        return this.structure.isFormed() && (this.blockCacheDirty || this.entityCacheDirty || this.forceRevalidate);
    }
    
    public AbstractSimulationRecipe getRuntimeRecipe() {
        if (selectedRecipe == null && selectedRecipeId != null && level != null) {
            this.selectedRecipe = resolveRecipeById(selectedRecipeId);
        }
        return this.selectedRecipe;
    }

    private AbstractSimulationRecipe resolveRecipeById(ResourceLocation id) {
        if (id == null || level == null) return null;
        var recipe = level.getRecipeManager().byKey(id).orElse(null);
        if (recipe != null && recipe.value() instanceof AbstractSimulationRecipe r) {
            return r;
        }
        return null;
    }
    
    public void setRuntimeRecipe(AbstractSimulationRecipe recipe) {
        this.selectedRecipe = recipe;
    }

    public ResourceLocation getMatchedRecipeId() {
        return matchedRecipeId != null ? matchedRecipeId : selectedRecipeId;
    }

    public void setMatchedRecipe(ResourceLocation id, AbstractSimulationRecipe recipe) {
        this.matchedRecipeId = id;
        this.matchedRecipe = recipe;
    }

    public AbstractSimulationRecipe getMatchedRecipe() {
        return matchedRecipe != null ? matchedRecipe : getRuntimeRecipe();
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
        // [修复] 成功运行后，材料已被消耗，需要标记缓存脏以强制下次重新扫描
        this.blockCacheDirty = true;
        this.entityCacheDirty = true;
        
        // [修复] 如果配方锁定，唤醒机器以便检查新材料
        if (this.recipeLocked) {
            wakeUp();
        }
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
        return isPulseFrame;
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

    public List<ChamberProcess.FoundMaterial> getInternalBlockCache() {
        return internalBlockCache;
    }

    public List<ChamberProcess.FoundMaterial> getInternalEntityCache() {
        return internalEntityCache;
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
        wakeUp();
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
        this.selectedRecipe = resolveRecipeById(id);
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
            // [核心修复] 进服/区块加载时，恢复内存中的临时数据
            if (structure.isFormed()) {
                // 1. 恢复结构属性 (效率、体积等)，依赖 NBT 加载的 camouflageState
                structure.recalculateStats(structure.getCamouflageState().getBlock());

                // 2. 恢复热力环境缓存 (扫描周围热源)
                thermo.recalculate(level, structure);

                // 3. 恢复性能参数 (升级卡影响)
                updatePerformance();

                // 4. 强制刷新端口缓存 (因为 cachedInputPorts 列表是空的)
                this.portsDirty = true;

                AABB bounds = AABB.encapsulatingFullBlocks(structure.getMinPos(), structure.getMaxPos());
                StructureManager.updateStructure(level, worldPosition, bounds);
            } else {
                // 未成型时也需要加入待定列表
                StructureManager.registerPending(level, worldPosition, getSearchBox());
            }

            // 延迟一刻验证，防止区块加载时的顺序问题
            level.scheduleTick(worldPosition, getBlockState().getBlock(), 1);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && !level.isClientSide) {
            StructureManager.removeStructure(level, worldPosition);
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