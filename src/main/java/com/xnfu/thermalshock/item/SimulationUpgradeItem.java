package com.xnfu.thermalshock.item;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class SimulationUpgradeItem extends Item {
    public SimulationUpgradeItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        // 简略描述 (Basic Desc) - 保留原有
        tooltipComponents.add(Component.translatable("item.thermalshock.simulation_upgrade.desc")
                .withStyle(ChatFormatting.GRAY));

        if (Screen.hasShiftDown()) {
            // === 机制变更 (Mechanic Change) ===
            tooltipComponents.add(Component.empty());
            tooltipComponents.add(Component.translatable("tooltip.thermalshock.header.mechanic_change")
                    .withStyle(ChatFormatting.RED, ChatFormatting.BOLD));

            // 物理失效，接口启用
            tooltipComponents.add(Component.translatable("item.thermalshock.simulation_upgrade.detail.virtualize")
                    .withStyle(ChatFormatting.GRAY));
            tooltipComponents.add(Component.translatable("item.thermalshock.simulation_upgrade.detail.io")
                    .withStyle(ChatFormatting.GRAY));

            // === 数值覆盖 (Stat Overwrite) ===
            tooltipComponents.add(Component.empty());
            tooltipComponents.add(Component.translatable("tooltip.thermalshock.header.scaling")
                    .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));

            // 忽略体积，只看数量
            tooltipComponents.add(Component.translatable("item.thermalshock.simulation_upgrade.detail.scaling_rule")
                    .withStyle(ChatFormatting.AQUA));
            // +4 批处理
            tooltipComponents.add(Component.translatable("item.thermalshock.simulation_upgrade.detail.batch")
                    .withStyle(ChatFormatting.YELLOW));
            // 指数级加成
            tooltipComponents.add(Component.translatable("item.thermalshock.simulation_upgrade.detail.exponential")
                    .withStyle(ChatFormatting.LIGHT_PURPLE));

            // 原有的效果 (解除限制)
            tooltipComponents.add(Component.empty());
            tooltipComponents.add(Component.translatable("item.thermalshock.simulation_upgrade.effect")
                    .withStyle(ChatFormatting.GOLD));
        } else {
            // 提示按 Shift
            tooltipComponents.add(Component.translatable("tooltip.thermalshock.hold_shift")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}