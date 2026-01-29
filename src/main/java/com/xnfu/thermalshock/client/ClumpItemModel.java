package com.xnfu.thermalshock.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.xnfu.thermalshock.data.ClumpInfo;
import com.xnfu.thermalshock.registries.ThermalShockDataComponents;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class ClumpItemModel implements BakedModel {
    private final BakedModel originalModel;
    private final boolean hasData;
    private final boolean isGuiContext;

    public ClumpItemModel(BakedModel originalModel) {
        this(originalModel, false, false);
    }

    private ClumpItemModel(BakedModel originalModel, boolean hasData, boolean isGuiContext) {
        this.originalModel = originalModel;
        this.hasData = hasData;
        this.isGuiContext = isGuiContext;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        if (isGuiContext && hasData && Screen.hasShiftDown()) {
            return Collections.emptyList();
        }
        return originalModel.getQuads(state, side, rand);
    }

    @Override
    public BakedModel applyTransform(ItemDisplayContext context, PoseStack poseStack, boolean applyLeftHandTransform) {
        boolean isGui = (context == ItemDisplayContext.GUI);
        BakedModel transformedOriginal = originalModel.applyTransform(context, poseStack, applyLeftHandTransform);
        return new ClumpItemModel(transformedOriginal, this.hasData, isGui);
    }

    @Override
    public ItemOverrides getOverrides() {
        return new ItemOverrides() {
            @Override
            public BakedModel resolve(BakedModel model, ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
                ClumpInfo info = stack.get(ThermalShockDataComponents.TARGET_OUTPUT);
                boolean dataPresent = info != null && !info.result().isEmpty();
                BakedModel resolvedOriginal = originalModel.getOverrides().resolve(originalModel, stack, level, entity, seed);
                return new ClumpItemModel(resolvedOriginal != null ? resolvedOriginal : originalModel, dataPresent, false);
            }
        };
    }

    // --- 委托方法 (Boilerplate) ---
    // [修复] 添加 SuppressWarnings 压制弃用警告 (这是实现接口必须的)
    @Override public boolean useAmbientOcclusion() { return originalModel.useAmbientOcclusion(); }
    @Override public boolean isGui3d() { return originalModel.isGui3d(); }
    @Override public boolean usesBlockLight() { return originalModel.usesBlockLight(); }
    @Override public boolean isCustomRenderer() { return originalModel.isCustomRenderer(); }
    @Deprecated @Override public TextureAtlasSprite getParticleIcon() { return originalModel.getParticleIcon(); }
    @Deprecated @Override public ItemTransforms getTransforms() { return originalModel.getTransforms(); }
}