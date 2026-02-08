package com.xnfu.thermalshock.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.xnfu.thermalshock.registries.ThermalShockItems;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;

public class ThermalConverterScreen extends AbstractContainerScreen<ThermalConverterMenu> {

    // === 颜色常量 ===
    private static final int COLOR_BG_BASE = 0xFFC6C6C6;
    private static final int COLOR_SLOT_BORDER = 0xFF373737;
    private static final int COLOR_SLOT_BG = 0xFF8B8B8B;
    private static final int COLOR_BAR_BG = 0xFF000000;

    // === 布局坐标常量 ===
    private static final int FLUID_L_X = 18;
    private static final int FLUID_L_Y = 20;

    private static final int SLOT_IN_X = 44;
    private static final int SLOT_IN_Y = 35;

    private static final int ARROW_X = 66;
    private static final int ARROW_Y = 36;
    private static final int ARROW_W = 24;
    private static final int ARROW_H = 16;

    private static final int SLOT_OUT1_X = 95;
    private static final int SLOT_OUT1_Y = 35;

    private static final int SLOT_OUT2_X = 113;
    private static final int SLOT_OUT2_Y = 35;

    private static final int FLUID_R_X = 142;
    private static final int FLUID_R_Y = 20;

    private static final int FLUID_W = 16;
    private static final int FLUID_H = 48;

    // 底部热力条 (加宽)
    private static final int HEAT_BAR_X = 28;
    private static final int HEAT_BAR_Y = 72; // 稍微上移一点
    private static final int HEAT_BAR_W = 120;
    private static final int HEAT_BAR_H = 8; // 从 5 改为 8，加宽
    private static final int VISUAL_MAX_HEAT = 1000;

    public ThermalConverterScreen(ThermalConverterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = 72 + 12;
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        super.render(gfx, mouseX, mouseY, partialTick);
        this.renderTooltip(gfx, mouseX, mouseY);
        renderFluidTooltips(gfx, mouseX, mouseY);
        renderHeatTooltips(gfx, mouseX, mouseY);
    }

    private void renderHeatTooltips(GuiGraphics gfx, int mouseX, int mouseY) {
        if (isHovering(HEAT_BAR_X, HEAT_BAR_Y, HEAT_BAR_W, HEAT_BAR_H, mouseX, mouseY)) {
            List<Component> tips = new ArrayList<>();
            tips.add(Component.translatable("gui.thermalshock.converter.heat_label", menu.getCurrentHeat()).withStyle(net.minecraft.ChatFormatting.GOLD));
            
            if (hasShiftDown()) {
                tips.add(Component.empty());
                tips.add(Component.translatable("gui.thermalshock.converter.heat_requirement").withStyle(net.minecraft.ChatFormatting.GRAY, net.minecraft.ChatFormatting.ITALIC));
            } else {
                tips.add(Component.translatable("gui.thermalshock.tooltip.hold_shift").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
            }
            
            gfx.renderTooltip(font, tips, java.util.Optional.empty(), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics gfx, float partialTick, int mouseX, int mouseY) {
        gfx.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF000000);
        gfx.fill(leftPos + 2, topPos + 2, leftPos + imageWidth - 2, topPos + imageHeight - 2, COLOR_BG_BASE);

        drawSlot(gfx, SLOT_IN_X, SLOT_IN_Y);
        drawSlot(gfx, SLOT_OUT1_X, SLOT_OUT1_Y);
        drawSlot(gfx, SLOT_OUT2_X, SLOT_OUT2_Y);

        // 绘制升级槽空心边框 (透明底板)
        for (int i = 0; i < 4; i++) {
            int sx = leftPos - 20;
            int sy = topPos + 10 + i * 18;
            gfx.fill(sx, sy, sx + 18, sy + 1, COLOR_SLOT_BORDER);       // Top
            gfx.fill(sx, sy + 17, sx + 18, sy + 18, COLOR_SLOT_BORDER); // Bottom
            gfx.fill(sx, sy, sx + 1, sy + 18, COLOR_SLOT_BORDER);       // Left
            gfx.fill(sx + 17, sy, sx + 18, sy + 18, COLOR_SLOT_BORDER); // Right

            // [新增] 渲染高度透明的背景图标
            if (menu.getSlot(3 + i).getItem().isEmpty()) {
                RenderSystem.enableBlend();
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 0.12f);
                gfx.renderItem(new ItemStack(ThermalShockItems.OVERCLOCK_UPGRADE.get()), sx + 1, sy + 1);
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
                RenderSystem.disableBlend();
            }
        }

        drawFluidTankBg(gfx, FLUID_L_X, FLUID_L_Y);
        drawFluidTankBg(gfx, FLUID_R_X, FLUID_R_Y);

        // 渲染流体
        renderFluid(gfx, 0, FLUID_L_X, FLUID_L_Y);
        renderFluid(gfx, 1, FLUID_R_X, FLUID_R_Y);

        renderProgressBar(gfx);
        renderHeatGauge(gfx);
        drawPlayerInv(gfx);

        // [核心修改] 渲染高度透明的背景图标
        RenderSystem.enableBlend();
        // 此处的透明度控制【输入槽】(Material Clump)
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 0.06f); 
        if (menu.getSlot(0).getItem().isEmpty()) {
            gfx.renderItem(new ItemStack(ThermalShockItems.MATERIAL_CLUMP.get()), leftPos + SLOT_IN_X + 1, topPos + SLOT_IN_Y + 1);
        }
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }

    // ==========================================
    // 绘图组件逻辑
    // ==========================================

    private void renderHeatGauge(GuiGraphics gfx) {
        int sx = leftPos + HEAT_BAR_X;
        int sy = topPos + HEAT_BAR_Y;
        int cx = sx + HEAT_BAR_W / 2;

        // 1. 背景轨道
        gfx.fill(sx, sy, sx + HEAT_BAR_W, sy + HEAT_BAR_H, COLOR_BAR_BG);

        int heat = menu.getCurrentHeat();
        float ratio = (float) Math.abs(heat) / VISUAL_MAX_HEAT;
        ratio = Mth.clamp(ratio, 0.0f, 1.0f);
        int barLength = (int) (ratio * (HEAT_BAR_W / 2));

        // 2. 热量进度条 (在刻度下方)
        if (heat > 0) {
            gfx.fillGradient(cx, sy + 1, cx + barLength, sy + HEAT_BAR_H - 1, 0xFFFF0000, 0xFFFFAA00);
        } else if (heat < 0) {
            gfx.fillGradient(cx - barLength, sy + 1, cx, sy + HEAT_BAR_H - 1, 0xFF00AAAA, 0xFF0055FF);
        }

        // 3. [调整] 刻度线置于顶层 (每 100 热量 = 6 像素)
        for (int i = 1; i <= 10; i++) {
            int offset = i * 6;
            // 仅在顶部和底部各绘制 2px 的线段，不再贯穿全高
            // 上方刻度
            gfx.fill(cx + offset, sy, cx + offset + 1, sy + 2, 0xAAFFFFFF);
            gfx.fill(cx - offset, sy, cx - offset + 1, sy + 2, 0xAAFFFFFF);
            // 下方刻度
            gfx.fill(cx + offset, sy + HEAT_BAR_H - 2, cx + offset + 1, sy + HEAT_BAR_H, 0xAAFFFFFF);
            gfx.fill(cx - offset, sy + HEAT_BAR_H - 2, cx - offset + 1, sy + HEAT_BAR_H, 0xAAFFFFFF);
        }

        // 4. 中心点 (圆点 0) - 使用加粗的纵向白块表示“原点”
        gfx.fill(cx - 1, sy - 1, cx + 2, sy + HEAT_BAR_H + 1, 0xFFFFFFFF);
        // 使用 drawString 并手动居中且禁用阴影
        gfx.drawString(font, "0", cx - font.width("0") / 2, sy - 10, 0xFFFFFFFF, false);
    }

    private void renderProgressBar(GuiGraphics gfx) {
        int sx = leftPos + ARROW_X;
        int sy = topPos + ARROW_Y;

        // 背景 (加粗)
        gfx.fill(sx, sy + 4, sx + ARROW_W, sy + 12, 0xFF555555);

        int total = menu.getTotalProcessTime();
        int current = menu.getProcessTime();
        if (total > 0 && current > 0) {
            float pct = (float) current / total;
            int fillW = (int) (pct * ARROW_W);

            if (fillW > 0) {
                gfx.fill(sx, sy + 4, sx + fillW, sy + 12, 0xFF00FF00);
            }
        }
    }

    private void renderFluid(GuiGraphics gfx, int tankIdx, int x, int y) {
        int id = menu.getFluidId(tankIdx);
        int amount = menu.getFluidAmount(tankIdx);
        int cap = menu.getFluidCapacity(tankIdx);

        if (amount <= 0 || cap <= 0) return;

        Fluid fluid = BuiltInRegistries.FLUID.byId(id);
        if (fluid == net.minecraft.world.level.material.Fluids.EMPTY) return;

        FluidStack stack = new FluidStack(fluid, amount);
        IClientFluidTypeExtensions clientFluid = IClientFluidTypeExtensions.of(fluid);
        TextureAtlasSprite sprite = this.minecraft.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(clientFluid.getStillTexture(stack));
        int color = clientFluid.getTintColor(stack);

        // [修复] 强制 Alpha 通道：如果流体颜色没有 Alpha (0x00RRGGBB)，强制设为不透明 (0xFFRRGGBB)
        if (((color >> 24) & 0xFF) == 0) {
            color |= 0xFF000000;
        }

        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = ((color >> 24) & 0xFF) / 255f;

        RenderSystem.setShaderColor(r, g, b, a);

        int drawH = (int) ((float) amount / cap * FLUID_H);
        int sx = leftPos + x + 1; // +1 因为背景有1px边框
        int bottomY = topPos + y + 1 + FLUID_H; // 槽底 Y

        // 平铺绘制 (从下往上)
        int renderedH = 0;
        int width = FLUID_W; // 16

        while (renderedH < drawH) {
            int segmentH = Math.min(drawH - renderedH, 16); // 每次最多画16高
            int drawY = bottomY - renderedH - segmentH;

            // [关键] 使用 blit 的重载方法精确控制 UV，防止贴图拉伸
            // 参数: x, y, blitOffset, width, height, sprite
            gfx.blit(sx, drawY, 0, width, segmentH, sprite);

            renderedH += segmentH;
        }

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private void renderFluidTooltips(GuiGraphics gfx, int mouseX, int mouseY) {
        for (int i = 0; i < 2; i++) {
            int x = (i == 0) ? FLUID_L_X : FLUID_R_X;
            int y = (i == 0) ? FLUID_L_Y : FLUID_R_Y;

            if (isHovering(x, y, FLUID_W + 2, FLUID_H + 2, mouseX, mouseY)) {
                int id = menu.getFluidId(i);
                int amount = menu.getFluidAmount(i);
                int cap = menu.getFluidCapacity(i);

                if (amount > 0) {
                    Fluid fluid = BuiltInRegistries.FLUID.byId(id);
                    Component name = fluid.getFluidType().getDescription();
                    gfx.renderTooltip(font, Component.translatable("gui.thermalshock.tooltip.fluid", name, amount, cap), mouseX, mouseY);
                } else {
                    gfx.renderTooltip(font, Component.literal("Empty"), mouseX, mouseY);
                }
            }
        }
    }

    // ==========================================
    // 基础绘制辅助方法
    // ==========================================

    private void drawSlot(GuiGraphics gfx, int x, int y) {
        gfx.fill(leftPos + x, topPos + y, leftPos + x + 18, topPos + y + 18, COLOR_SLOT_BORDER);
        gfx.fill(leftPos + x + 1, topPos + y + 1, leftPos + x + 17, topPos + y + 17, COLOR_SLOT_BG);
    }

    private void drawFluidTankBg(GuiGraphics gfx, int x, int y) {
        gfx.fill(leftPos + x, topPos + y, leftPos + x + 18, topPos + y + 50, COLOR_SLOT_BORDER);
        gfx.fill(leftPos + x + 1, topPos + y + 1, leftPos + x + 17, topPos + y + 49, COLOR_SLOT_BG);
    }

    private void drawPlayerInv(GuiGraphics gfx) {
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) {
                drawSlot(gfx, 8 + c * 18, 84 + r * 18);
            }
        }
        for (int c = 0; c < 9; c++) {
            drawSlot(gfx, 8 + c * 18, 142);
        }
    }
}