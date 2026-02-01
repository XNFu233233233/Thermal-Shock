package com.xnfu.thermalshock.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.xnfu.thermalshock.ThermalShock;
import com.xnfu.thermalshock.util.RecipeCache;
import com.xnfu.thermalshock.block.entity.MachineMode;
import com.xnfu.thermalshock.client.gui.component.CatalystBarWidget;
import com.xnfu.thermalshock.client.gui.component.HeatBarWidget;
import com.xnfu.thermalshock.client.gui.component.InfoPanelWidget;
import com.xnfu.thermalshock.network.PacketSelectRecipe;
import com.xnfu.thermalshock.network.PacketToggleLock;
import com.xnfu.thermalshock.network.PacketToggleMode;
import com.xnfu.thermalshock.recipe.AbstractSimulationRecipe;
import com.xnfu.thermalshock.registries.ThermalShockItems;
import com.xnfu.thermalshock.registries.ThermalShockRecipes;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.xnfu.thermalshock.client.gui.SimulationChamberMenu.*;

public class SimulationChamberScreen extends AbstractContainerScreen<SimulationChamberMenu> {

    private static final net.minecraft.resources.ResourceLocation GENERIC_CLUMP_ID =
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(ThermalShock.MODID, "generic_clump_processing");

    // ==========================================
    // 1. 样式与常量
    // ==========================================
    private static final int BG_COLOR = 0xFFC6C6C6;
    private static final int SLOT_BORDER = 0xFF373737;
    private static final int SLOT_BG = 0xFF8B8B8B;
    private static final int LIST_BG_COLOR = 0xFF000000;
    private static final int STATUS_X = 140;
    private static final int STATUS_Y = 10;
    public static final int BTN_RECIPE_X = 153;
    public static final int BTN_RECIPE_Y = 23;
    public static final int BTN_RECIPE_W = 16;
    public static final int BTN_RECIPE_H = 16;

    // --- 状态变量 ---
    private final List<RecipeButton> filteredButtons = new ArrayList<>();
    // [修复] 泛型改为通配符，以容纳 OverheatingRecipe 和 ThermalShockRecipe
    private List<RecipeHolder<? extends AbstractSimulationRecipe>> allRecipes;
    private MachineMode lastMode = null;

    private float scrollOffs;
    private long modeSwitchStartTime = -1;
    private static final long SWITCH_DELAY_MS = 3000;

    // --- 子组件 ---
    private Button btnToggleMode;
    private Button btnLock;
    private HeatBarWidget heatBar;
    private CatalystBarWidget catalystBar;
    private InfoPanelWidget infoPanel;


    public SimulationChamberScreen(SimulationChamberMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = INV_X;
        this.inventoryLabelY = INV_Y - 10;
    }

    // ==========================================
    // 2. 初始化 (Init)
    // ==========================================

    @Override
    protected void init() {
        super.init();

        // 使用缓存获取所有模拟配方
        this.allRecipes = new ArrayList<>();
        this.allRecipes.addAll(RecipeCache.getRecipes(MachineMode.OVERHEATING));
        this.allRecipes.addAll(RecipeCache.getRecipes(MachineMode.THERMAL_SHOCK));

        // --- 1. 锁定按钮 ---
        this.btnLock = addRenderableWidget(Button.builder(Component.literal("?"), btn -> {
                    PacketDistributor.sendToServer(new PacketToggleLock(menu.getBlockEntity().getBlockPos()));
                })
                .bounds(leftPos + 8, topPos + 22, 16, 16)
                .createNarration(supplier -> Component.translatable("tooltip.thermalshock.locked"))
                .build());

        // --- 2. 模式切换按钮 ---
        this.btnToggleMode = addRenderableWidget(Button.builder(Component.literal("M"), btn -> {
            long now = System.currentTimeMillis();
            this.modeSwitchStartTime = (this.modeSwitchStartTime > 0) ? -1 : now;
            playClickSound(this.modeSwitchStartTime > 0 ? 1.0f : 0.8f);
        }).bounds(leftPos + 153, topPos + 5, 16, 16).build());

        // --- 3. 配方按钮 (JEI 交互) ---
        addRenderableWidget(Button.builder(net.minecraft.network.chat.Component.literal("R"), btn -> {
            // 这里留空，由于我们在 JEI Plugin 注册了该区域，JEI 会拦截点击
        })
        .bounds(leftPos + BTN_RECIPE_X, topPos + BTN_RECIPE_Y, BTN_RECIPE_W, BTN_RECIPE_H)
        .build());

        // --- 4. 热量条 ---
        this.heatBar = addRenderableWidget(new HeatBarWidget(
                leftPos + HEAT_BAR_X,
                topPos + LIST_LAYOUT_Y,
                8, LIST_LAYOUT_H, menu));

        // --- 4. 催化剂条 ---
        this.catalystBar = addRenderableWidget(new CatalystBarWidget(
                leftPos + SLOT_CATALYST_X + 18 + 1,
                topPos + SLOT_CATALYST_Y,
                3, 18, menu));

        // --- 5. 信息面板 ---
        this.infoPanel = addRenderableWidget(new InfoPanelWidget(
                leftPos + 104, LIST_LAYOUT_Y + topPos,
                50, 60, menu));

        rebuildRecipeList();
    }

    // ==========================================
    // 3. 逻辑循环 (Tick)
    // ==========================================

    @Override
    public void containerTick() {
        super.containerTick();
        MachineMode currentMode = MachineMode.values()[menu.getMachineModeOrdinal()];

        // 列表刷新
        if (currentMode != this.lastMode) rebuildRecipeList();

        // 按钮状态更新
        updateModeButtonLogic(currentMode);
        updateLockButtonLogic();
    }

    private void updateModeButtonLogic(MachineMode currentMode) {
        if (this.modeSwitchStartTime > 0) {
            long elapsed = System.currentTimeMillis() - this.modeSwitchStartTime;
            if (elapsed >= SWITCH_DELAY_MS) {
                PacketDistributor.sendToServer(new PacketToggleMode(menu.getBlockEntity().getBlockPos()));
                this.modeSwitchStartTime = -1;
            } else {
                long sec = (SWITCH_DELAY_MS - elapsed) / 1000 + 1;
                btnToggleMode.setMessage(Component.literal(String.valueOf(sec)).withStyle(ChatFormatting.YELLOW));
                btnToggleMode.setTooltip(Tooltip.create(Component.translatable("gui.thermalshock.tooltip.switching", sec).withStyle(ChatFormatting.RED)));
            }
        } else {
            btnToggleMode.setMessage(Component.literal("M"));
            String modeKey = currentMode == MachineMode.OVERHEATING ? "gui.thermalshock.mode.overheating" : "gui.thermalshock.mode.shock";
            int color = currentMode == MachineMode.OVERHEATING ? 0xFF5555 : 0x55FFFF;
            Component tooltip = Component.empty()
                    .append(Component.translatable("gui.thermalshock.tooltip.switch_mode_title").withStyle(ChatFormatting.WHITE))
                    .append("\n")
                    .append(Component.translatable("gui.thermalshock.tooltip.mode_current").withStyle(ChatFormatting.GRAY))
                    .append(" ")
                    .append(Component.translatable(modeKey).withStyle(style -> style.withColor(color)));
            btnToggleMode.setTooltip(Tooltip.create(tooltip));
        }
    }

    private void updateLockButtonLogic() {
        boolean locked = menu.isLocked();
        if (btnLock != null) {
            String lockChar = locked ? "🔒" : "🔓";
            btnLock.setMessage(Component.literal(lockChar).withStyle(locked ? ChatFormatting.RED : ChatFormatting.GREEN));
            btnLock.setTooltip(Tooltip.create(Component.translatable(locked ? "tooltip.thermalshock.locked" : "tooltip.thermalshock.unlocked")));
        }
    }

    // ==========================================
    // 4. 渲染流程 (Render)
    // ==========================================

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gfx, mouseX, mouseY, partialTick);
        super.render(gfx, mouseX, mouseY, partialTick);
        renderRecipeList(gfx, mouseX, mouseY);
        renderUnifiedTooltips(gfx, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics gfx, float partialTick, int mouseX, int mouseY) {
        gfx.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF000000);
        gfx.fill(leftPos + 2, topPos + 2, leftPos + imageWidth - 2, topPos + imageHeight - 2, BG_COLOR);

        // 绘制槽位背景
        drawSlotBg(gfx, leftPos + SLOT_CATALYST_X, topPos + SLOT_CATALYST_Y);
        drawSlotBg(gfx, leftPos + SLOT_UPGRADE_X, topPos + SLOT_UPGRADE_Y);

        // [核心修改] 渲染高度透明的背景图标
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 0.12f); // 12% 透明度 (极淡，接近水印)

        // 催化剂槽图标
        if (menu.getSlot(0).getItem().isEmpty()) {
            gfx.renderItem(new ItemStack(ThermalShockItems.MATERIAL_CLUMP.get()), leftPos + SLOT_CATALYST_X + 1, topPos + SLOT_CATALYST_Y + 1);
        }

        // 升级槽图标
        if (menu.getSlot(1).getItem().isEmpty()) {
            gfx.renderItem(new ItemStack(ThermalShockItems.SIMULATION_UPGRADE.get()), leftPos + SLOT_UPGRADE_X + 1, topPos + SLOT_UPGRADE_Y + 1);
        }

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f); // 恢复颜色
        RenderSystem.disableBlend();

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) drawSlotBg(gfx, leftPos + INV_X + col * 18, topPos + INV_Y + row * 18);
        }
        for (int col = 0; col < 9; col++) drawSlotBg(gfx, leftPos + HOTBAR_X + col * 18, topPos + HOTBAR_Y);

        int lx = leftPos + LIST_LAYOUT_X;
        int ly = topPos + LIST_LAYOUT_Y;
        gfx.fill(lx - 1, ly - 1, lx + LIST_LAYOUT_W + 1, ly + LIST_LAYOUT_H + 1, 0xFF333333);
        gfx.fill(lx, ly, lx + LIST_LAYOUT_W, ly + LIST_LAYOUT_H, LIST_BG_COLOR);

        String icon = menu.isFormed() ? "✔" : "✖";
        int color = menu.isFormed() ? 0x00FF00 : 0xFF0000;
        gfx.drawString(this.font, icon, leftPos + STATUS_X, topPos + STATUS_Y, color, false);

        MachineMode mode = MachineMode.values()[menu.getMachineModeOrdinal()];
        String modeText = mode == MachineMode.OVERHEATING ? "Overheating" : "Thermal Shock";
        int modeColor = mode == MachineMode.OVERHEATING ? 0xFF5555 : 0x55FFFF;
        gfx.drawString(this.font, modeText, leftPos + 28, topPos + 26, modeColor, false);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
    }

    private void renderRecipeList(GuiGraphics gfx, int mouseX, int mouseY) {
        double scale = Minecraft.getInstance().getWindow().getGuiScale();
        int lx = leftPos + LIST_LAYOUT_X;
        int ly = topPos + LIST_LAYOUT_Y;

        RenderSystem.enableScissor((int) (lx * scale), (int) (Minecraft.getInstance().getWindow().getHeight() - (ly + LIST_LAYOUT_H) * scale), (int) (LIST_LAYOUT_W * scale), (int) (LIST_LAYOUT_H * scale));

        int contentHeight = getContentHeight();
        int maxScroll = Math.max(0, contentHeight - LIST_LAYOUT_H);
        int currentScroll = (int) (scrollOffs * maxScroll);

        gfx.pose().pushPose();
        gfx.pose().translate(lx, ly - currentScroll, 0);
        for (RecipeButton btn : filteredButtons) {
            btn.renderInList(gfx, mouseX - lx, mouseY - (ly - currentScroll));
        }
        gfx.pose().popPose();
        RenderSystem.disableScissor();

        int barX = lx + LIST_LAYOUT_W - 6;
        gfx.fill(barX, ly, barX + 6, ly + LIST_LAYOUT_H, 0xFF333333);

        int barH = maxScroll > 0 ? (int) ((float) LIST_LAYOUT_H / contentHeight * LIST_LAYOUT_H) : LIST_LAYOUT_H;
        int barY = ly + (int) (scrollOffs * (LIST_LAYOUT_H - barH));
        int thumbColor = maxScroll > 0 ? 0xFFAAAAAA : 0xFF555555;

        gfx.fill(barX, barY, barX + 6, barY + barH, thumbColor);
    }

    // ==========================================
    // 5. 工具提示
    // ==========================================

    private void renderUnifiedTooltips(GuiGraphics gfx, int mouseX, int mouseY) {
        List<Component> tooltip = new ArrayList<>();
        boolean shift = hasShiftDown();

        heatBar.appendHoverText(tooltip, shift);
        catalystBar.appendHoverText(tooltip, shift);
        infoPanel.appendHoverText(tooltip, mouseX, mouseY, shift);

        if (tooltip.isEmpty() && isMouseOverList(mouseX, mouseY)) {
            int currentScroll = (int) (scrollOffs * Math.max(0, getContentHeight() - LIST_LAYOUT_H));
            double relX = mouseX - (leftPos + LIST_LAYOUT_X);
            double relY = mouseY - (topPos + LIST_LAYOUT_Y) + currentScroll;
            for (RecipeButton btn : filteredButtons) {
                if (btn.isMouseOver(relX, relY)) {
                    // [Fix] GenericClumpButton 不显示悬浮配方
                    if (shift && !(btn instanceof GenericClumpButton)) {
                         gfx.renderTooltip(font, btn.getTooltipComponents(), 
                             Optional.of(new com.xnfu.thermalshock.client.gui.tooltip.RecipePreviewTooltip(btn.holder.value())), 
                             mouseX, mouseY);
                    } else {
                        // Standard tooltip
                        tooltip.addAll(btn.getTooltipComponents());
                    }
                    break;
                }
            }
        }

        if (tooltip.isEmpty() && isHovering(28, 26, 60, 9, mouseX - leftPos, mouseY - topPos)) {
            MachineMode mode = MachineMode.values()[menu.getMachineModeOrdinal()];
            String descKey = mode == MachineMode.OVERHEATING
                    ? "gui.thermalshock.tooltip.mode_desc.overheating"
                    : "gui.thermalshock.tooltip.mode_desc.shock";
            tooltip.add(Component.translatable(descKey).withStyle(ChatFormatting.GRAY));
        }

        if (tooltip.isEmpty() && isHovering(STATUS_X, STATUS_Y, 12, 12, mouseX - leftPos, mouseY - topPos)) {
            tooltip.add(Component.translatable(menu.isFormed() ? "gui.thermalshock.status.valid" : "gui.thermalshock.status.invalid")
                    .withStyle(menu.isFormed() ? ChatFormatting.GREEN : ChatFormatting.RED));

            if (menu.isFormed()) {
                var be = menu.getBlockEntity();
                net.minecraft.core.BlockPos min = be.getMinPos();
                net.minecraft.core.BlockPos max = be.getMaxPos();
                int dx = max.getX() - min.getX() + 1;
                int dy = max.getY() - min.getY() + 1;
                int dz = max.getZ() - min.getZ() + 1;
                String sizeStr = dx + "x" + dy + "x" + dz;
                Component casingName = be.getCamouflageState().getBlock().getName();
                tooltip.add(Component.translatable("gui.thermalshock.status.detail.size_casing", sizeStr, casingName).withStyle(ChatFormatting.WHITE));
                tooltip.add(Component.translatable("gui.thermalshock.status.detail.interior", menu.getStructVolume()).withStyle(ChatFormatting.GRAY));
                tooltip.add(Component.translatable("gui.thermalshock.status.detail.casing", menu.getMaxHeatRate(), menu.getMaxColdRate()).withStyle(ChatFormatting.YELLOW));
                tooltip.add(Component.translatable("gui.thermalshock.status.detail.max_batch", menu.getbatchSize()).withStyle(ChatFormatting.AQUA));
            } else {
                tooltip.add(Component.translatable("gui.thermalshock.status.help").withStyle(ChatFormatting.GRAY));
            }
        }

        if (!tooltip.isEmpty()) gfx.renderTooltip(font, tooltip, Optional.empty(), mouseX, mouseY);
        else this.renderTooltip(gfx, mouseX, mouseY);
    }

    // ==========================================
    // 5. 工具提示
    // ==========================================

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (isMouseOverList(mouseX, mouseY)) {
            int maxScroll = Math.max(0, getContentHeight() - LIST_LAYOUT_H);
            if (maxScroll > 0) {
                float delta = (float) (18.0 / maxScroll);
                this.scrollOffs = Mth.clamp(this.scrollOffs - (float) scrollY * delta, 0.0f, 1.0f);
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isMouseOverList(mouseX, mouseY)) {
            int currentScroll = (int) (scrollOffs * Math.max(0, getContentHeight() - LIST_LAYOUT_H));
            double relX = mouseX - (leftPos + LIST_LAYOUT_X);
            double relY = mouseY - (topPos + LIST_LAYOUT_Y) + currentScroll;
            for (RecipeButton btn : filteredButtons) {
                if (btn.isMouseOver(relX, relY)) {
                    btn.onPress.run();
                    playClickSound(1.0f);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void rebuildRecipeList() {
        this.filteredButtons.clear();
        this.scrollOffs = 0;
        MachineMode currentMode = MachineMode.values()[menu.getMachineModeOrdinal()];
        this.lastMode = currentMode;

        int cols = 4;
        int startX = 2;
        int btnX = startX, btnY = 0, col = 0;

        // [新增] 只有在过热模式下，显示通用 Clump 按钮
        if (currentMode == MachineMode.OVERHEATING) {
            ItemStack icon = new ItemStack(com.xnfu.thermalshock.registries.ThermalShockItems.MATERIAL_CLUMP.get());
            // 发送通用 ID 包
            RecipeButton genericBtn = new GenericClumpButton(btnX + col * 18, btnY, icon, () -> {
                PacketDistributor.sendToServer(new PacketSelectRecipe(menu.getBlockEntity().getBlockPos(), GENERIC_CLUMP_ID));
            });
            this.filteredButtons.add(genericBtn);

            col++;
            if (col >= cols) { col = 0; btnY += 18; }
        }

        for (RecipeHolder<? extends AbstractSimulationRecipe> holder : allRecipes) {
            // [修复] 增加判空保护，防止 RecipeCache 返回 null (虽然概率极低)
            if (holder == null || holder.value() == null) continue;
            
            AbstractSimulationRecipe recipe = holder.value();

            if (recipe.getMachineMode() != currentMode) continue;

            // [核心过滤] 如果是具体的团块处理配方，直接跳过，不在列表显示 (由通用按钮接管)
            if (recipe instanceof com.xnfu.thermalshock.recipe.ClumpProcessingRecipe) continue;

            RecipeButton btn = new RecipeButton(btnX + col * 18, btnY, holder, () -> {
                PacketDistributor.sendToServer(new PacketSelectRecipe(menu.getBlockEntity().getBlockPos(), holder.id()));
            });
            this.filteredButtons.add(btn);

            col++;
            if (col >= cols) {
                col = 0;
                btnY += 18;
            }
        }
    }

    private void drawSlotBg(GuiGraphics gfx, int x, int y) {
        gfx.fill(x, y, x + 18, y + 18, SLOT_BORDER);
        gfx.fill(x + 1, y + 1, x + 17, y + 17, SLOT_BG);
    }

    private int getContentHeight() {
        int cols = (LIST_LAYOUT_W - 10) / 18;
        return (filteredButtons.size() / cols + 1) * 18;
    }

    private boolean isMouseOverList(double x, double y) {
        int lx = leftPos + LIST_LAYOUT_X;
        int ly = topPos + LIST_LAYOUT_Y;
        return x >= lx && x <= lx + LIST_LAYOUT_W && y >= ly && y <= ly + LIST_LAYOUT_H;
    }

    private boolean isHovering(int x, int y, int w, int h, int relX, int relY) {
        return relX >= x && relX <= x + w && relY >= y && relY <= y + h;
    }

    private void playClickSound(float pitch) {
        Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, pitch));
    }

    class RecipeButton {
        final int x, y;
        final int w = 18, h = 18;
        final RecipeHolder<? extends AbstractSimulationRecipe> holder;
        final ItemStack icon;
        final Runnable onPress;

        public RecipeButton(int x, int y, RecipeHolder<? extends AbstractSimulationRecipe> holder, Runnable onPress) {
            this.x = x;
            this.y = y;
            this.holder = holder;
            this.onPress = onPress;
            // [修复] 判空保护：GenericClumpButton 会传入 null
            if (holder != null) {
                this.icon = holder.value().getResultItem(Minecraft.getInstance().level.registryAccess());
            } else {
                this.icon = ItemStack.EMPTY;
            }
        }

        public void renderInList(GuiGraphics gfx, int mouseX, int mouseY) {
            boolean hovered = isMouseOver(mouseX, mouseY);
            // [修复] 判空保护
            boolean selected = holder != null && holder.id().equals(menu.getBlockEntity().getSelectedRecipeId());
            int color = selected ? 0xFFDDAA00 : (hovered ? 0xFF888888 : 0xFF444444);

            gfx.fill(x, y, x + w, y + h, 0xFF000000);
            gfx.fill(x + 1, y + 1, x + w - 1, y + h - 1, color);

            // 渲染物品图标
            gfx.renderItem(icon, x + 1, y + 1);
            gfx.renderItemDecorations(font, icon, x + 1, y + 1);
        }

        public boolean isMouseOver(double mX, double mY) {
            return mX >= x && mX < x + w && mY >= y && mY < y + h;
        }

        public List<Component> getTooltipComponents() {
            if (icon.isEmpty()) return new ArrayList<>();
            Minecraft mc = Minecraft.getInstance();
            return new ArrayList<>(Screen.getTooltipFromItem(mc, icon));
        }
    }

    class GenericClumpButton extends RecipeButton {
        private final ItemStack customIcon;

        public GenericClumpButton(int x, int y, ItemStack icon, Runnable onPress) {
            // [修复] 直接传 null，RecipeButton 现在已支持 null holder
            super(x, y, null, onPress);
            this.customIcon = icon;
        }

        @Override
        public void renderInList(GuiGraphics gfx, int mouseX, int mouseY) {
            boolean selected = GENERIC_CLUMP_ID.equals(menu.getBlockEntity().getSelectedRecipeId());
            boolean hovered = isMouseOver(mouseX, mouseY);
            int color = selected ? 0xFFDDAA00 : (hovered ? 0xFF888888 : 0xFF444444);

            gfx.fill(x, y, x + w, y + h, 0xFF000000);
            gfx.fill(x + 1, y + 1, x + w - 1, y + h - 1, color);
            gfx.renderItem(customIcon, x + 1, y + 1);
        }

        @Override
        public List<Component> getTooltipComponents() {
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.translatable("gui.thermalshock.btn.generic_clump.title").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            tooltip.add(Component.translatable("gui.thermalshock.btn.generic_clump.desc1").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("gui.thermalshock.btn.generic_clump.desc2").withStyle(ChatFormatting.YELLOW));
            return tooltip;
        }
    }
}