package com.xnfu.thermalshock.client;

import com.xnfu.thermalshock.ThermalShock;
import com.xnfu.thermalshock.client.gui.RecipePreviewClientTooltipComponent;
import com.xnfu.thermalshock.client.gui.SimulationChamberScreen;
import com.xnfu.thermalshock.client.gui.SimulationPortScreen;
import com.xnfu.thermalshock.client.gui.ThermalConverterScreen;
import com.xnfu.thermalshock.client.gui.ThermalSourceScreen;
import com.xnfu.thermalshock.client.gui.tooltip.RecipePreviewTooltip;
import com.xnfu.thermalshock.client.renderer.SimulationChamberRenderer;
import com.xnfu.thermalshock.data.ClumpInfo;
import com.xnfu.thermalshock.registries.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.RegisterItemDecorationsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = ThermalShock.MODID, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ThermalShockBlockEntities.CHAMBER_CONTROLLER_BE.get(), SimulationChamberRenderer::new);
        event.registerBlockEntityRenderer(ThermalShockBlockEntities.SIMULATION_PORT_BE.get(), SimulationChamberRenderer::new);
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ThermalShockMenus.SIMULATION_CHAMBER_MENU.get(), SimulationChamberScreen::new);
        event.register(ThermalShockMenus.SIMULATION_PORT_MENU.get(), SimulationPortScreen::new);
        event.register(ThermalShockMenus.THERMAL_SOURCE_MENU.get(), ThermalSourceScreen::new);
        event.register(ThermalShockMenus.THERMAL_CONVERTER_MENU.get(), ThermalConverterScreen::new);
    }

    @SubscribeEvent
    public static void registerClientTooltips(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(RecipePreviewTooltip.class, RecipePreviewClientTooltipComponent::new);
    }

    @SubscribeEvent
    public static void modifyBakingResult(ModelEvent.ModifyBakingResult event) {
        ModelResourceLocation location = new ModelResourceLocation(
                ThermalShockItems.MATERIAL_CLUMP.getId(),
                "inventory"
        );

        if (event.getModels().containsKey(location)) {
            BakedModel original = event.getModels().get(location);
            event.getModels().put(location, new ClumpItemModel(original));
        }
    }

    public static boolean SUPPRESS_CLUMP_SHIFT_RENDER = false;

    @SubscribeEvent
    public static void registerItemDecorators(RegisterItemDecorationsEvent event) {
        event.register(ThermalShockItems.MATERIAL_CLUMP.get(), (guiGraphics, font, stack, x, y) -> {
            ClumpInfo info = stack.get(ThermalShockDataComponents.TARGET_OUTPUT);
            if (info == null) return false;

            ItemStack subStack = info.createStack();
            if (subStack.isEmpty()) return false;

            guiGraphics.pose().pushPose();

            if (Screen.hasShiftDown() && !SUPPRESS_CLUMP_SHIFT_RENDER) {
                guiGraphics.pose().translate(x, y, 100);
                guiGraphics.renderItem(subStack, 0, 0);
            } else {
                guiGraphics.pose().translate(x + 8, y, 100);
                guiGraphics.pose().scale(0.5f, 0.5f, 1.0f);
                guiGraphics.renderItem(subStack, 0, 0);
            }

            guiGraphics.pose().popPose();
            return true;
        });
    }
}