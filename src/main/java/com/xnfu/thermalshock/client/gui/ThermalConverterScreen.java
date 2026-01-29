package com.xnfu.thermalshock.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
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

public class ThermalConverterScreen extends AbstractContainerScreen<ThermalConverterMenu> {

    // 颜色常量
    private static final int BG_COLOR = 0xFFC6C6C6;
    private static final int SLOT_BG = 0xFF8B8B8B;
    private static final int SLOT_BORDER = 0xFF373737;

    public ThermalConverterScreen(ThermalConverterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics gfx, float partialTick, int mouseX, int mouseY) {
        // 1. 基础背景
        gfx.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF000000);
        gfx.fill(leftPos + 2, topPos + 2, leftPos + imageWidth - 2, topPos + imageHeight - 2, BG_COLOR);

        // 2. 槽位背景
        drawSlot(gfx, 44, 35); // In Item
        drawSlot(gfx, 116, 35); // Out Item
        drawSlot(gfx, 142, 35); // Scrap

        // 3. 流体槽背景 (In: 26,20 | Out: 80, 55 (下方))
        drawFluidTankBg(gfx, 26, 20);
        drawFluidTankBg(gfx, 80, 55); // 假设输出流体在中间下方

        // 4. 渲染流体
        renderFluid(gfx, 0, 26, 20);
        renderFluid(gfx, 1, 80, 55);

        // 5. 创新进度条：热力脉冲 (Thermal Pulse)
        // 位置: 66, 36 (输入和输出之间)
        renderThermalPulse(gfx, 66, 36);

        // 6. 温度计 (右侧)
        renderThermometer(gfx, 160, 20);

        // 玩家背包
        drawPlayerInv(gfx);
    }

    private void renderThermalPulse(GuiGraphics gfx, int x, int y) {
        int w = 44;
        int h = 16;
        int sx = leftPos + x;
        int sy = topPos + y;

        // 背景轨道
        gfx.fill(sx, sy + 6, sx + w, sy + 10, 0xFF333333);

        int total = menu.getTotalProcessTime();
        int current = menu.getProcessTime();
        if (total > 0 && current > 0) {
            float pct = (float)current / total;
            int fillW = (int)(pct * w);

            // 脉冲效果：根据时间生成正弦波颜色
            long time = System.currentTimeMillis() / 50;
            int brightness = (int)(180 + 75 * Math.sin(time * 0.2));
            int color = 0xFF000000 | (brightness << 16) | (brightness / 2 << 8); // 橙红色脉冲

            gfx.fill(sx, sy + 7, sx + fillW, sy + 9, color);

            // 头部高亮
            if (fillW > 0) {
                gfx.fill(sx + fillW - 2, sy + 5, sx + fillW, sy + 11, 0xFFFFFFFF);
            }
        }
    }

    private void renderThermometer(GuiGraphics gfx, int x, int y) {
        int sx = leftPos + x;
        int sy = topPos + y;
        int w = 8, h = 50;

        gfx.fill(sx, sy, sx + w, sy + h, 0xFF333333);

        int heat = menu.getCurrentHeat();
        // 动态归一化显示：假设最大显示范围 ±2000
        float ratio = Mth.clamp((float)heat / 2000.0f, -1.0f, 1.0f);

        int center = sy + h / 2;
        int barH = (int)(Math.abs(ratio) * (h / 2));

        if (heat > 0) {
            // 正热 (红) -> 向上
            gfx.fill(sx + 1, center - barH, sx + w - 1, center, 0xFFFF5555);
        } else if (heat < 0) {
            // 负热 (蓝) -> 向下
            gfx.fill(sx + 1, center, sx + w - 1, center + barH, 0xFF5555FF);
        }
        // 零点线
        gfx.fill(sx, center, sx + w, center + 1, 0xFFFFFFFF);
    }

    private void renderFluid(GuiGraphics gfx, int tankIdx, int x, int y) {
        int id = menu.getFluidId(tankIdx);
        int amount = menu.getFluidAmount(tankIdx);
        int cap = menu.getFluidCapacity(tankIdx);
        if (amount <= 0 || cap <= 0) return;

        Fluid fluid = BuiltInRegistries.FLUID.byId(id);
        FluidStack stack = new FluidStack(fluid, amount);
        IClientFluidTypeExtensions clientFluid = IClientFluidTypeExtensions.of(fluid);
        TextureAtlasSprite sprite = this.minecraft.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(clientFluid.getStillTexture(stack));
        int color = clientFluid.getTintColor(stack);

        int h = 50;
        int drawH = (int)((float)amount / cap * h);
        int sx = leftPos + x + 1;
        int sy = topPos + y + 1 + (h - drawH);

        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;

        RenderSystem.setShaderColor(r, g, b, 1.0f);
        gfx.blit(sx, sy, 0, 16, drawH, sprite);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private void drawSlot(GuiGraphics gfx, int x, int y) {
        gfx.fill(leftPos + x, topPos + y, leftPos + x + 18, topPos + y + 18, SLOT_BORDER);
        gfx.fill(leftPos + x + 1, topPos + y + 1, leftPos + x + 17, topPos + y + 17, SLOT_BG);
    }

    private void drawFluidTankBg(GuiGraphics gfx, int x, int y) {
        gfx.fill(leftPos + x, topPos + y, leftPos + x + 18, topPos + y + 52, SLOT_BORDER);
        gfx.fill(leftPos + x + 1, topPos + y + 1, leftPos + x + 17, topPos + y + 51, SLOT_BG);
    }

    private void drawPlayerInv(GuiGraphics gfx) {
        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++) drawSlot(gfx, 8 + c * 18, 84 + r * 18);
        for (int c = 0; c < 9; c++) drawSlot(gfx, 8 + c * 18, 142);
    }

    @Override public void render(GuiGraphics gfx, int mx, int my, float pt) {
        renderBackground(gfx, mx, my, pt);
        super.render(gfx, mx, my, pt);
        renderTooltip(gfx, mx, my);
    }
}