package com.cesg.network;

import com.cesg.CESG;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = CESG.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class CESGNetwork {
    private CESGNetwork() {}

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(StationConfigurationPacket.TYPE, StationConfigurationPacket.STREAM_CODEC,
                StationConfigurationPacket::handle);
        registrar.playToServer(SetGatewayChannelPacket.TYPE, SetGatewayChannelPacket.STREAM_CODEC,
                SetGatewayChannelPacket::handle);
        registrar.playToClient(TerminalContentPacket.TYPE, TerminalContentPacket.STREAM_CODEC,
                TerminalContentPacket::handle);
        registrar.playToServer(TerminalActionPacket.TYPE, TerminalActionPacket.STREAM_CODEC,
                TerminalActionPacket::handle);
    }
}
