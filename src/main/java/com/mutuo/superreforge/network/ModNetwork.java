package com.mutuo.superreforge.network;

import com.mutuo.superreforge.definition.DefinitionManager;
import com.mutuo.superreforge.definition.ModifierDisplayDefinition;
import com.mutuo.superreforge.item.AttributeRefreshService;
import java.util.LinkedHashMap;
import net.neoforged.bus.api.IEventBus;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** 注册报价与分块显示快照协议，并在登录/reload 时同步服务器权威显示代次。 */
public final class ModNetwork {
    private ModNetwork() {}

    public static void register(IEventBus modBus) {
        modBus.addListener(ModNetwork::registerPayloads);
        NeoForge.EVENT_BUS.addListener(ModNetwork::onDatapackSync);
        NeoForge.EVENT_BUS.addListener(ModNetwork::onPlayerLogout);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("2")
                .playToClient(
                        ReforgePreviewPayload.TYPE,
                        ReforgePreviewPayload.STREAM_CODEC,
                        (payload, context) -> ClientPreviewState.accept(payload))
                .playToClient(
                        DefinitionSyncPayload.TYPE,
                        DefinitionSyncPayload.STREAM_CODEC,
                        (payload, context) -> {
                            ClientDefinitionSync.Result result = ClientDefinitionSync.accept(payload);
                            if (result == ClientDefinitionSync.Result.COMPLETE) {
                                context.reply(new DefinitionSyncAckPayload(payload.generation(), true));
                            } else if (result == ClientDefinitionSync.Result.REJECTED) {
                                context.reply(new DefinitionSyncAckPayload(payload.generation(), false));
                            }
                        })
                .playToServer(
                        DefinitionSyncAckPayload.TYPE,
                        DefinitionSyncAckPayload.STREAM_CODEC,
                        (payload, context) -> {
                            if (context.player() instanceof ServerPlayer player) {
                                DefinitionSyncTracker.acknowledge(player, payload.generation(), payload.success());
                            }
                        });
    }

    private static void onDatapackSync(OnDatapackSyncEvent event) {
        synchronizePlayers(event.getRelevantPlayers());
    }

    /** KubeJS 独立 reload 完成后调用；立即推进所有在线玩家的显示快照与 ACK 期望代次。 */
    public static void synchronizeAll(MinecraftServer server) {
        synchronizePlayers(server.getPlayerList().getPlayers().stream());
    }

    /** 同步前先刷新已穿戴属性，保证玩家看到的新定义与实体当前数值属于同一代。 */
    private static void synchronizePlayers(java.util.stream.Stream<ServerPlayer> players) {
        long generation = DefinitionManager.generation();
        var display = new LinkedHashMap<net.minecraft.resources.ResourceLocation, ModifierDisplayDefinition>();
        DefinitionManager.snapshot().modifiers().forEach(
                (id, definition) -> display.put(id, ModifierDisplayDefinition.from(definition)));
        var chunks = DefinitionSyncPayload.chunked(generation, display);
        players.forEach(player -> {
            AttributeRefreshService.refresh(player);
            DefinitionSyncTracker.expect(player, generation);
            chunks.forEach(chunk -> PacketDistributor.sendToPlayer(player, chunk));
        });
    }

    private static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            DefinitionSyncTracker.remove(player);
        }
    }
}
