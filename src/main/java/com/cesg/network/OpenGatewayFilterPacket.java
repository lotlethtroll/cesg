package com.cesg.network;

import com.cesg.CESG;
import com.cesg.gateways.CrossDimensionalGatewayCoreBlockEntity;
import com.cesg.gateways.GatewayFilterMenu;
import com.simibubi.create.foundation.utility.AdventureUtil;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client -> server: open the routing-filter editor for one gateway channel (Phase 7B), requested from
 * the destination picker. The server opens a {@link GatewayFilterMenu} bound to the Core + channel.
 */
public record OpenGatewayFilterPacket(BlockPos pos, int channel) implements CustomPacketPayload {
    public static final Type<OpenGatewayFilterPacket> TYPE = new Type<>(CESG.id("open_gateway_filter"));

    public static final StreamCodec<ByteBuf, OpenGatewayFilterPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, OpenGatewayFilterPacket::pos,
            ByteBufCodecs.VAR_INT, OpenGatewayFilterPacket::channel,
            OpenGatewayFilterPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenGatewayFilterPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (player == null || player.isSpectator() || AdventureUtil.isAdventure(player))
                return;
            if (!player.level().isLoaded(packet.pos()) || !player.canInteractWithBlock(packet.pos(), 20))
                return;
            BlockEntity blockEntity = player.level().getBlockEntity(packet.pos());
            if (!(blockEntity instanceof CrossDimensionalGatewayCoreBlockEntity))
                return;
            int channel = Math.floorMod(packet.channel(), CrossDimensionalGatewayCoreBlockEntity.CHANNEL_COUNT);
            player.openMenu(new SimpleMenuProvider(
                    (containerId, inventory, p) -> new GatewayFilterMenu(containerId, inventory, packet.pos(), channel),
                    Component.translatable("cesg.gateway.filter.title", channel + 1)),
                    buf -> {
                        buf.writeBlockPos(packet.pos());
                        buf.writeVarInt(channel);
                    });
        });
    }
}
