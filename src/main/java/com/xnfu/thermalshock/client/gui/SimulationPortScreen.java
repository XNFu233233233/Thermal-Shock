package com.xnfu.thermalshock.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.xnfu.thermalshock.block.entity.PortMode;
import com.xnfu.thermalshock.network.PacketTogglePortMode;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.PacketDistributor;

public class SimulationPortScreen extends AbstractContainerScreen<SimulationPortMenu> {

    // === 样式 ===
    private static final int BG_COLOR = 0xFFC6C6C6;
    private static final int SLOT_BG = 0xFF8B8B8B;
    private static final int SLOT_BORDER = 0xFF373737;

    // === 滚动条 (紧贴列表右侧) ===
    private static final int SCROLL_X = 90;
    private static final int SCROLL_Y = 20;
    private static final int SCROLL_W = 8;
    private static final int SCROLL_H = 54;

    // === 流体条 (右移) ===
    private static final int FLUID_X_START = 108;
    private static final int FLUID_Y = 20;
    private static final int FLUID_W = 10;
    private static final int FLUID_H = 54;
    private static final int FLUID_GAP = 14;

    // === 状态 ===
    private float scrollOffs = 0;
    private boolean isScrolling = false;

    private Button btnMode;
    private Button btnExpand;

    public SimulationPortScreen(SimulationPortMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = 72;
    }

    @Override
    protected void init() {
        super.init();

        // 1. 模式按钮: 左侧, 20x20
        this.btnMode = addRenderableWidget(Button.builder(Component.literal("M"), btn -> {
            PacketDistributor.sendToServer(new PacketTogglePortMode(menu.be.getBlockPos()));
        }).bounds(leftPos + 8, topPos + 35, 20, 20).build());

        // 2. 展开按钮: 右上角, 12x12
        this.btnExpand = addRenderableWidget(Button.builder(Component.literal("≡"), btn -> {
            boolean newState = !menu.isExpanded();
            menu.setExpanded(newState);
            btn.setMessage(Component.literal(newState ? "×" : "≡"));
            this.scrollOffs = 0;
        }).bounds(leftPos + 156, topPos + 6, 12, 12).build());
    }

    @Override
    public void containerTick() {
        super.containerTick();

        // 展开时隐藏模式按钮
        this.btnMode.visible = !menu.isExpanded();

        PortMode mode = menu.getPortMode();
        btnMode.setMessage(Component.literal(mode.name().substring(0, 1)));
        btnMode.setTooltip(Tooltip.create(
                Component.literal("Mode: " + mode.getSerializedName().toUpperCase())
                        .withStyle(style -> style.withColor(mode.getColor()))
        ));
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gfx, mouseX, mouseY, partialTick);
        super.render(gfx, mouseX, mouseY, partialTick);
        this.renderTooltip(gfx, mouseX, mouseY);
        renderFluidTooltips(gfx, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics gfx, float partialTick, int mouseX, int mouseY) {
        gfx.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF000000);
        gfx.fill(leftPos + 2, topPos + 2, leftPos + imageWidth - 2, topPos + imageHeight - 2, BG_COLOR);

        if (menu.isExpanded()) {
            // === 展开模式 (9x3) ===
            int startX = leftPos + SimulationPortMenu.EXPANDED_START_X;
            int startY = topPos + SimulationPortMenu.EXPANDED_START_Y;
            for (int i = 0; i < 27; i++) {
                int col = i % 9;
                int row = i / 9;
                drawSlotBg(gfx, startX + col * 18, startY + row * 18);
            }
        } else {
            // === 默认模式 (3x3) ===
            int startX = leftPos + SimulationPortMenu.SLOT_START_X;
            int startY = topPos + SimulationPortMenu.SLOT_START_Y;
            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 3; c++) {
                    drawSlotBg(gfx, startX + c * 18, startY + r * 18);
                }
            }
            drawScrollBar(gfx);
            drawFluidBars(gfx);

            PortMode mode = menu.getPortMode();
            gfx.fill(leftPos + 30, topPos + 40, leftPos + 34, topPos + 44, mode.getColor());
        }

        drawPlayerInvBg(gfx);
    }

    private void drawScrollBar(GuiGraphics gfx) {
        int sx = leftPos + SCROLL_X;
        int sy = topPos + SCROLL_Y;

        gfx.fill(sx, sy, sx + SCROLL_W, sy + SCROLL_H, 0xFF333333);

        int range = 6;
        int thumbH = (int) (SCROLL_H / (float)(range + 1) * 2.5f);
        if (thumbH < 8) thumbH = 8;
        if (thumbH > SCROLL_H) thumbH = SCROLL_H;

        int thumbY = sy + (int) (scrollOffs * (SCROLL_H - thumbH));
        gfx.fill(sx, thumbY, sx + SCROLL_W, thumbY + thumbH, 0xFF888888);
        gfx.fill(sx, thumbY, sx + SCROLL_W - 1, thumbY + thumbH - 1, 0xFFC6C6C6);
    }

    private void drawFluidBars(GuiGraphics gfx) {
        for (int i = 0; i < 3; i++) {
            int fx = leftPos + FLUID_X_START + i * FLUID_GAP;
            int fy = topPos + FLUID_Y;
            gfx.fill(fx, fy, fx + FLUID_W, fy + FLUID_H, 0xFF333333);
            
            // 获取流体数据
            int fluidId = menu.getFluidId(i);
            int amount = menu.getFluidAmount(i);
            int cap = menu.getFluidCapacity(i);
            
            if (amount > 0 && cap > 0) {
                Fluid fluid = BuiltInRegistries.FLUID.byId(fluidId);
                if (fluid == net.minecraft.world.level.material.Fluids.EMPTY) continue;

                FluidStack stack = new FluidStack(fluid, amount);
                IClientFluidTypeExtensions clientFluid = IClientFluidTypeExtensions.of(fluid);
                TextureAtlasSprite sprite = this.minecraft.getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(clientFluid.getStillTexture(stack));
                int color = clientFluid.getTintColor(stack);

                // 设置颜色
                float r = ((color >> 16) & 0xFF) / 255f;
                float g = ((color >> 8) & 0xFF) / 255f;
                float b = (color & 0xFF) / 255f;
                float a = ((color >> 24) & 0xFF) / 255f;
                RenderSystem.setShaderColor(r, g, b, a);

                // 计算高度
                int height = (int)((float)amount / cap * FLUID_H);
                int renderY = fy + FLUID_H - height;
                int width = FLUID_W;

                // 循环绘制 (Tiling)
                int yOffset = 0;
                while (yOffset < height) {
                    int drawHeight = Math.min(height - yOffset, 16);
                    // 从底部向上绘制
                    int drawY = fy + FLUID_H - yOffset - drawHeight;
                    
                    // 这里的 blit 参数：x, y, z, width, height, sprite
                    gfx.blit(fx, drawY, 0, width, drawHeight, sprite);
                    yOffset += drawHeight;
                }
                
                // 重置颜色
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            }
        }
    }

    private void renderFluidTooltips(GuiGraphics gfx, int mouseX, int mouseY) {
        if (menu.isExpanded()) return;

        for (int i = 0; i < 3; i++) {
            int x = leftPos + FLUID_X_START + i * FLUID_GAP;
            if (isHovering(x - leftPos, FLUID_Y, FLUID_W, FLUID_H, mouseX, mouseY)) {
                int id = menu.getFluidId(i);
                int amount = menu.getFluidAmount(i);
                int cap = menu.getFluidCapacity(i);

                Fluid f = BuiltInRegistries.FLUID.byId(id);
                Component name = (amount > 0) ? f.getFluidType().getDescription() : Component.literal("Empty");

                // 使用语言文件键: gui.thermalshock.tooltip.fluid=%s: %s / %s mB
                gfx.renderTooltip(font, Component.translatable("gui.thermalshock.tooltip.fluid", name, amount, cap), mouseX, mouseY);
            }
        }
    }

    private void drawSlotBg(GuiGraphics gfx, int x, int y) {
        gfx.fill(x, y, x + 18, y + 18, SLOT_BORDER);
        gfx.fill(x + 1, y + 1, x + 17, y + 17, SLOT_BG);
    }

    private void drawPlayerInvBg(GuiGraphics gfx) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) drawSlotBg(gfx, leftPos + 8 + col * 18, topPos + 84 + row * 18);
        }
        for (int col = 0; col < 9; col++) drawSlotBg(gfx, leftPos + 8 + col * 18, topPos + 142);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!menu.isExpanded() && button == 0) {
            int sx = leftPos + SCROLL_X;
            int sy = topPos + SCROLL_Y;
            if (mouseX >= sx && mouseX <= sx + SCROLL_W && mouseY >= sy && mouseY <= sy + SCROLL_H) {
                isScrolling = true;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) isScrolling = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isScrolling && !menu.isExpanded()) {
            int sy = topPos + SCROLL_Y;
            int rangeH = SCROLL_H - 15;
            float val = (float) (mouseY - sy - 7.5f) / rangeH;
            val = Mth.clamp(val, 0.0f, 1.0f);
            this.scrollOffs = val;
            updateScroll();
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!menu.isExpanded()) {
            float step = 1.0f / 6.0f;
            this.scrollOffs = Mth.clamp(this.scrollOffs - (float)scrollY * step, 0.0f, 1.0f);
            updateScroll();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void updateScroll() {
        int row = Math.round(scrollOffs * 6);
        menu.setScrollOffset(row);
    }
}