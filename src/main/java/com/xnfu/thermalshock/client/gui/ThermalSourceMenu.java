package com.xnfu.thermalshock.client.gui;

import com.xnfu.thermalshock.block.entity.ThermalSourceBlockEntity;
import com.xnfu.thermalshock.registries.ThermalShockMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;

public class ThermalSourceMenu extends AbstractContainerMenu {

    private final ThermalSourceBlockEntity be;
    private final ContainerData data;

    public ThermalSourceMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, (ThermalSourceBlockEntity) inv.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(8));
    }

    public ThermalSourceMenu(int containerId, Inventory inv, ThermalSourceBlockEntity entity, ContainerData data) {
        super(ThermalShockMenus.THERMAL_SOURCE_MENU.get(), containerId);
        this.be = entity;
        this.data = data;

        // [修改] 校验数据大小为 8
        checkContainerDataCount(data, 8);
        addDataSlots(data);

        // 燃料槽 (Slot 0) - 上移至 y=26 以腾出下方空间
        this.addSlot(new SlotItemHandler(be.getItemHandler(), 0, 80, 26));

        // 玩家背包
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

    // [新增] 公开 BE 访问方法，解决 "menu.be has private access"
    public ThermalSourceBlockEntity getBlockEntity() {
        return this.be;
    }

    // --- 数据同步 Getters ---

    public int getBurnTime() { return data.get(0); }
    public int getMaxBurnTime() { return data.get(1); }

    // [修复] 重命名为 getTotalHeatOutput 以匹配 Screen 调用
    public int getTotalHeatOutput() { return data.get(2); }

    public long getEnergyStored() {
        long low = data.get(3) & 0xFFFF_FFFFL;
        long high = data.get(4);
        return (high << 32) | low;
    }

    public int getTargetHeat() { return data.get(5); }

    public long getMaxEnergyStored() {
        return (long) data.get(6) & 0xFFFF_FFFFL;
    }
    public int getLastTickEnergy() { return data.get(7); }

    public float getBurnProgress() {
        int current = getBurnTime();
        int max = getMaxBurnTime();
        if (max == 0 || current == 0) return 0.0f;
        return (float) current / max;
    }

    public boolean isBurning() {
        return getBurnTime() > 0;
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
            if (index < 1) { // 机器 -> 玩家
                if (!this.moveItemStackTo(itemstack1, 1, 37, true)) return ItemStack.EMPTY;
            } else { // 玩家 -> 机器
                if (!this.moveItemStackTo(itemstack1, 0, 1, false)) return ItemStack.EMPTY;
            }
            if (itemstack1.isEmpty()) slot.set(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return itemstack;
    }
}