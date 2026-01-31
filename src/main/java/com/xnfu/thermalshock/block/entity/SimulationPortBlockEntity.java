// [模式 A：全量重写]
package com.xnfu.thermalshock.block.entity;

import com.xnfu.thermalshock.client.gui.SimulationPortMenu;
import com.xnfu.thermalshock.registries.ThermalShockBlockEntities;
import com.xnfu.thermalshock.util.StructureManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.RangedWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SimulationPortBlockEntity extends BlockEntity implements MenuProvider {

    private PortMode portMode = PortMode.NONE; // 默认为 NONE (全通)
    private BlockPos controllerPos = null;
    private BlockState camouflageState = Blocks.AIR.defaultBlockState();

    // 内部存储：标准 27 格，64 堆叠
    private final ItemStackHandler itemHandler = new ItemStackHandler(27) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            SimulationPortBlockEntity.this.notifyController();
        }
    };

    // 内部流体：3 个独立的 64B (64000mB) 储罐
    private final MultiFluidTank fluidHandler = new MultiFluidTank(3, 64000);

    // [Fix] Capability Cache
    private final IItemHandler itemHandlerCap;
    private final IFluidHandler fluidHandlerCap;

    // 数据同步 (GUI 用)
    private final ContainerData data = new SimpleContainerData(9) {
        @Override
        public int get(int index) {
            int tankIdx = index / 3;
            int type = index % 3;
            if (tankIdx >= 3) return 0;
            var tank = fluidHandler.getTank(tankIdx);
            return switch (type) {
                case 0 -> BuiltInRegistries.FLUID.getId(tank.getFluid().getFluid());
                case 1 -> tank.getFluidAmount();
                case 2 -> tank.getCapacity();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }
    };

    public SimulationPortBlockEntity(BlockPos pos, BlockState blockState) {
        super(ThermalShockBlockEntities.SIMULATION_PORT_BE.get(), pos, blockState);
        this.itemHandlerCap = createItemHandlerCap();
        this.fluidHandlerCap = createFluidHandlerCap();
    }

    // =========================================================
    // 核心逻辑：能力暴露 (Capability Wrappers)
    // =========================================================

    private IItemHandler createItemHandlerCap() {
        return new RangedWrapper(itemHandler, 0, itemHandler.getSlots()) {
            @Override
            public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
                // 允许存入的模式：NONE, INPUT, CATALYST
                if (portMode == PortMode.OUTPUT) {
                    return stack; // 拒绝插入
                }
                return super.insertItem(slot, stack, simulate);
            }

            @Override
            public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
                // 允许取出的模式：NONE, OUTPUT, CATALYST
                if (portMode == PortMode.INPUT) {
                    return ItemStack.EMPTY; // 拒绝提取
                }
                return super.extractItem(slot, amount, simulate);
            }

            @Override
            public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                if (portMode == PortMode.OUTPUT) return false;
                return super.isItemValid(slot, stack);
            }
        };
    }

    private IFluidHandler createFluidHandlerCap() {
        return new IFluidHandler() {
            @Override
            public int getTanks() {
                return fluidHandler.getTanks();
            }

            @Override
            public @NotNull FluidStack getFluidInTank(int tank) {
                return fluidHandler.getFluidInTank(tank);
            }

            @Override
            public int getTankCapacity(int tank) {
                return fluidHandler.getTankCapacity(tank);
            }

            @Override
            public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
                return fluidHandler.isFluidValid(tank, stack);
            }

            @Override
            public int fill(FluidStack resource, FluidAction action) {
                // 允许存入：NONE, INPUT, CATALYST
                if (portMode == PortMode.OUTPUT) return 0;
                return fluidHandler.fill(resource, action);
            }

            @Override
            public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
                // 允许取出：NONE, OUTPUT, CATALYST
                if (portMode == PortMode.INPUT) return FluidStack.EMPTY;
                return fluidHandler.drain(resource, action);
            }

            @Override
            public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
                if (portMode == PortMode.INPUT) return FluidStack.EMPTY;
                return fluidHandler.drain(maxDrain, action);
            }
        };
    }

    public IItemHandler getCapabilityItemHandler() {
        return this.itemHandlerCap;
    }

    public IFluidHandler getCapabilityFluidHandler() {
        return this.fluidHandlerCap;
    }

    public void propagateUpdateToController(BlockPos updateSource) {
        if (this.controllerPos != null && level != null && !level.isClientSide) {
            // 只有当变动源不是控制器自己时才通知 (防止死循环)
            if (!updateSource.equals(this.controllerPos)) {
                if (level.isLoaded(this.controllerPos) && level.getBlockEntity(this.controllerPos) instanceof SimulationChamberBlockEntity controller) {
                    controller.onEnvironmentUpdate(updateSource, StructureManager.UpdateType.PLACE);
                }
            }
        }
    }

    // =========================================================
    // Getters / Setters & Utility
    // =========================================================

    public IItemHandler getItemHandler() {
        return itemHandler;
    }

    public MultiFluidTank getFluidHandler() {
        return fluidHandler;
    }

    public PortMode getPortMode() {
        return portMode;
    }

    public BlockState getCamouflageState() {
        return camouflageState;
    }

    public void setCamouflageState(BlockState state) {
        if (this.camouflageState != state) {
            this.camouflageState = state;
            setChanged();
            if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public void setPortMode(PortMode mode) {
        this.portMode = mode;
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    public void bindController(BlockPos pos) {
        this.controllerPos = pos;
        setChanged();
    }

    public void unbindController() {
        this.controllerPos = null;
        setChanged();
    }

    public void cyclePortMode() {
        setPortMode(getPortMode().next());
        if (this.controllerPos != null && level != null) {
            if (level.getBlockEntity(this.controllerPos) instanceof SimulationChamberBlockEntity controller) {
                controller.markPortsDirty();
            }
        }
    }

    private void notifyController() {
        if (this.controllerPos != null && level != null && !level.isClientSide) {
            if (level.getBlockEntity(this.controllerPos) instanceof SimulationChamberBlockEntity controller) {
                controller.markEntityCacheDirty();
                if (this.portMode == PortMode.CATALYST) {
                    controller.markCatalystDirty();
                }
            }
        }
    }

    public void notifyControllerOnBreak() {
        if (level != null && !level.isClientSide && controllerPos != null) {
            if (level.isLoaded(controllerPos) && level.getBlockEntity(controllerPos) instanceof SimulationChamberBlockEntity controller) {
                controller.notifyStructureBroken();
            }
        }
    }

    // =========================================================
    // NBT & Network
    // =========================================================

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", itemHandler.serializeNBT(registries));
        tag.put("Fluids", fluidHandler.serializeNBT(registries));
        tag.putString("Mode", portMode.name());
        if (camouflageState != null) tag.put("Camouflage", NbtUtils.writeBlockState(camouflageState));
        if (controllerPos != null) tag.putLong("ControllerPos", controllerPos.asLong());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        itemHandler.deserializeNBT(registries, tag.getCompound("Inventory"));
        fluidHandler.deserializeNBT(registries, tag.getCompound("Fluids"));
        if (tag.contains("Mode")) portMode = PortMode.valueOf(tag.getString("Mode"));
        if (tag.contains("Camouflage"))
            this.camouflageState = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), tag.getCompound("Camouflage"));
        if (tag.contains("ControllerPos")) controllerPos = BlockPos.of(tag.getLong("ControllerPos"));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
        loadAdditional(pkt.getTag(), registries);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.thermalshock.simulation_chamber_port");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
        return new SimulationPortMenu(i, inventory, this, this.data);
    }

    // =========================================================
    // Inner Class: MultiFluidTank
    // =========================================================
    public class MultiFluidTank implements IFluidHandler {
        public final FluidTank[] tanks;

        public MultiFluidTank(int count, int capacity) {
            this.tanks = new FluidTank[count];
            for (int i = 0; i < count; i++) {
                this.tanks[i] = new FluidTank(capacity) {
                    @Override
                    protected void onContentsChanged() {
                        setChanged();
                        // 任何流体变动也通知控制器，以便未来扩展液体配方
                        SimulationPortBlockEntity.this.notifyController();
                    }
                };
            }
        }

        public FluidTank getTank(int index) {
            return tanks[index];
        }

        @Override
        public int getTanks() {
            return tanks.length;
        }

        @Override
        public @NotNull FluidStack getFluidInTank(int tank) {
            return tanks[tank].getFluid();
        }

        @Override
        public int getTankCapacity(int tank) {
            return tanks[tank].getCapacity();
        }

        @Override
        public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
            return tanks[tank].isFluidValid(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            for (var tank : tanks) {
                if (tank.isFluidValid(resource)) {
                    int filled = tank.fill(resource, action);
                    if (filled > 0) return filled;
                }
            }
            return 0;
        }

        @Override
        public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
            for (var tank : tanks) {
                if (tank.getFluid().is(resource.getFluid())) {
                    var drained = tank.drain(resource, action);
                    if (!drained.isEmpty()) return drained;
                }
            }
            return FluidStack.EMPTY;
        }

        @Override
        public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
            for (var tank : tanks) {
                var drained = tank.drain(maxDrain, action);
                if (!drained.isEmpty()) return drained;
            }
            return FluidStack.EMPTY;
        }

        public CompoundTag serializeNBT(HolderLookup.Provider r) {
            CompoundTag nbt = new CompoundTag();
            for (int i = 0; i < tanks.length; i++) nbt.put("Tank" + i, tanks[i].writeToNBT(r, new CompoundTag()));
            return nbt;
        }

        public void deserializeNBT(HolderLookup.Provider r, CompoundTag nbt) {
            for (int i = 0; i < tanks.length; i++)
                if (nbt.contains("Tank" + i)) tanks[i].readFromNBT(r, nbt.getCompound("Tank" + i));
        }
    }
}