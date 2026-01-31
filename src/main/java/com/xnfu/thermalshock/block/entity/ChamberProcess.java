package com.xnfu.thermalshock.block.entity;

import com.xnfu.thermalshock.ThermalShock;
import com.xnfu.thermalshock.data.ClumpInfo;
import com.xnfu.thermalshock.recipe.*;
import com.xnfu.thermalshock.registries.ThermalShockDataComponents;
import com.xnfu.thermalshock.registries.ThermalShockItems;
import com.xnfu.thermalshock.registries.ThermalShockRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ChamberProcess {
    private static final ResourceLocation GENERIC_CLUMP_ID =
            ResourceLocation.fromNamespaceAndPath(ThermalShock.MODID, "generic_clump_processing");

    private final SimulationChamberBlockEntity be;
    private Vec3 primaryOutputPos = null;

    public ChamberProcess(SimulationChamberBlockEntity be) {
        this.be = be;
    }

    // [核心修改] 返回 boolean: true 表示需要继续循环，false 表示本次任务结束(或失败)
    public boolean tick(Level level) {
        // 1. 结构检查
        if (!be.getStructure().isFormed()) return false;

        // 2. 配方检查
        ResourceLocation selectedId = be.getSelectedRecipeId();
        if (selectedId == null) {
            be.setInputsDirty(false);
            return false;
        }

        // 3. 分支处理
        if (be.performance.isVirtual()) {
            return tickVirtual(level, selectedId);
        } else {
            return tickPhysical(level, selectedId);
        }
    }

    // =========================================================
    // 1. 虚拟化处理 (Virtual Mode)
    // =========================================================
    private boolean tickVirtual(Level level, ResourceLocation selectedId) {
        List<BlockPos> inputPosList = be.getCachedInputPorts();
        List<BlockPos> outputPosList = be.getCachedOutputPorts();
        if (inputPosList.isEmpty() || outputPosList.isEmpty()) return false;

        AbstractSimulationRecipe targetRecipe = resolveRecipeById(level, selectedId);
        boolean isGenericClumpMode = GENERIC_CLUMP_ID.equals(selectedId);

        if (!isGenericClumpMode && targetRecipe == null) {
            // [修复] 只有未锁定时才清除配方，锁定时保留配方等待条件满足
            if (!be.isLocked()) {
                be.setSelectedRecipe(null);
            }
            return false;
        }

        AbstractSimulationRecipe recipeToRun = targetRecipe;
        if (isGenericClumpMode) {
            recipeToRun = findFirstValidGenericRecipe(level, inputPosList);
            if (recipeToRun == null) {
                be.setMatchedRecipe(null, null);
                return false;
            }
        }
        
        // 实时更新匹配成功的配方，供 Jade 等显示
        AbstractSimulationRecipe matchedRecipeRef = recipeToRun;
        ResourceLocation matchedId = selectedId;
        if (isGenericClumpMode && level != null) {
            var overType = ThermalShockRecipes.OVERHEATING_TYPE.get();
            var shockType = ThermalShockRecipes.THERMAL_SHOCK_TYPE.get();
            matchedId = level.getRecipeManager().getAllRecipesFor(overType).stream()
                        .filter(h -> h.value() == matchedRecipeRef).findFirst().map(net.minecraft.world.item.crafting.RecipeHolder::id)
                        .orElseGet(() -> level.getRecipeManager().getAllRecipesFor(shockType).stream()
                                .filter(h -> h.value() == matchedRecipeRef).findFirst().map(net.minecraft.world.item.crafting.RecipeHolder::id).orElse(null));
        }
        be.setMatchedRecipe(matchedId, recipeToRun);

        // 计算
        int maxBatchByItems = calculateMaxBatchFromPorts(level, recipeToRun, inputPosList);
        if (maxBatchByItems <= 0) {
            be.setInputsDirty(checkForMatchingInputButInsufficient(level, recipeToRun, inputPosList));
            return false;
        }

        if (!checkThermalConditions(recipeToRun)) {
            be.setInputsDirty(true);
            return false;
        }

        // 空间预判 (收集 Pool)
        List<ItemStack> poolIn = new ArrayList<>();
        List<RecipeSourceType> poolTy = new ArrayList<>();
        for (BlockPos pos : inputPosList) {
            BlockEntity pbe = level.getBlockEntity(pos);
            if (pbe instanceof SimulationPortBlockEntity port) {
                IItemHandler handler = port.getItemHandler();
                for (int i = 0; i < handler.getSlots(); i++) {
                    ItemStack s = handler.getStackInSlot(i);
                    if (!s.isEmpty()) {
                        poolIn.add(s);
                        poolTy.add(RecipeSourceType.ITEM);
                    }
                }
            }
        }

        ItemStack resultTemplate = recipeToRun.assemble(new SimulationRecipeInput(ItemStack.EMPTY, poolIn, poolTy), level.registryAccess());
        if (!resultTemplate.isEmpty()) {
            float yieldMult = be.performance.getYieldMultiplier();
            float catalystBonus = be.calculateCatalystBonus(be.performance.getEfficiency());
            double outputPerOp = resultTemplate.getCount() * yieldMult * (1.0 + catalystBonus);
            int requiredSpacePerOp = Math.max(1, (int) Math.ceil(outputPerOp));

            int totalSpace = calculateAvailableOutputSpace(level, resultTemplate, outputPosList);
            if (totalSpace < requiredSpacePerOp) {
                be.setInputsDirty(false);
                return false;
            }
            int maxBatchByOutput = totalSpace / requiredSpacePerOp;
            maxBatchByItems = Math.min(maxBatchByItems, maxBatchByOutput);
        }

        // 核心批次计算 (v17 逻辑)
        int structureMax = be.performance.getBatchSize();
        int limit = Math.min(structureMax, maxBatchByItems);
        int actualBatch = calculateExecutionBatch(limit, recipeToRun);

        if (actualBatch <= 0) return false;

        // 执行
        consumeIngredientsFromPorts(level, recipeToRun, actualBatch, inputPosList);

        if (be.getMachineMode() == MachineMode.OVERHEATING && recipeToRun instanceof OverheatingRecipe ov) {
            // [修复] 传入 level
            be.getThermo().consumeHeat(level, ov.getHeatCost() * actualBatch);
        }
        be.consumeCatalystBuffer(actualBatch, be.performance.getEfficiency());

        if (!resultTemplate.isEmpty()) {
            float yieldMult = be.performance.getYieldMultiplier();
            float catalystBonus = be.calculateCatalystBonus(be.performance.getEfficiency());
            float totalItemsFloat = actualBatch * resultTemplate.getCount() * yieldMult * (1.0f + catalystBonus);

            be.addAccumulatedYield(totalItemsFloat);
            int outputCount = be.popAccumulatedYield();

            if (outputCount > 0) {
                ItemStack totalOutput = resultTemplate.copy();
                totalOutput.setCount(outputCount);
                distributeOutput(level, totalOutput, outputPosList);
            }
        }

        be.onRecipeSuccess();
        
        // [优化] 如果未锁定，返回 false 以便立即休眠 (因为配方已被清除)
        // 如果锁定，返回 true，控制器会尝试在下一 Tick 继续运行
        return be.isLocked();
    }

    // =========================================================
    // 2. 物理处理 (Physical Mode)
    // =========================================================
    private boolean tickPhysical(Level level, ResourceLocation selectedId) {
        AbstractSimulationRecipe targetRecipe = resolveRecipeById(level, selectedId);
        boolean isGenericClumpMode = GENERIC_CLUMP_ID.equals(selectedId);

        if (!isGenericClumpMode && targetRecipe == null) {
            // [修复] 只有未锁定时才清除配方，锁定时保留配方等待条件满足
            if (!be.isLocked()) {
                be.setSelectedRecipe(null);
            }
            return false;
        }

        this.primaryOutputPos = null;

        // [核心修改] 性能优化：引入脏标记检查
        // 只有当 BE内部缓存发生变化（isDirty）或者 上一次运行未能成功配对（需要重试）时，才重新复制
        // 如果机器处于空闲稳定状态且内容无变化，直接跳过扫描
        // [核心修改] 性能优化：引入脏标记检查 -> 由于会导致逻辑卡顿，暂时移除，每一刻都执行检查
        // boolean isDirty = be.isValidAndChanged();
        // AbstractSimulationRecipe runRecipe = be.getRuntimeRecipe();

        List<FoundMaterial> blockPool = copyMaterialList(be.getInternalBlockCache());
        List<FoundMaterial> itemPool = copyMaterialList(be.getInternalEntityCache());

        AbstractSimulationRecipe recipeToRun = targetRecipe;
        if (isGenericClumpMode) {
             recipeToRun = findGenericClumpRecipeFromPool(level, itemPool);
             if (recipeToRun == null) {
                 be.setMatchedRecipe(null, null);
                 return false;
             }
        } else {
             if (!canMatchRecipe(recipeToRun, blockPool, itemPool)) {
                 be.setMatchedRecipe(selectedId, recipeToRun); // 虽然无法运行但 ID 已选
                 return false;
             }
        }

        // 实时更新匹配成功的配方
        AbstractSimulationRecipe matchedRecipeRef = recipeToRun;
        ResourceLocation matchedId = selectedId;
        if (isGenericClumpMode && level != null) {
            var overType = ThermalShockRecipes.OVERHEATING_TYPE.get();
            var shockType = ThermalShockRecipes.THERMAL_SHOCK_TYPE.get();
            matchedId = level.getRecipeManager().getAllRecipesFor(overType).stream()
                        .filter(h -> h.value() == matchedRecipeRef).findFirst().map(net.minecraft.world.item.crafting.RecipeHolder::id)
                        .orElseGet(() -> level.getRecipeManager().getAllRecipesFor(shockType).stream()
                                .filter(h -> h.value() == matchedRecipeRef).findFirst().map(net.minecraft.world.item.crafting.RecipeHolder::id).orElse(null));
        }
        be.setMatchedRecipe(matchedId, recipeToRun);

        if (!checkThermalConditions(recipeToRun)) {
            return false;
        }

        int materialMax = calculateMaxMaterialBatch(recipeToRun, blockPool, itemPool);
        int structureMax = be.performance.getBatchSize();
        int physicalLimit = Math.min(structureMax, Math.min(64, materialMax));
        int actualBatch = calculateExecutionBatch(physicalLimit, recipeToRun);

        if (actualBatch <= 0) return false;

        if (!be.performance.isVirtual()) {
            ItemStack resultStack = recipeToRun.getResultItem(level.registryAccess());
            long projectedTotal = (long) (actualBatch * resultStack.getCount() * be.performance.getYieldMultiplier());
            if (projectedTotal > 1024) return false;
        }

        int successCount = 0;
        int heatToConsume = 0;
        List<ItemStack> rawOutputs = new ArrayList<>();

        // 由于上面计算 batch 时没有实际扣减，这里执行实际扣减
        // 注意：我们需要在一个循环中重新匹配并扣减，确保逻辑严密
        // 因为 blockPool 和 itemPool 是副本，可以随意修改

        // 重置副本状态用于实际消耗
        blockPool = copyMaterialList(be.getInternalBlockCache());
        itemPool = copyMaterialList(be.getInternalEntityCache());

        for (int i = 0; i < actualBatch; i++) {
            MatchResult match = tryMatchIngredient(recipeToRun, blockPool, itemPool);
            if (match == null) break; // 理论上不应发生，因为前面 calculateMax 算过了

            if (be.getMachineMode() == MachineMode.OVERHEATING && recipeToRun instanceof OverheatingRecipe ov) {
                heatToConsume += ov.getHeatCost();
            }

            successCount++;
            ItemStack mainInput = match.consumedInputs.get(0);
            
            // 构建当前物理池快照
            List<ItemStack> physPoolIn = new ArrayList<>();
            List<RecipeSourceType> physPoolTy = new ArrayList<>();
            for (FoundMaterial m : blockPool) { physPoolIn.add(m.stack); physPoolTy.add(RecipeSourceType.BLOCK); }
            for (FoundMaterial m : itemPool) { physPoolIn.add(m.stack); physPoolTy.add(RecipeSourceType.ITEM); }

            rawOutputs.add(recipeToRun.assemble(new SimulationRecipeInput(mainInput, physPoolIn, physPoolTy), level.registryAccess()));

            if (primaryOutputPos == null) capturePrimaryPos(match.foundMaterials);
            // [修复] 移除空方法调用
        }

        if (successCount > 0) {
            triggerMachineEffects(level);
            if (heatToConsume > 0) be.getThermo().consumeHeat(level, heatToConsume);
            be.consumeCatalystBuffer(successCount, be.performance.getEfficiency());

            // 视觉与物理移除：这里会触发 BlockBreak/EntityRemove 事件
            // 这些事件会回调 StructureManager -> BE.markDirty -> 下一 Tick 重建缓存
            consumeMaterialsVisuals(level, blockPool);
            consumeMaterialsVisuals(level, itemPool);

            spawnMergedResults(level, rawOutputs);

            be.onRecipeSuccess();

            // [优化] 同上，由锁定状态决定是否继续循环
            return be.isLocked();
        } else {
            return false;
        }
    }

    // 深拷贝材质列表，用于模拟计算
    private List<FoundMaterial> copyMaterialList(List<FoundMaterial> original) {
        List<FoundMaterial> copy = new ArrayList<>(original.size());
        for (FoundMaterial m : original) {
            // 创建新对象，但 source 和 stack 引用可以共享（stack 不会被修改，只会读取）
            // 注意：FoundMaterial 内部有 remainingCount 和 consumedCount 是可变的
            FoundMaterial newMat = new FoundMaterial(m.source, m.stack);
            newMat.remainingCount = m.remainingCount; // 继承当前状态
            newMat.consumedCount = m.consumedCount;   // [修复] 也复制 consumedCount
            copy.add(newMat);
        }
        return copy;
    }

    // =========================================================
    // 3. 核心算法 (Unified Batch Calculation) - 严格恢复 v17 逻辑
    // =========================================================

    private int calculateExecutionBatch(int limitByStructureAndItems, AbstractSimulationRecipe recipe) {
        // 1. 热冲击模式逻辑：无热量消耗，直接拉满
        // [优化] 热量条件已在 checkThermalConditions 中统一检查，此处只做批处理计算
        if (be.getMachineMode() == MachineMode.THERMAL_SHOCK) {
            return limitByStructureAndItems;
        }

        // 2. 过热模式逻辑：涉及热量消耗与速率逻辑
        if (be.getMachineMode() == MachineMode.OVERHEATING && recipe instanceof OverheatingRecipe ov) {

            int costPerOp = ov.getHeatCost();
            if (costPerOp <= 0) return limitByStructureAndItems;

            int currentHeat = be.getThermo().getCurrentHeat();
            // 计算当前缓存够跑多少次
            int maxByStored = currentHeat / costPerOp;

            // === 分支 A: 红石脉冲 (Rising Edge) ===
            // 逻辑：强制执行。不看速率，只看库存。尽可能多跑。
            if (be.isPulseFrame()) {
                return Math.min(limitByStructureAndItems, maxByStored);
            }

            // === 分支 B: 持续信号 (Continuous) ===
            // 逻辑：基于输入速率 (Input Rate) 的动态平衡
            int inputRate = be.getThermo().getLastInputRate();
            int maxByRate = inputRate / costPerOp;

            if (maxByRate < 1) {
                // [修改] 速率不足 (充能慢) -> 电池放电模式
                // 逻辑：优先消耗库存。如果库存很多，就全速跑；如果库存没了，自动降为0等待。
                // 这比之前的 (maxByStored >= 1 ? 1 : 0) 更智能，允许利用缓存进行批处理。
                return Math.min(limitByStructureAndItems, maxByStored);
            } else {
                // 速率充足 (充能快) -> 流量平衡模式
                // 逻辑：按输入速率跑，保持收支平衡。
                int target = Math.min(limitByStructureAndItems, maxByRate);

                // 3. 溢出保护: 如果缓存 > 95%，无视速率限制，全速泄洪
                if (currentHeat > be.getThermo().getMaxHeatCapacity() * 0.95) {
                    return Math.min(limitByStructureAndItems, maxByStored);
                }
                return target;
            }
        }
        return 0;
    }

    // =========================================================
    // 辅助方法
    // =========================================================

    private boolean checkForMatchingInputButInsufficient(Level level, AbstractSimulationRecipe recipe, List<BlockPos> ports) {
        for (SimulationIngredient simIng : recipe.getSimulationIngredients()) {
            boolean foundAny = false;
            boolean isStrictClumpCheck = (recipe instanceof ClumpProcessingRecipe);
            ItemStack strictTarget = isStrictClumpCheck ? ((ClumpProcessingRecipe) recipe).getTargetContent() : ItemStack.EMPTY;

            for (BlockPos pos : ports) {
                BlockEntity portBe = level.getBlockEntity(pos);
                if (portBe instanceof SimulationPortBlockEntity port) {
                    IItemHandler handler = port.getItemHandler();
                    for (int i = 0; i < handler.getSlots(); i++) {
                        ItemStack stack = handler.getStackInSlot(i);
                        if (!stack.isEmpty() && simIng.ingredient().test(stack)) {
                            if (isStrictClumpCheck) {
                                ClumpInfo info = stack.get(ThermalShockDataComponents.TARGET_OUTPUT);
                                if (info == null || !ItemStack.isSameItemSameComponents(info.result(), strictTarget))
                                    continue;
                            }
                            foundAny = true;
                            break;
                        }
                    }
                }
                if (foundAny) break;
            }
            if (foundAny) return true;
        }
        return false;
    }

    private AbstractSimulationRecipe resolveRecipeById(Level level, ResourceLocation id) {
        if (id == null || GENERIC_CLUMP_ID.equals(id)) return null;
        Optional<RecipeHolder<?>> opt = level.getRecipeManager().byKey(id);
        if (opt.isPresent() && opt.get().value() instanceof AbstractSimulationRecipe r) {
            if (r.getMachineMode() == be.getMachineMode()) return r;
        }
        return null;
    }

    private AbstractSimulationRecipe findMatchingClumpRecipe(Level level, ItemStack stack) {
        if (!stack.is(ThermalShockItems.MATERIAL_CLUMP.get())) return null;
        ClumpInfo info = stack.get(ThermalShockDataComponents.TARGET_OUTPUT);
        if (info == null || info.result().isEmpty()) return null;

        var all = level.getRecipeManager().getAllRecipesFor(ThermalShockRecipes.OVERHEATING_TYPE.get());
        for (RecipeHolder<?> h : all) {
            if (h.value() instanceof ClumpProcessingRecipe cr) {
                if (ItemStack.isSameItemSameComponents(cr.getTargetContent(), info.result())) {
                    return cr;
                }
            }
        }
        return null;
    }

    private AbstractSimulationRecipe findFirstValidGenericRecipe(Level level, List<BlockPos> ports) {
        for (BlockPos pos : ports) {
            BlockEntity portBe = level.getBlockEntity(pos);
            if (portBe instanceof SimulationPortBlockEntity port) {
                IItemHandler handler = port.getItemHandler();
                for (int i = 0; i < handler.getSlots(); i++) {
                    ItemStack stack = handler.getStackInSlot(i);
                    if (stack.isEmpty()) continue;
                    AbstractSimulationRecipe r = findMatchingClumpRecipe(level, stack);
                    if (r != null) return r;
                }
            }
        }
        return null;
    }

    private AbstractSimulationRecipe findGenericClumpRecipeFromPool(Level level, List<FoundMaterial> itemPool) {
        for (FoundMaterial mat : itemPool) {
            AbstractSimulationRecipe r = findMatchingClumpRecipe(level, mat.stack);
            if (r != null) return r;
        }
        return null;
    }

    private int calculateMaxBatchFromPorts(Level level, AbstractSimulationRecipe recipe, List<BlockPos> ports) {
        int minBatch = Integer.MAX_VALUE;
        for (SimulationIngredient simIng : recipe.getSimulationIngredients()) {
            int totalFound = 0;
            boolean isStrictClumpCheck = (recipe instanceof ClumpProcessingRecipe);
            ItemStack strictTarget = isStrictClumpCheck ? ((ClumpProcessingRecipe) recipe).getTargetContent() : ItemStack.EMPTY;

            for (BlockPos pos : ports) {
                BlockEntity portBe = level.getBlockEntity(pos);
                if (portBe instanceof SimulationPortBlockEntity port) {
                    IItemHandler handler = port.getItemHandler();
                    for (int i = 0; i < handler.getSlots(); i++) {
                        ItemStack stack = handler.getStackInSlot(i);
                        if (simIng.ingredient().test(stack)) {
                            if (isStrictClumpCheck) {
                                ClumpInfo info = stack.get(ThermalShockDataComponents.TARGET_OUTPUT);
                                if (info == null || !ItemStack.isSameItemSameComponents(info.result(), strictTarget))
                                    continue;
                            }
                            totalFound += stack.getCount();
                        }
                    }
                }
            }
            minBatch = Math.min(minBatch, totalFound);
            if (minBatch == 0) return 0;
        }
        return minBatch == Integer.MAX_VALUE ? 0 : minBatch;
    }

    private int calculateAvailableOutputSpace(Level level, ItemStack outputTemplate, List<BlockPos> ports) {
        int totalSpace = 0;
        int maxStackSize = outputTemplate.getMaxStackSize();
        for (BlockPos pos : ports) {
            BlockEntity portBe = level.getBlockEntity(pos);
            if (portBe instanceof SimulationPortBlockEntity port) {
                IItemHandler handler = port.getItemHandler();
                for (int i = 0; i < handler.getSlots(); i++) {
                    ItemStack existing = handler.getStackInSlot(i);
                    if (existing.isEmpty()) {
                        totalSpace += maxStackSize;
                    } else if (ItemStack.isSameItemSameComponents(existing, outputTemplate)) {
                        int spaceInSlot = Math.max(0, maxStackSize - existing.getCount());
                        totalSpace += spaceInSlot;
                    }
                }
            }
            if (totalSpace > 1000000) break;
        }
        return totalSpace;
    }

    private void consumeIngredientsFromPorts(Level level, AbstractSimulationRecipe recipe, int batchToConsume, List<BlockPos> ports) {
        for (SimulationIngredient simIng : recipe.getSimulationIngredients()) {
            int remainingToConsume = batchToConsume;
            boolean isStrictClumpCheck = (recipe instanceof ClumpProcessingRecipe);
            ItemStack strictTarget = isStrictClumpCheck ? ((ClumpProcessingRecipe) recipe).getTargetContent() : ItemStack.EMPTY;

            for (BlockPos pos : ports) {
                if (remainingToConsume <= 0) break;
                BlockEntity portBe = level.getBlockEntity(pos);
                if (portBe instanceof SimulationPortBlockEntity port) {
                    IItemHandler handler = port.getItemHandler();
                    for (int i = 0; i < handler.getSlots(); i++) {
                        if (remainingToConsume <= 0) break;
                        ItemStack stack = handler.getStackInSlot(i);
                        if (simIng.ingredient().test(stack)) {
                            if (isStrictClumpCheck) {
                                ClumpInfo info = stack.get(ThermalShockDataComponents.TARGET_OUTPUT);
                                if (info == null || !ItemStack.isSameItemSameComponents(info.result(), strictTarget))
                                    continue;
                            }
                            int extractCount = Math.min(stack.getCount(), remainingToConsume);
                            handler.extractItem(i, extractCount, false);
                            remainingToConsume -= extractCount;
                        }
                    }
                }
            }
        }
    }

    private void distributeOutput(Level level, ItemStack stack, List<BlockPos> ports) {
        ItemStack remaining = stack.copy();
        for (BlockPos pos : ports) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof SimulationPortBlockEntity port) {
                remaining = ItemHandlerHelper.insertItemStacked(port.getItemHandler(), remaining, false);
                if (remaining.isEmpty()) return;
            }
        }
    }

    private int calculateMaxMaterialBatch(AbstractSimulationRecipe recipe, List<FoundMaterial> blockPool, List<FoundMaterial> itemPool) {
        List<Integer> blockCounts = new ArrayList<>();
        for (FoundMaterial m : blockPool) blockCounts.add(m.remainingCount);
        List<Integer> itemCounts = new ArrayList<>();
        for (FoundMaterial m : itemPool) itemCounts.add(m.remainingCount);

        int maxPossible = 0;
        int limit = 64;

        for (int i = 0; i < limit; i++) {
            boolean matched = true;
            for (var ingredient : recipe.getSimulationIngredients()) {
                boolean foundIngredient = false;
                List<FoundMaterial> pool = (ingredient.type() == RecipeSourceType.BLOCK) ? blockPool : itemPool;
                List<Integer> counts = (ingredient.type() == RecipeSourceType.BLOCK) ? blockCounts : itemCounts;

                for (int j = 0; j < pool.size(); j++) {
                    if (counts.get(j) > 0 && ingredient.ingredient().test(pool.get(j).stack)) {
                        counts.set(j, counts.get(j) - 1);
                        foundIngredient = true;
                        break;
                    }
                }
                if (!foundIngredient) {
                    matched = false;
                    break;
                }
            }
            if (matched) maxPossible++;
            else break;
        }
        return maxPossible;
    }

    private boolean canMatchRecipe(AbstractSimulationRecipe recipe, List<FoundMaterial> blockPool, List<FoundMaterial> itemPool) {
        if (recipe.getSimulationIngredients().isEmpty()) return false;
        var firstReq = recipe.getSimulationIngredients().get(0);
        List<FoundMaterial> pool = (firstReq.type() == RecipeSourceType.BLOCK) ? blockPool : itemPool;
        for (FoundMaterial m : pool) {
            if (firstReq.ingredient().test(m.stack)) return true;
        }
        return false;
    }

    private MatchResult tryMatchIngredient(AbstractSimulationRecipe recipe, List<FoundMaterial> blockPool, List<FoundMaterial> itemPool) {
        MatchResult result = new MatchResult();
        for (var ingredient : recipe.getSimulationIngredients()) {
            FoundMaterial found = null;
            List<FoundMaterial> pool = (ingredient.type() == RecipeSourceType.BLOCK) ? blockPool : itemPool;
            for (FoundMaterial mat : pool) {
                if (mat.remainingCount > 0 && ingredient.ingredient().test(mat.stack)) {
                    found = mat;
                    break;
                }
            }
            if (found != null) {
                found.remainingCount--;
                found.consumedCount++;
                result.foundMaterials.add(found);
                result.consumedInputs.add(found.stack);
            } else {
                result.rollback();
                return null;
            }
        }
        return result;
    }

    private void capturePrimaryPos(List<FoundMaterial> mats) {
        for (FoundMaterial m : mats) {
            if (m.source instanceof BlockPos p) {
                primaryOutputPos = Vec3.atCenterOf(p);
                return;
            }
        }
        if (!mats.isEmpty() && mats.get(0).source instanceof ItemEntity ie) {
            primaryOutputPos = ie.position();
        }
    }

    private void consumeMaterialsVisuals(Level level, List<FoundMaterial> materials) {
        if (!(level instanceof ServerLevel sl)) return;
        for (FoundMaterial mat : materials) {
            if (mat.consumedCount <= 0) continue;
            Vec3 fxPos = null;
            if (mat.source instanceof BlockPos p) {
                fxPos = Vec3.atCenterOf(p);
                level.removeBlock(p, false);
            } else if (mat.source instanceof ItemEntity ie) {
                fxPos = ie.position();
                ItemStack s = ie.getItem();
                s.shrink(mat.consumedCount);
                ie.setItem(s);
                if (s.isEmpty()) ie.discard();
            }
            if (fxPos != null) {
                if (be.getMachineMode() == MachineMode.OVERHEATING) {
                    sl.sendParticles(ParticleTypes.FLAME, fxPos.x, fxPos.y, fxPos.z, 5, 0.2, 0.2, 0.2, 0.05);
                } else {
                    sl.sendParticles(ParticleTypes.EXPLOSION, fxPos.x, fxPos.y, fxPos.z, 1, 0.0, 0.0, 0.0, 0.0);
                }
            }
            mat.consumedCount = 0;
        }
    }

    private void spawnMergedResults(Level level, List<ItemStack> rawResults) {
        if (level == null) return;
        List<ItemStack> mergedList = new ArrayList<>();
        for (ItemStack raw : rawResults) {
            if (raw.isEmpty()) continue;
            boolean merged = false;
            for (ItemStack existing : mergedList) {
                if (ItemStack.isSameItemSameComponents(existing, raw) && existing.getCount() + raw.getCount() <= existing.getMaxStackSize()) {
                    existing.grow(raw.getCount());
                    merged = true;
                    break;
                }
            }
            if (!merged) mergedList.add(raw.copy());
        }

        Vec3 spawnPos;
        if (this.primaryOutputPos != null) {
            spawnPos = this.primaryOutputPos;
        } else {
            BlockPos min = be.getStructure().getMinPos();
            BlockPos max = be.getStructure().getMaxPos();
            spawnPos = new Vec3((min.getX() + max.getX()) / 2.0 + 0.5, (min.getY() + max.getY()) / 2.0 + 0.5, (min.getZ() + max.getZ()) / 2.0 + 0.5);
        }

        for (ItemStack stack : mergedList) {
            float yieldMult = be.performance.getYieldMultiplier();
            float efficiency = be.performance.getEfficiency();
            int baseCount = stack.getCount();

            float catalystBonus = be.calculateCatalystBonus(efficiency);
            float totalRate = yieldMult * (1.0f + catalystBonus);
            float totalAdded = baseCount * totalRate;

            be.addAccumulatedYield(totalAdded - baseCount);
            int extra = be.popAccumulatedYield();
            stack.grow(extra);

            if (level instanceof ServerLevel sl) {
                if (be.getMachineMode() == MachineMode.OVERHEATING) {
                    sl.sendParticles(ParticleTypes.FLAME, spawnPos.x, spawnPos.y, spawnPos.z, 20, 0.2, 0.2, 0.2, 0.1);
                    sl.sendParticles(ParticleTypes.POOF, spawnPos.x, spawnPos.y, spawnPos.z, 5, 0.3, 0.3, 0.3, 0.05);
                } else {
                    sl.sendParticles(ParticleTypes.EXPLOSION, spawnPos.x, spawnPos.y, spawnPos.z, 1, 0.0, 0.0, 0.0, 0.0);
                    sl.sendParticles(ParticleTypes.CLOUD, spawnPos.x, spawnPos.y, spawnPos.z, 8, 0.3, 0.3, 0.3, 0.1);
                }
            }

            while (!stack.isEmpty()) {
                int splitSize = Math.min(stack.getCount(), stack.getMaxStackSize());
                ItemStack drop = stack.split(splitSize);
                ItemEntity ie = new ItemEntity(level, spawnPos.x, spawnPos.y, spawnPos.z, drop);
                double spread = 0.15;
                double speedMult = (be.getMachineMode() == MachineMode.THERMAL_SHOCK) ? 1.5 : 1.0;
                ie.setDeltaMovement((level.random.nextDouble() - 0.5) * spread * speedMult, ((level.random.nextDouble() - 0.5) * spread + 0.2) * speedMult, (level.random.nextDouble() - 0.5) * spread * speedMult);
                ie.setPickUpDelay(10);
                level.addFreshEntity(ie);
            }
        }
    }

    private void triggerMachineEffects(Level level) {
        be.setLitState(true);
        BlockPos min = be.getStructure().getMinPos();
        BlockPos max = be.getStructure().getMaxPos();
        double cx = (min.getX() + max.getX()) / 2.0 + 0.5;
        double cy = (min.getY() + max.getY()) / 2.0 + 0.5;
        double cz = (min.getZ() + max.getZ()) / 2.0 + 0.5;

        if (be.getMachineMode() == MachineMode.OVERHEATING) {
            float pitch = 0.8f + level.random.nextFloat() * 0.4f;
            level.playSound(null, cx, cy, cz, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 5.0f / 16.0f, pitch);
        } else {
            float pitch = 0.9f + level.random.nextFloat() * 0.2f;
            level.playSound(null, cx, cy, cz, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, (5.0f / 16.0f) * 0.5f, pitch);
        }

        if (level instanceof ServerLevel sl) {
            for (BlockPos v : be.getStructure().getVents()) {
                Vec3 center = new Vec3(cx, cy, cz);
                Vec3 ventPos = Vec3.atCenterOf(v);
                Vec3 dir = ventPos.subtract(center).normalize();

                if (be.getMachineMode() == MachineMode.OVERHEATING) {
                    sl.sendParticles(ParticleTypes.FLAME, ventPos.x + dir.x * 0.6, ventPos.y + dir.y * 0.6, ventPos.z + dir.z * 0.6, 10, 0.2, 0.2, 0.2, 0.1);
                    sl.sendParticles(ParticleTypes.LARGE_SMOKE, ventPos.x + dir.x * 0.6, ventPos.y + dir.y * 0.6, ventPos.z + dir.z * 0.6, 5, 0.15, 0.15, 0.15, 0.1);
                } else {
                    sl.sendParticles(ParticleTypes.CLOUD, ventPos.x + dir.x * 0.6, ventPos.y + dir.y * 0.6, ventPos.z + dir.z * 0.6, 6, 0.1, 0.1, 0.1, 0.25);
                    sl.sendParticles(ParticleTypes.POOF, ventPos.x + dir.x * 0.6, ventPos.y + dir.y * 0.6, ventPos.z + dir.z * 0.6, 3, 0.2, 0.2, 0.2, 0.05);
                }
            }
        }
    }

    /**
     * 检查当前机器的热力状态是否满足配方需求
     * 修复了之前 idea 找不到符号的问题
     */
    private boolean checkThermalConditions(AbstractSimulationRecipe recipe) {
        // 1. 过热模式判定
        if (be.getMachineMode() == MachineMode.OVERHEATING) {
            if (!(recipe instanceof OverheatingRecipe ov)) return false;
            // [修复] 添加 minHeatRate 检查 - 热输入速率必须满足配方要求
            return be.getThermo().getLastInputRate() >= ov.getMinHeatRate();
        }
        // 2. 热冲击模式判定
        else {
            if (!(recipe instanceof ThermalShockRecipe ts)) return false;

            // 必须满足：High > MinHot, Low < MaxCold, Delta > ReqDelta
            // 注意：be.getThermo().getCurrentHighTemp() 是正数
            // be.getThermo().getCurrentLowTemp() 是负数 (e.g. -20)

            boolean highOk = be.getThermo().getCurrentHighTemp() >= ts.getMinHotTemp();
            boolean lowOk = be.getThermo().getCurrentLowTemp() <= ts.getMaxColdTemp(); // e.g. -50 < -20
            boolean deltaOk = be.getThermo().getDeltaT() >= ts.getRequiredDelta();

            return highOk && lowOk && deltaOk;
        }
    }

    private static class MatchResult {
        final List<ItemStack> consumedInputs = new ArrayList<>();
        final List<FoundMaterial> foundMaterials = new ArrayList<>();

        void rollback() {
            for (FoundMaterial mat : foundMaterials) {
                mat.remainingCount++;
                mat.consumedCount--;
            }
        }
    }

    public static class FoundMaterial {
        final Object source;
        final ItemStack stack;
        int remainingCount;
        int consumedCount;

        FoundMaterial(Object s, ItemStack st) {
            source = s;
            stack = st;
            remainingCount = st.getCount();
            consumedCount = 0;
        }
    }
}