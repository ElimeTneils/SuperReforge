package com.mutuo.superreforge.network;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** 注册只从服务器流向客户端的报价同步协议。 */
public final class ModNetwork {
    private ModNetwork() {}

    public static void register(IEventBus modBus) {
        modBus.addListener(ModNetwork::registerPayloads);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1")
                .playToClient(
                        ReforgePreviewPayload.TYPE,
                        ReforgePreviewPayload.STREAM_CODEC,
                        (payload, context) -> ClientPreviewState.accept(payload));
    }
}
