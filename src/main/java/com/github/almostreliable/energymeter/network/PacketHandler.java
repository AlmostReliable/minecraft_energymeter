package com.github.almostreliable.energymeter.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class PacketHandler {

    private static final String PROTOCOL = "1";

    private PacketHandler() {}

    public static void init(IEventBus modEventBus) {
        modEventBus.addListener(PacketHandler::onPacketRegistration);
    }

    private static void onPacketRegistration(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL);

        // server to client
        registrar.playToClient(
            ClientSyncPacket.TYPE,
            ClientSyncPacket.STREAM_CODEC,
            wrapHandler(ClientSyncPacket::handle)
        );

        // client to server
        registrar.playToServer(
            AccuracyUpdatePacket.TYPE,
            AccuracyUpdatePacket.STREAM_CODEC,
            wrapHandler(AccuracyUpdatePacket::handle)
        );
        registrar.playToServer(
            IOUpdatePacket.TYPE,
            IOUpdatePacket.STREAM_CODEC,
            wrapHandler(IOUpdatePacket::handle)
        );
        registrar.playToServer(
            SettingUpdatePacket.TYPE,
            SettingUpdatePacket.STREAM_CODEC,
            wrapHandler(SettingUpdatePacket::handle)
        );
    }

    private static <T extends CustomPacketPayload> IPayloadHandler<T> wrapHandler(IPayloadHandler<T> handler) {
        return (payload, context) -> context.enqueueWork(() -> handler.handle(payload, context));
    }
}
