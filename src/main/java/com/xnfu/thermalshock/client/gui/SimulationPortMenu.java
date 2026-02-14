package com.xnfu.thermalshock.client.gui;

import com.mojang.logging.LogUtils; // [新增]
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
import org.slf4j.Logger;

public class SimulationPortMenu extends AbstractContainerMenu {
    private static final Logger LOGGER = LogUtils.getLogger(); // [新增] 日志记录器

    public final SimulationPortBlockEntity be;
    private final ContainerData data;
    private final Inventory playerInventory;

    // GUI 布局常量
    public static final int SLOT_SIZE = 18;
    public static final int VISIBLE_ROWS = 3;

    // 默认列表位置
    public static final int SLOT_START_X = 36;
    public static final int SLOT_START_Y = 21;

    // 展开列表位置
    public static final int EXPANDED_START_X = 8;
    public static final int EXPANDED_START_Y = 21;

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

        // 1. 初始化时一次性添加所有槽位
        addSlots();
        // 2. 根据初始状态设置坐标
        updateSlotPositions();
    }

    private void addSlots() {
        // 添加机器槽位 (Index 0-26)
        for (int i = 0; i < 27; i++) {
            this.addSlot(new SlotItemHandler(be.getItemHandler(), i, 0, 0) {
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

        // 添加玩家背包 (Index 27-62)
        addPlayerInventory(new InvWrapper(playerInventory));
    }

    public void updateSlotPositions() {
        for (int i = 0; i < 27; i++) {
            Slot slot = this.slots.get(i);
            int x = -10000; 
            int y = -10000;

            if (isExpanded) {
                int col = i % 9;
                int row = i / 9;
                x = EXPANDED_START_X + col * SLOT_SIZE;
                y = EXPANDED_START_Y + row * SLOT_SIZE;
            } else {
                int col = i % 3;
                int row = i / 3;
                if (row >= scrollOffset && row < scrollOffset + VISIBLE_ROWS) {
                    int viewRow = row - scrollOffset;
                    x = SLOT_START_X + col * SLOT_SIZE;
                    y = SLOT_START_Y + viewRow * SLOT_SIZE;
                }
            }
            
            setSlotPosition(slot, x + 1, y + 1);
        }
    }

    private void setSlotPosition(Slot slot, int x, int y) {
        try {
            java.lang.reflect.Field fieldX = Slot.class.getDeclaredField("x");
            java.lang.reflect.Field fieldY = Slot.class.getDeclaredField("y");
            fieldX.setAccessible(true);
            fieldY.setAccessible(true);
            fieldX.setInt(slot, x);
            fieldY.setInt(slot, y);
        } catch (Exception e) {
            LOGGER.error("Failed to set slot position via reflection", e);
        }
    }

    public void setExpanded(boolean expanded) {
        this.isExpanded = expanded;
        this.scrollOffset = 0;
        updateSlotPositions();
    }

    public void setScrollOffset(int offset) {
        this.scrollOffset = Math.max(0, Math.min(offset, 6));
        updateSlotPositions();
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
                this.addSlot(new SlotItemHandler(playerInventory, col + row * 9 + 9, 9 + col * 18, 85 + row * 18));
            }
        }
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new SlotItemHandler(playerInventory, col, 9 + col * 18, 143));
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