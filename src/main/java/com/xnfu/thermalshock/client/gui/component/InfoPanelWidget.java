package com.xnfu.thermalshock.client.gui.component;

import com.xnfu.thermalshock.block.entity.MachineMode;
import com.xnfu.thermalshock.client.gui.SimulationChamberMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.List;

public class InfoPanelWidget extends AbstractWidget {

    private final SimulationChamberMenu menu;
    private final Font font;

    // 内部行高
    private static final int LINE_HEIGHT = 10;
    // 行间距
    private static final int LINE_GAP = 2;

    // 布局偏移 (相对于 Widget Y)
    // 4行布局，垂直排列
    private static final int REL_Y_1 = 0;                   // IO
    private static final int REL_Y_2 = LINE_HEIGHT + LINE_GAP; // Efficiency
    private static final int REL_Y_3 = (LINE_HEIGHT + LINE_GAP) * 2; // Yield
    private static final int REL_Y_4 = (LINE_HEIGHT + LINE_GAP) * 3; // Progress
    private static final int REL_Y_5 = (LINE_HEIGHT + LINE_GAP) * 4;

    public InfoPanelWidget(int x, int y, int width, int height, SimulationChamberMenu menu) {
        super(x, y, width, height, Component.empty());
        this.menu = menu;
        this.font = Minecraft.getInstance().font;
    }

    @Override
    protected void renderWidget(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        int mode = menu.getMachineModeOrdinal();
        int x = getX() + 2;
        int y = getY();

        // === Line 1: 热力数值 (IO / Delta) ===
        if (mode == MachineMode.OVERHEATING.ordinal()) {
            int rate = menu.getHeatIoRate();
            String sign = rate > 0 ? "+" : "";
            int color = rate >= 0 ? 0xFF550000 : 0xFF0000AA;
            gfx.drawString(font, "Rate: " + sign + rate + " H", x, y + REL_Y_1, color, false);
        } else {
            int delta = menu.getHeatStored();
            gfx.drawString(font, "ΔT: " + delta, x, y + REL_Y_1, 0xFF55FFFF, false);
        }

        // === Line 2: 效率 (Efficiency) ===
        // Menu.getStructureYieldMultiplier() 读取 Index 15 (Efficiency)
        // Menu 内部已除以 100，此处直接使用
        float eff = menu.getStructureYieldMultiplier();
        gfx.drawString(font, String.format("Eff: %.0f%%", eff), x, y + REL_Y_2, 0xFF00AA00, false);

        // === Line 3: 产量加成 (Yield Bonus) ===
        // [重构] 提取计算逻辑
        float totalRate = calculateTotalYieldRate();

        // 4. 显示增量: (2.2 - 1.0) * 100 = 120%
        float displayVal = (totalRate - 1.0f) * 100.0f;

        String yieldText = String.format("Yield: +%.0f%%", displayVal);
        gfx.drawString(font, yieldText, x, y + REL_Y_3, 0xFFDDAA00, false);

        // === Line 4: 进度 (Progress) ===
        int progress = menu.getAccumulatedYieldProgress();
        gfx.drawString(font, String.format("Prog: %d%%", progress), x, y + REL_Y_4, 0xFF555555, false);

        // === Line 5: 警告信息 (Warning) ===
        gfx.drawString(font, Component.translatable("gui.thermalshock.warning.short"), x, y + REL_Y_5, 0xFFFF5555, false);
    }

    // [新增] 提取的计算方法
    private float calculateTotalYieldRate() {
        float structMult = menu.getBonusYield();
        if (structMult < 1.0f) structMult = 1.0f;
        float catBonus = menu.getEfficiency();
        // 公式: 1(基础) * 结构倍率 * (1 + 催化剂)
        return 1.0f * structMult * (1.0f + catBonus);
    }

    public void appendHoverText(List<Component> tooltip, int mouseX, int mouseY, boolean isShiftDown) {
        if (!isHovered) return;
        int relY = mouseY - getY();

        // Line 1
        if (relY >= REL_Y_1 && relY < REL_Y_1 + LINE_HEIGHT) {
            if (menu.getMachineModeOrdinal() == MachineMode.THERMAL_SHOCK.ordinal()) {
                tooltip.add(Component.translatable("gui.thermalshock.tooltip.delta.title").withStyle(ChatFormatting.AQUA));
                if (isShiftDown) {
                    int high = menu.getHeatIoRate(); // 热冲击模式下这里存的是 High
                    int low = menu.getLowTempInput();
                    tooltip.add(Component.translatable("gui.thermalshock.tooltip.input.high", high).withStyle(ChatFormatting.RED));
                    tooltip.add(Component.translatable("gui.thermalshock.tooltip.input.low", low).withStyle(ChatFormatting.BLUE));
                }
            } else {
                tooltip.add(Component.translatable("gui.thermalshock.tooltip.heat_io.title").withStyle(ChatFormatting.GOLD));
                if (isShiftDown) tooltip.add(Component.translatable("gui.thermalshock.tooltip.heat_io.detail").withStyle(ChatFormatting.DARK_GRAY));
            }
        }
        // Line 2
        else if (relY >= REL_Y_2 && relY < REL_Y_2 + LINE_HEIGHT) {
            tooltip.add(Component.translatable("gui.thermalshock.tooltip.efficiency.title").withStyle(ChatFormatting.GREEN));
            if (isShiftDown) tooltip.add(Component.translatable("gui.thermalshock.tooltip.efficiency.detail").withStyle(ChatFormatting.DARK_GRAY));
        }
        // Line 3
        else if (relY >= REL_Y_3 && relY < REL_Y_3 + LINE_HEIGHT) {
            tooltip.add(Component.translatable("gui.thermalshock.tooltip.yield.title").withStyle(ChatFormatting.GOLD));
            if (isShiftDown) tooltip.add(Component.translatable("gui.thermalshock.tooltip.yield.formula").withStyle(ChatFormatting.GRAY));
        }
        // Line 4
        else if (relY >= REL_Y_4 && relY < REL_Y_4 + LINE_HEIGHT) {
            tooltip.add(Component.translatable("gui.thermalshock.tooltip.progress.title").withStyle(ChatFormatting.GRAY));
            if (isShiftDown) {
                tooltip.add(Component.translatable("gui.thermalshock.tooltip.progress.desc").withStyle(ChatFormatting.DARK_GRAY));
            }
        }
        // [新增] Line 5: 警告详情
        else if (relY >= REL_Y_5 && relY < REL_Y_5 + LINE_HEIGHT) {
            if (isShiftDown) {
                tooltip.add(Component.translatable("gui.thermalshock.warning.detail").withStyle(ChatFormatting.RED));
                tooltip.add(Component.translatable("gui.thermalshock.warning.solution").withStyle(ChatFormatting.GRAY));
            } else {
                tooltip.add(Component.translatable("gui.thermalshock.tooltip.hold_shift").withStyle(ChatFormatting.DARK_GRAY));
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {}
}