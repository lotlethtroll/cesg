package com.cesg.network;

import com.cesg.CESG;
import com.cesg.storage.station.AbstractShulkerStationBlockEntity;
import com.simibubi.create.foundation.utility.AdventureUtil;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record StationConfigurationPacket(BlockPos pos, int retention, int fullness, int threshold)
        implements CustomPacketPayload {
    public static final Type<StationConfigurationPacket> TYPE = new Type<>(CESG.id("configure_station"));

    public static final StreamCodec<ByteBuf, StationConfigurationPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, StationConfigurationPacket::pos,
            ByteBufCodecs.VAR_INT, StationConfigurationPacket::retention,
            ByteBufCodecs.VAR_INT, StationConfigurationPacket::fullness,
            ByteBufCodecs.VAR_INT, StationConfigurationPacket::threshold,
            StationConfigurationPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(StationConfigurationPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (player == null || player.isSpectator() || AdventureUtil.isAdventure(player))
                return;
            if (!player.level().isLoaded(packet.pos()))
                return;
            if (!player.canInteractWithBlock(packet.pos(), 20))
                return;
            BlockEntity blockEntity = player.level().getBlockEntity(packet.pos());
            if (blockEntity instanceof AbstractShulkerStationBlockEntity station) {
                station.applyConfig(packet.retention(), packet.fullness(), packet.threshold());
                station.sendData();
                station.setChanged();
            }
        });
    }
}
