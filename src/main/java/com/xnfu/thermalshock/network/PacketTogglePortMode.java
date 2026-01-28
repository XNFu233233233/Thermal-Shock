package com.xnfu.thermalshock.network;

import com.xnfu.thermalshock.ThermalShock;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PacketTogglePortMode(BlockPos pos) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<PacketTogglePortMode> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ThermalShock.MODID, "toggle_port_mode"));

    public static final StreamCodec<ByteBuf, PacketTogglePortMode> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, PacketTogglePortMode::pos,
            PacketTogglePortMode::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(PacketTogglePortMode payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                if (player.distanceToSqr(payload.pos.getCenter()) < 64.0 &&
                        // [修改] 现在检查的是 Port BE
                        player.level().getBlockEntity(payload.pos) instanceof com.xnfu.thermalshock.block.entity.SimulationPortBlockEntity be) {
                    be.cyclePortMode();
                }
            }
        });
    }
}