package com.mutuo.superreforge.network;

import com.mutuo.superreforge.SuperReforge;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** 客户端完整组装显示快照后回送的代次确认。 */
public record DefinitionSyncAckPayload(long generation, boolean success) implements CustomPacketPayload {
    public static final Type<DefinitionSyncAckPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(SuperReforge.MOD_ID, "definition_sync_ack"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DefinitionSyncAckPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_LONG,
                    DefinitionSyncAckPayload::generation,
                    ByteBufCodecs.BOOL,
                    DefinitionSyncAckPayload::success,
                    DefinitionSyncAckPayload::new);

    public DefinitionSyncAckPayload {
        if (generation < 0) {
            throw new IllegalArgumentException("definition generation 不能为负数");
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
