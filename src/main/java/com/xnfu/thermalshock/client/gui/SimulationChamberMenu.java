package com.xnfu.thermalshock.client.gui;

import com.xnfu.thermalshock.block.entity.SimulationChamberBlockEntity;
import com.xnfu.thermalshock.registries.ThermalShockMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;

public class SimulationChamberMenu extends AbstractContainerMenu {
    public static final int GUI_WIDTH = 178 ;
    public static final int GUI_HEIGHT = 202;

    public static final int INV_X = 8;
    public static final int INV_Y = 120;
    public static final int HOTBAR_X = 8;
    public static final int HOTBAR_Y = 178;

    public static final int LIST_LAYOUT_X = 8;
    public static final int LIST_LAYOUT_Y = 42;
    public static final int LIST_LAYOUT_W = 82;
    public static final int LIST_LAYOUT_H = 74;

    public static final int HEAT_BAR_X = 92;
    public static final int SLOT_CATALYST_X = 104;
    public static final int SLOT_CATALYST_Y = 98;
    public static final int SLOT_UPGRADE_X = 134;
    public static final int SLOT_UPGRADE_Y = 98;

    private final SimulationChamberBlockEntity blockEntity;
    private final ContainerData data;

    public SimulationChamberMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, (SimulationChamberBlockEntity) inv.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(16));
    }

    public SimulationChamberMenu(int containerId, Inventory inv, SimulationChamberBlockEntity entity, ContainerData data) {
        super(ThermalShockMenus.SIMULATION_CHAMBER_MENU.get(), containerId);
        this.blockEntity = entity;
        this.data = data;

        checkContainerDataCount(data, 16);
        this.addDataSlots(data);

        IItemHandler itemHandler = this.blockEntity.getItemHandler();

        // Index 0: 催化剂槽 (Strict Check)
        this.addSlot(new SlotItemHandler(itemHandler, 0, SLOT_CATALYST_X + 1, SLOT_CATALYST_Y + 1) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                // 双重校验：调用 BE 的 check，同时防止 null
                return blockEntity.getItemHandler().isItemValid(0, stack);
            }
        });

        // Index 1: 模拟升级槽 (Strict Check)
        this.addSlot(new SlotItemHandler(itemHandler, 1, SLOT_UPGRADE_X + 1, SLOT_UPGRADE_Y + 1) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return blockEntity.getItemHandler().isItemValid(1, stack);
            }
        });

        addPlayerInventory(new InvWrapper(inv));
    }

    private void addPlayerInventory(IItemHandler playerInventory) {
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new SlotItemHandler(playerInventory, col + row * 9 + 9, INV_X + col * 18 + 1, INV_Y + row * 18 + 1));
            }
        }
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new SlotItemHandler(playerInventory, col, HOTBAR_X + col * 18 + 1, HOTBAR_Y + 1));
        }
    }

    public SimulationChamberBlockEntity getBlockEntity() { return this.blockEntity; }

    public int getHeatStored() { return this.data.get(0); }
    public int getMaxHeat() { return this.data.get(1); }
    public int getMinTemp() { return this.data.get(2); }
    public int getMaxTemp() { return this.data.get(3); }
    public float getEfficiency() { return this.data.get(4) / 100.0f; }
    public float getBonusYield() { return this.data.get(5) / 100.0f; }
    public float getCatalystAmount() { return this.data.get(6) / 10.0f; }
    public int getHeatIoRate() { return this.data.get(7); }
    public int getStructVolume() { return this.data.get(8); }
    public boolean isFormed() { return this.data.get(9) == 1; }
    public boolean isLocked() { return this.data.get(10) == 1; }
    public int getLastBatchSize() { return this.data.get(11); }
    public int getAccumulatedYieldProgress() { return this.data.get(12); }
    public int getMachineModeOrdinal() { return this.data.get(13); }
    public int getLowTempInput() { return this.data.get(14); }
    public int getStructureYieldMultiplier() {
        int val = this.data.get(15);
        return val <= 0 ? 1 : val;
    }

    @Override
    public boolean stillValid(Player player) {
        return ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos())
                .evaluate((level, pos) -> player.distanceToSqr(pos.getCenter()) <= 64.0, true);
    }
    
    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        return ItemStack.EMPTY;
    }
}