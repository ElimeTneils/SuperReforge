package com.mutuo.superreforge.network;

import com.mutuo.superreforge.SuperReforge;
import com.mutuo.superreforge.definition.ModifierDisplayDefinition;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** 登录和 reload 后发送的一块只读词条显示快照。 */
public record DefinitionSyncPayload(
        long generation,
        int chunkIndex,
        int totalChunks,
        Map<ResourceLocation, ModifierDisplayDefinition> modifiers) implements CustomPacketPayload {
    public static final int MAX_PER_CHUNK = 128;
    public static final int MAX_CHUNKS = 64;
    public static final int MAX_TOTAL_MODIFIERS = MAX_PER_CHUNK * MAX_CHUNKS;

    public static final Type<DefinitionSyncPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(SuperReforge.MOD_ID, "definition_sync"));

    private static final StreamCodec<RegistryFriendlyByteBuf, Map<ResourceLocation, ModifierDisplayDefinition>>
            MODIFIER_MAP_CODEC = ByteBufCodecs.map(
                    LinkedHashMap::new,
                    ResourceLocation.STREAM_CODEC,
                    ByteBufCodecs.fromCodecWithRegistries(ModifierDisplayDefinition.CODEC),
                    MAX_PER_CHUNK);

    public static final StreamCodec<RegistryFriendlyByteBuf, DefinitionSyncPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_LONG,
                    DefinitionSyncPayload::generation,
                    ByteBufCodecs.VAR_INT,
                    DefinitionSyncPayload::chunkIndex,
                    ByteBufCodecs.VAR_INT,
                    DefinitionSyncPayload::totalChunks,
                    MODIFIER_MAP_CODEC,
                    DefinitionSyncPayload::modifiers,
                    DefinitionSyncPayload::new);

    public DefinitionSyncPayload {
        if (generation < 0) {
            throw new IllegalArgumentException("definition generation 不能为负数");
        }
        if (totalChunks < 1 || totalChunks > MAX_CHUNKS || chunkIndex < 0 || chunkIndex >= totalChunks) {
            throw new IllegalArgumentException("无效定义同步分块 " + chunkIndex + "/" + totalChunks);
        }
        if (modifiers.size() > MAX_PER_CHUNK) {
            throw new IllegalArgumentException("单个定义同步分块超过 " + MAX_PER_CHUNK + " 条");
        }
        modifiers = Map.copyOf(modifiers);
    }

    /** 按资源 ID 稳定排序并分块；空快照仍发送一块，以便客户端清除旧定义并回 ACK。 */
    public static List<DefinitionSyncPayload> chunked(
            long generation, Map<ResourceLocation, ModifierDisplayDefinition> modifiers) {
        if (modifiers.size() > MAX_TOTAL_MODIFIERS) {
            throw new IllegalArgumentException("客户端显示词条超过上限 " + MAX_TOTAL_MODIFIERS);
        }
        var entries = modifiers.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .toList();
        int total = Math.max(1, (entries.size() + MAX_PER_CHUNK - 1) / MAX_PER_CHUNK);
        List<DefinitionSyncPayload> chunks = new ArrayList<>(total);
        for (int index = 0; index < total; index++) {
            Map<ResourceLocation, ModifierDisplayDefinition> part = new LinkedHashMap<>();
            int from = index * MAX_PER_CHUNK;
            int to = Math.min(entries.size(), from + MAX_PER_CHUNK);
            for (int cursor = from; cursor < to; cursor++) {
                var entry = entries.get(cursor);
                part.put(entry.getKey(), entry.getValue());
            }
            chunks.add(new DefinitionSyncPayload(generation, index, total, part));
        }
        return List.copyOf(chunks);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
