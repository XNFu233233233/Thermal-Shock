// [模式 A：全量重写]
package com.xnfu.thermalshock.block;

import com.xnfu.thermalshock.block.entity.SimulationPortBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

public class SimulationChamberPortBlock extends Block implements EntityBlock {

    public SimulationChamberPortBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SimulationPortBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof SimulationPortBlockEntity port) {

                // 1. 优先处理流体交互 (桶/通用流体容器)
                // FluidUtil 会自动查找玩家手里的物品是否有流体能力，并尝试与方块的流体能力交互
                if (FluidUtil.interactWithFluidHandler(player, InteractionHand.MAIN_HAND, level, pos, hitResult.getDirection())) {
                    return InteractionResult.SUCCESS;
                }

                // 2. Shift+右键：切换模式
                if (player.isShiftKeyDown()) {
                    port.cyclePortMode();
                    player.displayClientMessage(Component.translatable("message.thermalshock.port_mode", port.getPortMode().getSerializedName()), true);
                }
                // 3. 普通右键：打开 GUI
                else {
                    player.openMenu(port, pos);
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof SimulationPortBlockEntity port) {
                // 将邻居更新事件转发给绑定的控制器
                // 这样无论热源是贴在接口上，还是贴在控制器上，控制器都能收到通知
                port.propagateUpdateToController(neighborPos);
            }
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof SimulationPortBlockEntity port) {
                // 1. 通知控制器结构失效
                port.notifyControllerOnBreak();

                // 2. 掉落内部物品
                IItemHandler handler = port.getItemHandler();
                for (int i = 0; i < handler.getSlots(); i++) {
                    ItemStack stack = handler.getStackInSlot(i);
                    if (!stack.isEmpty()) {
                        net.minecraft.world.Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
                    }
                }
                // 流体无需掉落，随 BE 销毁消失
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }
}