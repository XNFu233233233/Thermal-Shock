package com.xnfu.thermalshock.client.gui;

import com.xnfu.thermalshock.block.entity.ThermalConverterBlockEntity;
import com.xnfu.thermalshock.registries.ThermalShockMenus;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;

public class ThermalConverterMenu extends AbstractContainerMenu {

    private final ThermalConverterBlockEntity be;
    private final ContainerData data;

    // 两个流体的同步数据 (ID, Amount, Capacity) x 2 = 6 ints
    private final ContainerData fluidData = new SimpleContainerData(6) {
        @Override public int get(int index) {
            int tankIdx = index / 3;
            int type = index % 3;
            var tank = tankIdx == 0 ? be.getInputTank() : be.getOutputTank();
            return switch (type) {
                case 0 -> BuiltInRegistries.FLUID.getId(tank.getFluid().getFluid());
                case 1 -> tank.getFluidAmount();
                case 2 -> tank.getCapacity();
                default -> 0;
            };
        }
        @Override public void set(int index, int value) {}
    };

    public ThermalConverterMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, (ThermalConverterBlockEntity) inv.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(3));
    }

    public ThermalConverterMenu(int containerId, Inventory inv, ThermalConverterBlockEntity entity, ContainerData data) {
        super(ThermalShockMenus.THERMAL_CONVERTER_MENU.get(), containerId);
        this.be = entity;
        this.data = data;

        checkContainerDataCount(data, 3);
        addDataSlots(data);
        addDataSlots(fluidData);

        // Slots
        // 输入 (Slot 0)
        this.addSlot(new SlotItemHandler(be.getItemHandler(), 0, 44, 35));
        // 输出主 (Slot 1)
        this.addSlot(new SlotItemHandler(be.getItemHandler(), 1, 116, 35));
        // 输出废料 (Slot 2)
        this.addSlot(new SlotItemHandler(be.getItemHandler(), 2, 142, 35));

        addPlayerInventory(new InvWrapper(inv));
    }

    private void addPlayerInventory(net.neoforged.neoforge.items.IItemHandler playerInventory) {
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new SlotItemHandler(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new SlotItemHandler(playerInventory, col, 8 + col * 18, 142));
        }
    }

    public int getProcessTime() { return data.get(0); }
    public int getTotalProcessTime() { return data.get(1); }
    public int getCurrentHeat() { return data.get(2); }

    // 流体数据获取
    public int getFluidId(int tank) { return fluidData.get(tank * 3); }
    public int getFluidAmount(int tank) { return fluidData.get(tank * 3 + 1); }
    public int getFluidCapacity(int tank) { return fluidData.get(tank * 3 + 2); }

    @Override public boolean stillValid(Player player) { return ContainerLevelAccess.create(be.getLevel(), be.getBlockPos()).evaluate((l, p) -> player.distanceToSqr(p.getCenter()) <= 64.0, true); }
    @Override public ItemStack quickMoveStack(Player p, int i) { return ItemStack.EMPTY; } // 简化略
}