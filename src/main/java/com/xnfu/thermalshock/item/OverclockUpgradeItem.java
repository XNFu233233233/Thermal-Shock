package com.xnfu.thermalshock.item;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class OverclockUpgradeItem extends Item {
    public OverclockUpgradeItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        // 基础描述
        tooltipComponents.add(Component.translatable("item.thermalshock.overclock_upgrade.desc")
                .withStyle(ChatFormatting.GRAY));

        if (Screen.hasShiftDown()) {
            tooltipComponents.add(Component.empty());
            tooltipComponents.add(Component.translatable("tooltip.thermalshock.header")
                    .withStyle(ChatFormatting.BLUE, ChatFormatting.BOLD));

            // 速度加成
            tooltipComponents.add(Component.translatable("item.thermalshock.overclock_upgrade.speed")
                    .withStyle(ChatFormatting.YELLOW));
            
            // 批处理加成
            tooltipComponents.add(Component.translatable("item.thermalshock.overclock_upgrade.batch")
                    .withStyle(ChatFormatting.GOLD));
        } else {
            tooltipComponents.add(Component.translatable("tooltip.thermalshock.hold_shift")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
