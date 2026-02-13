package com.xnfu.thermalshock.item;

import com.xnfu.thermalshock.data.ClumpInfo;
import com.xnfu.thermalshock.registries.ThermalShockDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class MaterialClumpItem extends Item {
    public MaterialClumpItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        ClumpInfo info = stack.get(ThermalShockDataComponents.TARGET_OUTPUT);

        if (info == null) {
            return Component.translatable("item.thermalshock.material_clump.empty")
                    .withStyle(ChatFormatting.GRAY);
        }

        ItemStack result = info.createStack();
        if (result.isEmpty()) {
            return Component.translatable("item.thermalshock.material_clump.empty")
                    .withStyle(ChatFormatting.GRAY);
        }

        return Component.translatable("item.thermalshock.material_clump.filled",
                        result.getHoverName())
                .withStyle(ChatFormatting.GOLD);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        ClumpInfo info = stack.get(ThermalShockDataComponents.TARGET_OUTPUT);
        if (info != null && !info.createStack().isEmpty()) {
            tooltipComponents.add(Component.translatable("tooltip.thermalshock.clump_instruction")
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            if (Screen.hasShiftDown()) {
               // [Modified] Removed extra tooltip details as they are handled by SimulationChamberScreen recipe preview.
            }
        }
    }
}