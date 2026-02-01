package com.xnfu.thermalshock.network;

import com.xnfu.thermalshock.ThermalShock;
import com.xnfu.thermalshock.block.entity.SimulationChamberBlockEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PacketToggleMode(BlockPos pos) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<PacketToggleMode> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ThermalShock.MODID, "toggle_mode"));

    public static final StreamCodec<ByteBuf, PacketToggleMode> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, PacketToggleMode::pos,
            PacketToggleMode::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(PacketToggleMode payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                handleServer(payload, player);
            }
        });
    }

    private static void handleServer(PacketToggleMode payload, ServerPlayer player) {
        if (player.level().isLoaded(payload.pos) && 
            player.distanceToSqr(payload.pos.getCenter()) < 64.0 &&
            player.level().getBlockEntity(payload.pos) instanceof SimulationChamberBlockEntity be) {
            be.requestModeChange();
        }
    }
}