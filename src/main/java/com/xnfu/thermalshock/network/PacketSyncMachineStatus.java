package com.xnfu.thermalshock.network;

import com.xnfu.thermalshock.ThermalShock;
import com.xnfu.thermalshock.block.entity.SimulationChamberBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Optional;

/**
 * 轻量化机器状态同步包 (S2C)。
 */
public record PacketSyncMachineStatus(
        BlockPos pos,
        ThermalData thermal,
        CatalystData catalyst,
        int modeOrdinal,
        Optional<ResourceLocation> matchedRecipeId
) implements CustomPacketPayload {

    public record ThermalData(int currentHeat, int highTemp, int lowTemp, boolean isWorking) {
        public static final StreamCodec<RegistryFriendlyByteBuf, ThermalData> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, ThermalData::currentHeat,
                ByteBufCodecs.VAR_INT, ThermalData::highTemp,
                ByteBufCodecs.VAR_INT, ThermalData::lowTemp,
                ByteBufCodecs.BOOL, ThermalData::isWorking,
                ThermalData::new
        );
    }

    public record CatalystData(float buffer, float lockedPoints, float bonus) {
        public static final StreamCodec<RegistryFriendlyByteBuf, CatalystData> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.FLOAT, CatalystData::buffer,
                ByteBufCodecs.FLOAT, CatalystData::lockedPoints,
                ByteBufCodecs.FLOAT, CatalystData::bonus,
                CatalystData::new
        );
    }

    public static final Type<PacketSyncMachineStatus> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ThermalShock.MODID, "sync_machine_status"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PacketSyncMachineStatus> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, PacketSyncMachineStatus::pos,
            ThermalData.STREAM_CODEC, PacketSyncMachineStatus::thermal,
            CatalystData.STREAM_CODEC, PacketSyncMachineStatus::catalyst,
            ByteBufCodecs.VAR_INT, PacketSyncMachineStatus::modeOrdinal,
            ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC), PacketSyncMachineStatus::matchedRecipeId,
            PacketSyncMachineStatus::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final PacketSyncMachineStatus payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().level().getBlockEntity(payload.pos()) instanceof SimulationChamberBlockEntity be) {
                be.syncFromPacket(payload);
            }
        });
    }
}
