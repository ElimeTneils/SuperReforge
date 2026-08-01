package com.mutuo.superreforge.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mutuo.superreforge.definition.DefinitionSnapshot;
import com.mutuo.superreforge.definition.LevelDefinition;
import com.mutuo.superreforge.definition.ModifierDefinition;
import com.mutuo.superreforge.reforge.CandidateLevel;
import com.mutuo.superreforge.reforge.CandidatePool;
import com.mutuo.superreforge.reforge.Cost;
import com.mutuo.superreforge.reforge.ReforgeQuote;
import com.mutuo.superreforge.reforge.ReforgeQuoteResult;
import com.mutuo.superreforge.reforge.WeightedValue;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.connection.ConnectionType;
import org.junit.jupiter.api.Test;

/** 验证预览网络安全上限不会静默伪装成完整概率列表。 */
final class ReforgePreviewPayloadTest {
    @Test
    void marksPreviewTruncatedWhenLevelLimitIsReached() {
        List<CandidateLevel> levels = new ArrayList<>();
        Map<ResourceLocation, LevelDefinition> definitions = new LinkedHashMap<>();
        for (int index = 0; index < 65; index++) {
            ResourceLocation level = id("level_" + index);
            ResourceLocation modifier = id("modifier_" + index);
            levels.add(new CandidateLevel(level, 1.0, List.of(new WeightedValue<>(modifier, 1.0))));
            definitions.put(level, new LevelDefinition(index, Component.literal("等级 " + index)));
        }

        ReforgePreviewPayload payload = ReforgePreviewPayload.from(
                7, success(new CandidatePool(levels)), snapshot(definitions, Map.of()));

        assertEquals(64, payload.levels().size());
        assertTrue(payload.truncated());
    }

    @Test
    void marksPreviewTruncatedWhenModifierLimitIsReached() {
        ResourceLocation level = id("only_level");
        List<WeightedValue<ResourceLocation>> modifiers = new ArrayList<>();
        Map<ResourceLocation, ModifierDefinition> definitions = new LinkedHashMap<>();
        for (int index = 0; index < 257; index++) {
            ResourceLocation modifier = id("modifier_" + index);
            modifiers.add(new WeightedValue<>(modifier, 1.0));
            definitions.put(modifier, new ModifierDefinition(
                    level, List.of(), Component.literal("词条 " + index), 1.0, List.of()));
        }

        ReforgePreviewPayload payload = ReforgePreviewPayload.from(
                8,
                success(new CandidatePool(List.of(new CandidateLevel(level, 1.0, modifiers)))),
                snapshot(Map.of(level, new LevelDefinition(1, Component.literal("唯一等级"))), definitions));

        assertEquals(256, payload.levels().getFirst().modifiers().size());
        assertTrue(payload.truncated());
    }

    @Test
    void leavesSmallPreviewMarkedComplete() {
        ResourceLocation level = id("small");
        ResourceLocation modifier = id("steady");
        CandidatePool pool = new CandidatePool(List.of(
                new CandidateLevel(level, 1.0, List.of(new WeightedValue<>(modifier, 1.0)))));

        ReforgePreviewPayload payload = ReforgePreviewPayload.from(
                9,
                success(pool),
                snapshot(
                        Map.of(level, new LevelDefinition(1, Component.literal("普通"))),
                        Map.of(modifier, new ModifierDefinition(
                                level, List.of(), Component.literal("稳定"), 1.0, List.of()))));

        assertFalse(payload.truncated());
    }

    @Test
    void preservesTruncationFlagAcrossNetworkRoundTrip() {
        ReforgePreviewPayload original = new ReforgePreviewPayload(
                11, 4L, -1, 2, 3, true, List.of());
        RegistryFriendlyByteBuf buffer =
                new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY, ConnectionType.OTHER);

        ReforgePreviewPayload.STREAM_CODEC.encode(buffer, original);
        ReforgePreviewPayload decoded = ReforgePreviewPayload.STREAM_CODEC.decode(buffer);

        assertEquals(original, decoded);
        assertTrue(decoded.truncated());
    }

    private static ReforgeQuoteResult success(CandidatePool pool) {
        return ReforgeQuoteResult.success(new ReforgeQuote(id("catalyst"), null, new Cost(2, 3), pool));
    }

    private static DefinitionSnapshot snapshot(
            Map<ResourceLocation, LevelDefinition> levels,
            Map<ResourceLocation, ModifierDefinition> modifiers) {
        return new DefinitionSnapshot(levels, Map.of(), modifiers, Map.of());
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("test", path);
    }
}
