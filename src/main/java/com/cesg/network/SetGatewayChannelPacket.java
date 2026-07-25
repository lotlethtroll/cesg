package com.cesg.network;

import com.cesg.CESG;
import com.cesg.gateways.CrossDimensionalGatewayCoreBlockEntity;
import com.simibubi.create.foundation.utility.AdventureUtil;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client -> server: configure a Gateway Core from the destination picker (Phase 6A) — select the
 * active channel and/or set the gateway's display name. {@code channel < 0} keeps the current channel
 * (name-only update, sent when the screen closes without a pick).
 */
public record SetGatewayChannelPacket(BlockPos pos, int channel, String name, boolean chunkLoading,
        boolean routeMode) implements CustomPacketPayload {
    public static final Type<SetGatewayChannelPacket> TYPE = new Type<>(CESG.id("set_gateway_channel"));

    public static final StreamCodec<ByteBuf, SetGatewayChannelPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, SetGatewayChannelPacket::pos,
            ByteBufCodecs.VAR_INT, SetGatewayChannelPacket::channel,
            ByteBufCodecs.stringUtf8(32), SetGatewayChannelPacket::name,
            ByteBufCodecs.BOOL, SetGatewayChannelPacket::chunkLoading,
            ByteBufCodecs.BOOL, SetGatewayChannelPacket::routeMode,
            SetGatewayChannelPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SetGatewayChannelPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (player == null || player.isSpectator() || AdventureUtil.isAdventure(player))
                return;
            if (!player.level().isLoaded(packet.pos()) || !player.canInteractWithBlock(packet.pos(), 20))
                return;
            BlockEntity blockEntity = player.level().getBlockEntity(packet.pos());
            if (blockEntity instanceof CrossDimensionalGatewayCoreBlockEntity core) {
                if (packet.channel() >= 0)
                    core.setActiveChannel(packet.channel());
                core.setGatewayName(packet.name());
                core.setChunkLoading(packet.chunkLoading());
                core.setRouteMode(packet.routeMode());
                core.setChanged();
            }
        });
    }
}
