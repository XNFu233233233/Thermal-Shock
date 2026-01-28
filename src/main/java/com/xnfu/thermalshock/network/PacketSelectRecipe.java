package com.xnfu.thermalshock.network;

import com.xnfu.thermalshock.ThermalShock;
import com.xnfu.thermalshock.block.entity.SimulationChamberBlockEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PacketSelectRecipe(BlockPos pos, ResourceLocation recipeId) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<PacketSelectRecipe> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ThermalShock.MODID, "select_recipe"));

    public static final StreamCodec<ByteBuf, PacketSelectRecipe> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, PacketSelectRecipe::pos,
            ResourceLocation.STREAM_CODEC, PacketSelectRecipe::recipeId,
            PacketSelectRecipe::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PacketSelectRecipe payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                if (player.level().getBlockEntity(payload.pos) instanceof SimulationChamberBlockEntity be) {
                    be.setSelectedRecipe(payload.recipeId);
                }
            }
        });
    }
}