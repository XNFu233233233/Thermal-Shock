package com.xnfu.thermalshock.block;

import com.mojang.serialization.MapCodec;
import com.xnfu.thermalshock.block.entity.SimulationChamberBlockEntity;
import com.xnfu.thermalshock.registries.ThermalShockBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

public class SimulationChamberBlock extends BaseEntityBlock {
    public static final MapCodec<SimulationChamberBlock> CODEC = simpleCodec(SimulationChamberBlock::new);

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public SimulationChamberBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, net.minecraft.core.Direction.NORTH)
                .setValue(LIT, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof SimulationChamberBlockEntity chamber) {
                // 放置时立即进行一次验证
                chamber.performValidation(placer instanceof Player player ? player : null);
                chamber.updatePoweredState(level.hasNeighborSignal(pos));
            }
        }
    }

    // [Event-Driven Core] 邻居方块改变时触发
    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof SimulationChamberBlockEntity chamber) {
                // 1. 更新红石状态 (可能触发 process)
                chamber.updatePoweredState(level.hasNeighborSignal(pos));

                // 2. 响应环境变化 (热源数值波动/方块更替)
                chamber.onEnvironmentUpdate(neighborPos, false);
            }
        }
    }

    // [Event-Driven Scheduling] 接收 scheduleTick 的回调
    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof SimulationChamberBlockEntity chamber) {
            chamber.onScheduledTick();
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof SimulationChamberBlockEntity chamber) {
                IItemHandler itemHandler = chamber.getItemHandler();
                SimpleContainer tempContainer = new SimpleContainer(itemHandler.getSlots());
                for (int i = 0; i < itemHandler.getSlots(); i++) {
                    tempContainer.setItem(i, itemHandler.getStackInSlot(i));
                }
                Containers.dropContents(level, pos, tempContainer);
                
                // 确保结构逻辑被拆除
                chamber.notifyStructureBroken();
                
                level.updateNeighbourForOutputSignal(pos, this);
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof SimulationChamberBlockEntity chamber) {
                if (player.isShiftKeyDown()) {
                    // 强制手动验证 (玩家在 Shift+右键时最希望看到最新的结构状态)
                    chamber.performValidation(player);
                } else {
                    chamber.performLazyValidation(); // 打开 GUI 前进行一次惰性检查
                    player.openMenu(chamber, pos);
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SimulationChamberBlockEntity(pos, state);
    }
}
