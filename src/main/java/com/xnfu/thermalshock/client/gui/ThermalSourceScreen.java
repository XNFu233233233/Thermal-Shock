package com.xnfu.thermalshock.client.gui;

import com.xnfu.thermalshock.network.PacketSetTargetHeat;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
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
    private Button confirmBtn;

    // 用于检测是否需要从服务端同步回填 (当玩家没在输入时)
    private int cachedTargetHeat = -1;

    public ThermalSourceScreen(ThermalSourceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = 72;
    }

    @Override
    protected void init() {
        super.init();

        // 1. 输入框 (右侧)
        this.inputField = new EditBox(this.font, leftPos + 115, topPos + 26, 36, 16, Component.translatable("gui.thermalshock.source.target"));
        this.inputField.setMaxLength(6);
        this.inputField.setBordered(true);
        this.inputField.setVisible(true);
        this.inputField.setValue(String.valueOf(menu.getTargetHeat()));
        this.inputField.setFilter(s -> s.matches("\\d*")); // 仅数字
        this.addRenderableWidget(this.inputField);

        // 2. 确认按钮 (紧贴输入框右侧)
        this.confirmBtn = Button.builder(Component.literal("✔"), btn -> sendUpdate())
                .bounds(leftPos + 152, topPos + 26, 16, 16)
                .tooltip(Tooltip.create(Component.translatable("gui.thermalshock.source.set").withStyle(ChatFormatting.GREEN)))
                .build();
        this.addRenderableWidget(this.confirmBtn);
    }

    private void sendUpdate() {
        String val = inputField.getValue();
        if (!val.isEmpty()) {
            try {
                int target = Integer.parseInt(val);
                PacketDistributor.sendToServer(new PacketSetTargetHeat(menu.getBlockEntity().getBlockPos(), target));
            } catch (NumberFormatException ignored) {}
        }
    }

    @Override
    public void containerTick() {
        super.containerTick();
        // 只有当输入框没有聚焦时，才同步服务端的数据，防止打字被打断
        if (!inputField.isFocused() && menu.getTargetHeat() != cachedTargetHeat) {
            cachedTargetHeat = menu.getTargetHeat();
            inputField.setValue(String.valueOf(cachedTargetHeat));
        }
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gfx, mouseX, mouseY, partialTick);
        super.render(gfx, mouseX, mouseY, partialTick);

        this.renderTooltip(gfx, mouseX, mouseY);

        // 能量条 Tooltip
        if (isHovering(10, 20, 12, 50, mouseX, mouseY)) {
            List<Component> tips = new ArrayList<>();
            tips.add(Component.translatable("gui.thermalshock.source.energy_buffer").withStyle(ChatFormatting.AQUA));
            tips.add(Component.literal(menu.getEnergyStored() + " / " + menu.getMaxEnergyStored() + " FE").withStyle(ChatFormatting.GRAY));
            tips.add(Component.translatable("gui.thermalshock.source.energy_input", menu.getLastTickEnergy()).withStyle(ChatFormatting.YELLOW));
            gfx.renderTooltip(font, tips, Optional.empty(), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics gfx, float partialTick, int mouseX, int mouseY) {
        // 背景
        gfx.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, BORDER_COLOR);
        gfx.fill(leftPos + 2, topPos + 2, leftPos + imageWidth - 2, topPos + imageHeight - 2, BG_COLOR);

        // 玩家物品栏槽位
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) drawSlotBg(gfx, leftPos + 8 + col * 18, topPos + 84 + row * 18);
        }
        for (int col = 0; col < 9; col++) drawSlotBg(gfx, leftPos + 8 + col * 18, topPos + 142);

        // 机器槽位
        drawSlotBg(gfx, leftPos + 80, topPos + 26);

        // 进度条
        drawEnergyBar(gfx, leftPos + 10, topPos + 20);
        drawProgressBar(gfx, leftPos + 58, topPos + 48); // 居中于槽位下方

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
            // [修复] 防止渲染溢出
            int fillH = Math.min(h, (int)(pct * h));

            // 绘制渐变
            gfx.fillGradient(x + 1, y + h - fillH, x + w - 1, y + h, 0xFFFF0000, 0xFFAA0000);
        }
    }

    private void drawProgressBar(GuiGraphics gfx, int x, int y) {
        float progress = menu.getBurnProgress();
        int barW = 60; // 足够宽
        int barH = 5;
        gfx.fill(x, y, x + barW, y + barH, 0xFF555555);

        if (progress > 0) {
            int fillW = Math.min(barW, (int)(progress * barW));
            boolean isHeater = menu.getTotalHeatOutput() >= 0;
            int colorTop = isHeater ? 0xFFFFDD00 : 0xFFAAFFFF;
            int colorBot = isHeater ? 0xFFFF5500 : 0xFF00AAAA;
            gfx.fillGradient(x, y, x + fillW, y + barH, colorTop, colorBot);
        }
    }

    private void drawInfoText(GuiGraphics gfx) {
        // 总热量输出文本
        int heat = menu.getTotalHeatOutput();
        String sign = heat > 0 ? "+" : "";
        int color = heat > 0 ? 0xFFAA0000 : (heat < 0 ? 0xFF0000AA : 0xFF555555);
        Component text = Component.translatable("gui.thermalshock.source.output", sign + heat);

        // 居中显示在进度条下方 (y=58)
        gfx.drawCenteredString(font, text, leftPos + imageWidth / 2, topPos + 58, color);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.inputField.isFocused()) {
            // ESC 退出
            if (keyCode == 256) {
                this.minecraft.player.closeContainer();
                return true;
            }
            // 回车确认
            if (keyCode == 257) {
                sendUpdate();
                return true;
            }
            return this.inputField.keyPressed(keyCode, scanCode, modifiers) || this.inputField.canConsumeInput();
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}