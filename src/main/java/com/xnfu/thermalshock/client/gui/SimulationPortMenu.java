package com.xnfu.thermalshock.client.gui;

import com.xnfu.thermalshock.block.entity.PortMode;
import com.xnfu.thermalshock.block.entity.SimulationPortBlockEntity;
import com.xnfu.thermalshock.registries.ThermalShockDataMaps;
import com.xnfu.thermalshock.registries.ThermalShockMenus;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;

import java.lang.reflect.Field;
import java.util.List;

public class SimulationPortMenu extends AbstractContainerMenu {

    public final SimulationPortBlockEntity be;
    private final ContainerData data;
    private final Inventory playerInventory;

    // GUI 布局常量
    public static final int SLOT_SIZE = 18;
    public static final int VISIBLE_ROWS = 3;

    // 默认列表位置
    public static final int SLOT_START_X = 35;
    public static final int SLOT_START_Y = 20;

    // 展开列表位置
    public static final int EXPANDED_START_X = 7;
    public static final int EXPANDED_START_Y = 20;

    // 状态变量
    private boolean isExpanded = false;
    private int scrollOffset = 0; // 0 ~ 6

    public SimulationPortMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, (SimulationPortBlockEntity) inv.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(9));
    }

    public SimulationPortMenu(int containerId, Inventory inv, SimulationPortBlockEntity entity, ContainerData data) {
        super(ThermalShockMenus.SIMULATION_PORT_MENU.get(), containerId);
        this.be = entity;
        this.data = data;
        this.playerInventory = inv;

        checkContainerDataCount(data, 9);
        addDataSlots(data);

        // 初始化布局
        layoutSlots();
    }

    public void layoutSlots() {
        // 1. 清空 public 的 slots 列表
        this.slots.clear();

        // 2. [反射] 强制清空父类的 private 列表
        try {
            Field lastSlotsField = AbstractContainerMenu.class.getDeclaredField("lastSlots");
            lastSlotsField.setAccessible(true);
            ((List<?>) lastSlotsField.get(this)).clear();

            Field remoteSlotsField = AbstractContainerMenu.class.getDeclaredField("remoteSlots");
            remoteSlotsField.setAccessible(true);
            ((List<?>) remoteSlotsField.get(this)).clear();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 3. 重新添加机器槽位 (Index 0-26)
        int totalMachineSlots = 27;
        for (int i = 0; i < totalMachineSlots; i++) {
            int x = -10000; // 默认隐藏
            int y = -10000;

            if (isExpanded) {
                // 展开模式: 9x3
                int col = i % 9;
                int row = i / 9;
                x = EXPANDED_START_X + col * SLOT_SIZE;
                y = EXPANDED_START_Y + row * SLOT_SIZE;
            } else {
                // 默认模式: 3x3 带滚动
                int col = i % 3;
                int row = i / 3;
                if (row >= scrollOffset && row < scrollOffset + VISIBLE_ROWS) {
                    int viewRow = row - scrollOffset;
                    x = SLOT_START_X + col * SLOT_SIZE;
                    y = SLOT_START_Y + viewRow * SLOT_SIZE;
                }
            }

            // 使用 NeoForge 标准 SlotItemHandler
            this.addSlot(new SlotItemHandler(be.getItemHandler(), i, x, y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    PortMode mode = be.getPortMode();
                    if (mode == PortMode.OUTPUT) return false;
                    if (mode == PortMode.CATALYST) {
                        return BuiltInRegistries.ITEM.wrapAsHolder(stack.getItem()).getData(ThermalShockDataMaps.CATALYST_PROPERTY) != null;
                    }
                    return true;
                }
            });
        }

        // 4. 重新添加玩家背包 (Index 27-62)
        addPlayerInventory(new InvWrapper(playerInventory));
    }

    public void setExpanded(boolean expanded) {
        this.isExpanded = expanded;
        this.scrollOffset = 0;
        layoutSlots();
    }

    public void setScrollOffset(int offset) {
        this.scrollOffset = Math.max(0, Math.min(offset, 6));
        layoutSlots();
    }

    public int getScrollOffset() { return scrollOffset; }
    public boolean isExpanded() { return isExpanded; }

    public int getFluidId(int tankIndex) { return data.get(tankIndex * 3); }
    public int getFluidAmount(int tankIndex) { return data.get(tankIndex * 3 + 1); }
    public int getFluidCapacity(int tankIndex) { return data.get(tankIndex * 3 + 2); }
    public PortMode getPortMode() { return be.getPortMode(); }

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

    @Override
    public boolean stillValid(Player player) {
        return ContainerLevelAccess.create(be.getLevel(), be.getBlockPos())
                .evaluate((level, pos) -> player.distanceToSqr(pos.getCenter()) <= 64.0, true);
    }

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();

            if (index < 27) {
                if (!this.moveItemStackTo(itemstack1, 27, 63, true)) return ItemStack.EMPTY;
            } else {
                if (!this.moveItemStackTo(itemstack1, 0, 27, false)) return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty()) slot.set(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return itemstack;
    }
}