package com.xnfu.thermalshock.client.gui.component;

import com.xnfu.thermalshock.block.entity.MachineMode;
import com.xnfu.thermalshock.client.gui.SimulationChamberMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.List;

public class HeatBarWidget extends AbstractWidget {

    private final SimulationChamberMenu menu;

    public HeatBarWidget(int x, int y, int width, int height, SimulationChamberMenu menu) {
        super(x, y, width, height, Component.empty());
        this.menu = menu;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 禁用点击交互和音效
        return false;
    }

    @Override
    protected void renderWidget(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        // 绘制背景
        gfx.fill(getX(), getY(), getX() + width, getY() + height, 0xFF333333);

        int modeOrdinal = menu.getMachineModeOrdinal();
        int value = menu.getHeatStored();
        int max = menu.getMaxHeat();
        if (max == 0) max = 10000;

        int h = (int)((float)value / max * height);
        h = Mth.clamp(h, 0, height);

        if (h > 0) {
            int c1, c2;
            if (modeOrdinal == MachineMode.OVERHEATING.ordinal()) {
                c1 = 0xFFFF0000; // 红
                c2 = 0xFFFFFF00; // 黄
            } else {
                c1 = 0xFF00FFFF; // 青
                c2 = 0xFF0088FF; // 蓝
            }
            gfx.fillGradient(getX(), getY() + height - h, getX() + width, getY() + height, c1, c2);
        }
    }

    public void appendHoverText(List<Component> tooltip, boolean isShiftDown) {
        if (!isHovered) return;

        int modeOrdinal = menu.getMachineModeOrdinal();

        if (modeOrdinal == MachineMode.OVERHEATING.ordinal()) {
            // 过热模式
            tooltip.add(Component.translatable("gui.thermalshock.tooltip.heat_bar").withStyle(ChatFormatting.RED));
            tooltip.add(Component.literal(menu.getHeatStored() + " / " + menu.getMaxHeat()).withStyle(ChatFormatting.GOLD));

            // 显示净输入 (Index 7 在过热模式下传输的是 Sum)
            int netInput = menu.getHeatIoRate();
            tooltip.add(Component.translatable("gui.thermalshock.tooltip.input.net", netInput).withStyle(ChatFormatting.GRAY));

            if (isShiftDown) {
                tooltip.add(Component.translatable("gui.thermalshock.tooltip.heat_bar.desc").withStyle(ChatFormatting.DARK_GRAY));
            }
        } else {
            // 热冲击模式
            tooltip.add(Component.literal("ΔT: " + menu.getHeatStored()).withStyle(ChatFormatting.AQUA));

            // 获取详细输入数据
            // 在热冲击模式：HeatIoRate(Index 7) = HighTemp, LowTempInput(Index 14) = LowTemp
            int high = menu.getHeatIoRate();
            int low = menu.getLowTempInput();

            tooltip.add(Component.translatable("gui.thermalshock.tooltip.input.high", high).withStyle(ChatFormatting.RED));
            tooltip.add(Component.translatable("gui.thermalshock.tooltip.input.low", low).withStyle(ChatFormatting.BLUE));

            if (isShiftDown) {
                // 这里我们不再显示 generic desc，而是留给 InfoPanel 显示详细配方需求
                tooltip.add(Component.translatable("gui.thermalshock.tooltip.delta.desc", "---", "---").withStyle(ChatFormatting.DARK_GRAY));
            }
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {}
}