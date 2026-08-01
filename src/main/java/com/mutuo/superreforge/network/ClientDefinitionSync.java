package com.mutuo.superreforge.network;

import com.mutuo.superreforge.definition.DefinitionManager;
import com.mutuo.superreforge.definition.ModifierDisplayDefinition;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

/** 客户端按代次原子组装定义分块；缺块或陈旧分块绝不会覆盖最后一份完整快照。 */
public final class ClientDefinitionSync {
    public enum Result { INCOMPLETE, COMPLETE, REJECTED }

    private static long incomingGeneration = -1L;
    private static long completedGeneration = -1L;
    private static int expectedChunks;
    private static final Map<Integer, Map<ResourceLocation, ModifierDisplayDefinition>> CHUNKS = new HashMap<>();

    private ClientDefinitionSync() {}

    public static synchronized Result accept(DefinitionSyncPayload payload) {
        if (payload.generation() < completedGeneration) {
            return Result.REJECTED;
        }
        if (payload.generation() != incomingGeneration) {
            incomingGeneration = payload.generation();
            expectedChunks = payload.totalChunks();
            CHUNKS.clear();
        }
        if (payload.totalChunks() != expectedChunks) {
            CHUNKS.clear();
            return Result.REJECTED;
        }
        Map<ResourceLocation, ModifierDisplayDefinition> previous = CHUNKS.putIfAbsent(
                payload.chunkIndex(), payload.modifiers());
        if (previous != null && !previous.equals(payload.modifiers())) {
            CHUNKS.clear();
            return Result.REJECTED;
        }
        if (CHUNKS.size() != expectedChunks) {
            return Result.INCOMPLETE;
        }
        Map<ResourceLocation, ModifierDisplayDefinition> merged = new LinkedHashMap<>();
        for (int index = 0; index < expectedChunks; index++) {
            Map<ResourceLocation, ModifierDisplayDefinition> part = CHUNKS.get(index);
            if (part == null) {
                return Result.INCOMPLETE;
            }
            merged.putAll(part);
        }
        DefinitionManager.installClientModifiers(merged);
        completedGeneration = incomingGeneration;
        CHUNKS.clear();
        return Result.COMPLETE;
    }

    public static synchronized long completedGeneration() {
        return completedGeneration;
    }

    public static synchronized void clear() {
        incomingGeneration = -1L;
        completedGeneration = -1L;
        expectedChunks = 0;
        CHUNKS.clear();
    }
}
