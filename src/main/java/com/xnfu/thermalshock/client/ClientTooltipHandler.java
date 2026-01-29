package com.xnfu.thermalshock.client;

import com.xnfu.thermalshock.ThermalShock;
import com.xnfu.thermalshock.data.CasingData;
import com.xnfu.thermalshock.data.CatalystData;
import com.xnfu.thermalshock.data.ColdSourceData;
import com.xnfu.thermalshock.data.HeatSourceData;
import com.xnfu.thermalshock.registries.ThermalShockDataMaps;
import com.xnfu.thermalshock.registries.ThermalShockTags;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;

import java.util.Optional;

@EventBusSubscriber(modid = ThermalShock.MODID, value = Dist.CLIENT)
public class ClientTooltipHandler {

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();

        // 1. 获取 Item 的 Holder (用于查询 Catalyst)
        var itemHolder = BuiltInRegistries.ITEM.wrapAsHolder(stack.getItem());
        CatalystData catalystData = itemHolder.getData(ThermalShockDataMaps.CATALYST_PROPERTY);

        // 2. 尝试获取关联的 Block (方块或流体源方块)
        Block block = null;
        boolean isWater = false; // 特殊标记

        if (stack.getItem() instanceof BlockItem blockItem) {
            block = blockItem.getBlock();
        } else {
            // 尝试检查是否为流体容器 (如水桶)
            Optional<FluidStack> fluidOpt = FluidUtil.getFluidContained(stack);
            if (fluidOpt.isPresent()) {
                Fluid fluid = fluidOpt.get().getFluid();
                // 获取流体对应的默认方块状态
                block = fluid.defaultFluidState().createLegacyBlock().getBlock();
                // 检查是否为水 (用于硬编码 0 度)
                if (fluid.is(FluidTags.WATER)) {
                    isWater = true;
                }
            }
        }

        // 3. 查询 Block 属性
        CasingData casingData = null;
        HeatSourceData heatData = null;
        ColdSourceData coldData = null;
        boolean isVent = false;
        boolean isAccess = false;

        if (block != null) {
            var blockHolder = BuiltInRegistries.BLOCK.wrapAsHolder(block);
            casingData = blockHolder.getData(ThermalShockDataMaps.CASING_PROPERTY);
            heatData = blockHolder.getData(ThermalShockDataMaps.HEAT_SOURCE_PROPERTY);
            coldData = blockHolder.getData(ThermalShockDataMaps.COLD_SOURCE_PROPERTY);
            isVent = blockHolder.is(ThermalShockTags.VENTS);
            isAccess = blockHolder.is(ThermalShockTags.CASING_ACCESS);
        }

        // 4. 判定是否需要显示
        // 注意：如果是水桶，虽然 coldData 为空，但 isWater 为真，也需要显示
        if (catalystData == null && casingData == null && heatData == null && coldData == null && !isVent && !isAccess && !isWater) {
            return;
        }

        // 5. Shift 键检测
        if (!Screen.hasShiftDown()) {
            event.getToolTip().add(Component.translatable("tooltip.thermalshock.hold_shift")
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            return;
        }

        // 6. 添加标题
        event.getToolTip().add(Component.translatable("tooltip.thermalshock.header")
                .withStyle(ChatFormatting.BLUE, ChatFormatting.BOLD));

        // === A. 外壳属性 ===
        if (casingData != null) {
            // [修复] 使用 maxHeatRate 和 maxColdRate
            event.getToolTip().add(Component.translatable("tooltip.thermalshock.rate_limit",
                    casingData.maxHeatRate(), casingData.maxColdRate()).withStyle(ChatFormatting.RED));
            event.getToolTip().add(Component.translatable("tooltip.thermalshock.efficiency",
                    String.format("%.0f%%", casingData.efficiency() * 100)).withStyle(ChatFormatting.GREEN));
        }

        // === B. 热源属性 (高温) ===
        if (heatData != null) {
            event.getToolTip().add(Component.translatable("tooltip.thermalshock.heat_source_rate",
                    heatData.heatPerTick()).withStyle(ChatFormatting.GOLD));
        }

        // === C. 冷源属性 (低温) ===
        if (coldData != null) {
            event.getToolTip().add(Component.translatable("tooltip.thermalshock.cold_source_rate",
                    coldData.coolingPerTick()).withStyle(ChatFormatting.AQUA));
        } else if (isWater) {
            // 水的硬编码提示
            event.getToolTip().add(Component.translatable("tooltip.thermalshock.cold_source_rate",
                    0).withStyle(ChatFormatting.AQUA));
        }

        // === D. 催化剂属性 ===
        if (catalystData != null) {
            event.getToolTip().add(Component.translatable("tooltip.thermalshock.catalyst_yield",
                    String.format("+%.0f%%", catalystData.bonusYield() * 100)).withStyle(ChatFormatting.LIGHT_PURPLE));
            event.getToolTip().add(Component.translatable("tooltip.thermalshock.catalyst_buffer",
                    String.format("%.1f", catalystData.bufferAmount())).withStyle(ChatFormatting.LIGHT_PURPLE));
        }

        // === E. 功能组件 ===
        if (isVent) {
            event.getToolTip().add(Component.translatable("tooltip.thermalshock.type_vent")
                    .withStyle(ChatFormatting.AQUA));
        }
        if (isAccess) {
            event.getToolTip().add(Component.translatable("tooltip.thermalshock.type_access")
                    .withStyle(ChatFormatting.GREEN));
        }
    }
}