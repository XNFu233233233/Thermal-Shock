package com.xnfu.thermalshock.client.renderer;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.xnfu.thermalshock.ThermalShock;
import com.xnfu.thermalshock.block.SimulationChamberBlock;
import com.xnfu.thermalshock.block.entity.SimulationChamberBlockEntity;
import com.xnfu.thermalshock.block.entity.SimulationPortBlockEntity;
import com.xnfu.thermalshock.registries.ThermalShockBlocks;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

// 泛型改为 BlockEntity 以同时支持 Controller 和 Port
public class SimulationChamberRenderer implements BlockEntityRenderer<BlockEntity> {

    // 纹理定义
    private static final ResourceLocation OVERLAY_FRONT_OFF = ResourceLocation.fromNamespaceAndPath(ThermalShock.MODID, "textures/block/chamber_controller_overlay_off.png");
    private static final ResourceLocation OVERLAY_FRONT_ON = ResourceLocation.fromNamespaceAndPath(ThermalShock.MODID, "textures/block/chamber_controller_overlay_on.png");
    private static final ResourceLocation OVERLAY_PORT = ResourceLocation.fromNamespaceAndPath(ThermalShock.MODID, "textures/block/chamber_port_overlay.png");

    private final BlockRenderDispatcher blockDispatcher;

    public SimulationChamberRenderer(BlockEntityRendererProvider.Context context) {
        this.blockDispatcher = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(BlockEntity be, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int combinedLight, int combinedOverlay) {
        BlockState currentState = be.getBlockState();
        BlockState camouflageState = null;

        // 1. 获取伪装状态 (根据 BE 类型)
        if (be instanceof SimulationChamberBlockEntity controller) {
            camouflageState = controller.getCamouflageState();
        } else if (be instanceof SimulationPortBlockEntity port) {
            camouflageState = port.getCamouflageState();
        }

        // 2. 渲染伪装底色 (Camo Base)
        if (camouflageState != null && !camouflageState.isAir()) {
            poseStack.pushPose();

            // 微调缩放以解决 Z-Fighting
            poseStack.translate(0.5, 0.5, 0.5);
            poseStack.scale(1.001f, 1.001f, 1.001f);
            poseStack.translate(-0.5, -0.5, -0.5);

            // 渲染伪装方块
            blockDispatcher.renderSingleBlock(camouflageState, poseStack, bufferSource, combinedLight, combinedOverlay, net.neoforged.neoforge.client.model.data.ModelData.EMPTY, null);

            poseStack.popPose();
        }

        // 3. 渲染覆盖层 (Overlay)
        if (currentState.is(ThermalShockBlocks.SIMULATION_CHAMBER_CONTROLLER.get())) {
            renderControllerOverlay(be, currentState, poseStack, bufferSource, combinedLight);
        } else if (currentState.is(ThermalShockBlocks.SIMULATION_CHAMBER_PORT.get())) {
            renderPortOverlay(poseStack, bufferSource, combinedLight);
        }

        // 4. 渲染错误框 (仅控制器)
        if (be instanceof SimulationChamberBlockEntity controller) {
            BlockPos errorPos = controller.getErrorPos();
            if (errorPos != null) {
                renderErrorBox(controller, errorPos, poseStack, bufferSource);
            }
        }
    }

    @Override
    public AABB getRenderBoundingBox(BlockEntity be) {
        AABB box = new AABB(be.getBlockPos());
        // 如果是控制器且有错误坐标，扩展渲染盒
        if (be instanceof SimulationChamberBlockEntity controller) {
            if (controller.getErrorPos() != null) {
                box = box.minmax(new AABB(controller.getErrorPos()));
            }
        }
        return box;
    }

    // --- 错误框渲染 ---
    private void renderErrorBox(SimulationChamberBlockEntity be, BlockPos errorPos, PoseStack poseStack, MultiBufferSource bufferSource) {
        if (errorPos == null) return;
        if (be.isFormed()) return;

        poseStack.pushPose();

        BlockPos origin = be.getBlockPos();
        float dx = errorPos.getX() - origin.getX();
        float dy = errorPos.getY() - origin.getY();
        float dz = errorPos.getZ() - origin.getZ();

        poseStack.translate(dx, dy, dz);

        // 使用自定义的"穿透"渲染类型
        VertexConsumer builder = bufferSource.getBuffer(getAlwaysOnTopRenderType());

        // 绘制红色线框
        LevelRenderer.renderLineBox(poseStack, builder,
                new net.minecraft.world.phys.AABB(0, 0, 0, 1, 1, 1),
                1.0f, 0.0f, 0.0f, 1.0f); // 纯红

        poseStack.popPose();
    }

    private static RenderType getAlwaysOnTopRenderType() {
        return RenderType.create("thermalshock_error_lines",
                DefaultVertexFormat.POSITION_COLOR_NORMAL,
                VertexFormat.Mode.LINES,
                256,
                false,
                false,
                RenderType.CompositeState.builder()
                        .setShaderState(RenderStateShard.RENDERTYPE_LINES_SHADER)
                        .setLineState(new RenderStateShard.LineStateShard(java.util.OptionalDouble.of(5.0D)))
                        .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                        .setOutputState(RenderStateShard.ITEM_ENTITY_TARGET)
                        .setWriteMaskState(RenderStateShard.COLOR_DEPTH_WRITE)
                        .setCullState(RenderStateShard.NO_CULL)
                        .setDepthTestState(RenderStateShard.NO_DEPTH_TEST) // 关闭深度测试
                        .createCompositeState(false));
    }

    // --- 控制器覆盖层 ---
    private void renderControllerOverlay(BlockEntity be, BlockState state, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        Direction facing = state.getValue(SimulationChamberBlock.FACING);
        boolean isLit = state.getValue(SimulationChamberBlock.LIT);

        ResourceLocation texture = isLit ? OVERLAY_FRONT_ON : OVERLAY_FRONT_OFF;

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);

        // [修复] 使用 switch 表达式正确赋值
        float yRot = switch (facing) {
            case EAST -> 270;
            case SOUTH -> 180;
            case WEST -> 90;
            default -> 0;
        };

        poseStack.mulPose(new org.joml.Quaternionf().rotateY((float) Math.toRadians(yRot)));

        poseStack.translate(-0.5, -0.5, -0.5);

        // 渲染北面
        VertexConsumer builder = bufferSource.getBuffer(RenderType.entityCutout(texture));
        renderQuad(poseStack, builder, packedLight,
                1, 1, 0, 0,
                1f, 1f, -0.002f, // 右上
                1f, 0f, -0.002f, // 右下
                0f, 0f, -0.002f, // 左下
                0f, 1f, -0.002f  // 左上
        );

        poseStack.popPose();
    }

    // --- 接口覆盖层 ---
    private void renderPortOverlay(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        VertexConsumer builder = bufferSource.getBuffer(RenderType.entityCutout(OVERLAY_PORT));

        // 手动绘制 6 个面，确保无遗漏
        renderFace(poseStack, builder, packedLight, 0, 0, 0, Direction.NORTH);
        renderFace(poseStack, builder, packedLight, 0, 0, 0, Direction.SOUTH);
        renderFace(poseStack, builder, packedLight, 0, 0, 0, Direction.EAST);
        renderFace(poseStack, builder, packedLight, 0, 0, 0, Direction.WEST);
        renderFace(poseStack, builder, packedLight, 0, 0, 0, Direction.UP);
        renderFace(poseStack, builder, packedLight, 0, 0, 0, Direction.DOWN);
    }

    // 辅助：根据方向画一个面
    private void renderFace(PoseStack poseStack, VertexConsumer builder, int packedLight, float x, float y, float z, Direction dir) {
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);

        switch (dir) {
            case SOUTH -> poseStack.mulPose(new Quaternionf().rotateY((float) Math.toRadians(180)));
            case WEST -> poseStack.mulPose(new Quaternionf().rotateY((float) Math.toRadians(90)));
            case EAST -> poseStack.mulPose(new Quaternionf().rotateY((float) Math.toRadians(-90)));
            case UP -> poseStack.mulPose(new Quaternionf().rotateX((float) Math.toRadians(90)));
            case DOWN -> poseStack.mulPose(new Quaternionf().rotateX((float) Math.toRadians(-90)));
        }

        poseStack.translate(-0.5, -0.5, -0.5);

        // Z = -0.002 处绘制
        renderQuad(poseStack, builder, packedLight,
                1, 1, 0, 0,
                1f, 1f, -0.002f,
                1f, 0f, -0.002f,
                0f, 0f, -0.002f,
                0f, 1f, -0.002f
        );

        poseStack.popPose();
    }

    // 核心绘制方法：画一个四边形
    private void renderQuad(PoseStack poseStack, VertexConsumer builder, int packedLight,
                            float u0, float v0, float u1, float v1,
                            float x0, float y0, float z0,
                            float x1, float y1, float z1,
                            float x2, float y2, float z2,
                            float x3, float y3, float z3) {
        Matrix4f matrix = poseStack.last().pose();

        // 逆时针绘制 4 个顶点
        builder.addVertex(matrix, x0, y0, z0).setColor(255, 255, 255, 255).setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(0, 0, 1);
        builder.addVertex(matrix, x1, y1, z1).setColor(255, 255, 255, 255).setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(0, 0, 1);
        builder.addVertex(matrix, x2, y2, z2).setColor(255, 255, 255, 255).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(0, 0, 1);
        builder.addVertex(matrix, x3, y3, z3).setColor(255, 255, 255, 255).setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(0, 0, 1);
    }
}