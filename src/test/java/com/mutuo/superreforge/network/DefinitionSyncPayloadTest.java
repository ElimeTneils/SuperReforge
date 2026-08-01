package com.mutuo.superreforge.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mutuo.superreforge.definition.DefinitionManager;
import com.mutuo.superreforge.definition.ModifierDisplayDefinition;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** 验证显示快照按上限分块、可乱序原子组装，并且服务端只接受当前代次的成功 ACK。 */
final class DefinitionSyncPayloadTest {
    @AfterEach
    void clearState() {
        ClientDefinitionSync.clear();
        DefinitionManager.clearClientSession();
        DefinitionSyncTracker.clearForTests();
    }

    @Test
    void chunksLargeSnapshotAndPublishesOnlyAfterEveryChunkArrives() {
        Map<ResourceLocation, ModifierDisplayDefinition> definitions = definitions(130);
        List<DefinitionSyncPayload> chunks = DefinitionSyncPayload.chunked(42L, definitions);

        assertEquals(2, chunks.size());
        assertTrue(chunks.stream().allMatch(chunk -> chunk.modifiers().size() <= DefinitionSyncPayload.MAX_PER_CHUNK));

        assertEquals(ClientDefinitionSync.Result.INCOMPLETE, ClientDefinitionSync.accept(chunks.get(1)));
        assertTrue(DefinitionManager.clientModifiers().isEmpty(), "缺块时不得发布半份客户端快照");

        assertEquals(ClientDefinitionSync.Result.COMPLETE, ClientDefinitionSync.accept(chunks.get(0)));
        assertEquals(130, DefinitionManager.clientModifiers().size());
        assertEquals(42L, ClientDefinitionSync.completedGeneration());
    }

    @Test
    void trackerRejectsMissingFailedOrStaleAcknowledgements() {
        UUID player = UUID.randomUUID();
        DefinitionSyncTracker.expect(player, 8L);

        assertFalse(DefinitionSyncTracker.canReforge(player, 8L));
        DefinitionSyncTracker.acknowledge(player, 7L, true);
        assertFalse(DefinitionSyncTracker.canReforge(player, 8L));
        DefinitionSyncTracker.acknowledge(player, 8L, false);
        assertFalse(DefinitionSyncTracker.canReforge(player, 8L));
        DefinitionSyncTracker.acknowledge(player, 8L, true);
        assertTrue(DefinitionSyncTracker.canReforge(player, 8L));
    }

    private static Map<ResourceLocation, ModifierDisplayDefinition> definitions(int count) {
        Map<ResourceLocation, ModifierDisplayDefinition> values = new LinkedHashMap<>();
        for (int index = 0; index < count; index++) {
            values.put(
                    ResourceLocation.fromNamespaceAndPath("test", "modifier_" + index),
                    new ModifierDisplayDefinition(Component.literal("词条 " + index), List.of()));
        }
        return values;
    }
}
