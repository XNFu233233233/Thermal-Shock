package com.xnfu.thermalshock.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.xnfu.thermalshock.network.PacketSetTargetHeat;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ThermalSourceScreen extends AbstractContainerScreen<ThermalSourceMenu> {

    private static final int BG_COLOR = 0xFFC6C6C6;
    private static final int BORDER_COLOR = 0xFF000000;
    private static final int SLOT_BORDER = 0xFF373737;
    private static final int SLOT_BG = 0xFF8B8B8B;

    private EditBox inputField;
    private int lastSyncedTarget = -1;

    public ThermalSourceScreen(ThermalSourceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = 72;
    }

    @Override
    protected void init() {
        super.init();

        this.inputField = new EditBox(this.font, leftPos + 110, topPos + 20, 50, 16, Component.literal("Target Heat"));
        this.inputField.setMaxLength(6);
        this.inputField.setBordered(true);
        this.inputField.setVisible(true);
        this.inputField.setValue(String.valueOf(menu.getTargetHeat()));
        this.inputField.setFilter(s -> s.matches("\\d*"));

        this.inputField.setResponder(text -> {
            if (text.isEmpty()) return;
            try {
                int val = Integer.parseInt(text);
                // [修复] 使用 getBlockEntity() 访问 BE
                PacketDistributor.sendToServer(new PacketSetTargetHeat(menu.getBlockEntity().getBlockPos(), val));
            } catch (NumberFormatException ignored) {}
        });

        this.addRenderableWidget(this.inputField);
    }

    @Override
    public void containerTick() {
        super.containerTick();
        if (!inputField.isFocused() && menu.getTargetHeat() != lastSyncedTarget) {
            lastSyncedTarget = menu.getTargetHeat();
            inputField.setValue(String.valueOf(lastSyncedTarget));
        }
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gfx, mouseX, mouseY, partialTick);
        super.render(gfx, mouseX, mouseY, partialTick);

        this.renderTooltip(gfx, mouseX, mouseY);

        if (isHovering(10, 20, 12, 50, mouseX, mouseY)) {
            List<Component> tips = new ArrayList<>();
            tips.add(Component.literal("Energy").withStyle(ChatFormatting.AQUA));
            tips.add(Component.literal(menu.getEnergyStored() + " / " + menu.getMaxEnergyStored() + " FE").withStyle(ChatFormatting.GRAY));
            gfx.renderTooltip(font, tips, Optional.empty(), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics gfx, float partialTick, int mouseX, int mouseY) {
        gfx.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, BORDER_COLOR);
        gfx.fill(leftPos + 2, topPos + 2, leftPos + imageWidth - 2, topPos + imageHeight - 2, BG_COLOR);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) drawSlotBg(gfx, leftPos + 8 + col * 18, topPos + 84 + row * 18);
        }
        for (int col = 0; col < 9; col++) drawSlotBg(gfx, leftPos + 8 + col * 18, topPos + 142);

        drawSlotBg(gfx, leftPos + 80, topPos + 35);
        drawEnergyBar(gfx, leftPos + 10, topPos + 20);
        drawProgressBar(gfx, leftPos + 102, topPos + 36);
        drawInfoText(gfx);
    }

    private void drawSlotBg(GuiGraphics gfx, int x, int y) {
        gfx.fill(x, y, x + 18, y + 18, SLOT_BORDER);
        gfx.fill(x + 1, y + 1, x + 17, y + 17, SLOT_BG);
    }

    private void drawEnergyBar(GuiGraphics gfx, int x, int y) {
        int w = 12;
        int h = 50;
        gfx.fill(x, y, x + w, y + h, 0xFF333333);

        long max = menu.getMaxEnergyStored();
        long cur = menu.getEnergyStored();
        if (max > 0 && cur > 0) {
            float pct = (float) ((double) cur / max);
            int fillH = (int)(pct * h);
            gfx.fillGradient(x + 1, y + h - fillH, x + w - 1, y + h, 0xFFFF0000, 0xFFAA0000);
        }
    }

    private void drawProgressBar(GuiGraphics gfx, int x, int y) {
        float progress = menu.getBurnProgress();
        int barW = 4;
        int barH = 16;
        gfx.fill(x, y, x + barW, y + barH, 0xFF555555);

        if (progress > 0) {
            int fillH = (int)(progress * barH);
            boolean isHeater = menu.getTotalHeatOutput() >= 0;
            int colorTop = isHeater ? 0xFFFFDD00 : 0xFFAAFFFF;
            int colorBot = isHeater ? 0xFFFF5500 : 0xFF00AAAA;
            gfx.fillGradient(x, y + barH - fillH, x + barW, y + barH, colorTop, colorBot);
        }
    }

    private void drawInfoText(GuiGraphics gfx) {
        int ticks = menu.getBurnTime();
        if (ticks > 0) {
            String timeStr = (ticks / 20) + "s";
            gfx.drawString(font, timeStr, leftPos + 110, topPos + 40, 0xFF444444, false);
        } else {
            gfx.drawString(font, "Idle", leftPos + 110, topPos + 40, 0xFF888888, false);
        }

        int heat = menu.getTotalHeatOutput();
        String sign = heat > 0 ? "+" : "";
        int color = heat > 0 ? 0xFFAA0000 : (heat < 0 ? 0xFF0000AA : 0xFF555555);
        String text = "Total: " + sign + heat + " H";
        gfx.drawString(font, text, leftPos + 60, topPos + 60, color, false);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.inputField.isFocused()) {
            if (keyCode == 256) {
                this.minecraft.player.closeContainer();
                return true;
            }
            return this.inputField.keyPressed(keyCode, scanCode, modifiers) || this.inputField.canConsumeInput();
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}