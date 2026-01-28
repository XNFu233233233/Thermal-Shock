package com.xnfu.thermalshock.network;

import com.xnfu.thermalshock.ThermalShock;
import com.xnfu.thermalshock.block.entity.ThermalSourceBlockEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PacketSetTargetHeat(BlockPos pos, int targetHeat) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<PacketSetTargetHeat> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ThermalShock.MODID, "set_target_heat"));

    public static final StreamCodec<ByteBuf, PacketSetTargetHeat> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, PacketSetTargetHeat::pos,
            ByteBufCodecs.VAR_INT, PacketSetTargetHeat::targetHeat,
            PacketSetTargetHeat::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(PacketSetTargetHeat payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                if (player.level().getBlockEntity(payload.pos) instanceof ThermalSourceBlockEntity be) {
                    be.setTargetElectricHeat(payload.targetHeat);
                }
            }
        });
    }
}