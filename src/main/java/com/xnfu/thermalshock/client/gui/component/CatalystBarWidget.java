package com.xnfu.thermalshock.client.gui.component;

import com.xnfu.thermalshock.client.gui.SimulationChamberMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.List;

public class CatalystBarWidget extends AbstractWidget {

    private final SimulationChamberMenu menu;

    public CatalystBarWidget(int x, int y, int width, int height, SimulationChamberMenu menu) {
        super(x, y, width, height, Component.empty());
        this.menu = menu;
    }

    @Override
    protected void renderWidget(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        // 1. 背景 (深灰色)
        gfx.fill(getX(), getY(), getX() + width, getY() + height, 0xFF333333);

        // 2. 数据
        float current = menu.getCatalystAmount();
        float max = menu.getCatalystMax(); // [修改] 使用从服务端同步的实时最大值

        // 3. 计算高度 (如果 max 为 0，说明没催化剂，高度为 0)
        int h = (max > 0) ? (int)(current / max * height) : 0;
        if (h > height) h = height;

        // 4. 绘制填充 (粉紫色渐变)
        if (h > 0) {
            int startY = getY() + height - h;
            int endY = getY() + height;
            // 上浅下深，营造液体感
            gfx.fillGradient(getX(), startY, getX() + width, endY, 0xFFFF55FF, 0xFFAA00AA);
        }
    }

    public void appendHoverText(List<Component> tooltip, boolean isShiftDown) {
        if (!isHovered) return;

        tooltip.add(Component.translatable("gui.thermalshock.tooltip.catalyst_bar").withStyle(ChatFormatting.LIGHT_PURPLE));
        if (isShiftDown) {
            tooltip.add(Component.translatable("gui.thermalshock.tooltip.catalyst_bar.desc").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal(String.format("%.1f / %.1f Pts", menu.getCatalystAmount(), menu.getCatalystMax())).withStyle(ChatFormatting.DARK_GRAY));
        } else {
            tooltip.add(Component.literal(String.format("%.1f Pts", menu.getCatalystAmount())));
        }
    }

    // 禁用点击交互和音效
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {}
}