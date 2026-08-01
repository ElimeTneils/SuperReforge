package com.mutuo.superreforge.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

/** 保护物品持久数据的兼容契约，防止重载或重启后丢失词条身份。 */
final class ReforgeDataCodecTest {
    @Test
    void codecRoundTripPreservesModifierSeedAndSchema() {
        ReforgeData original = new ReforgeData(
                ResourceLocation.fromNamespaceAndPath("example", "legendary_blade"),
                8_675_309L,
                ReforgeData.CURRENT_SCHEMA);

        JsonElement encoded = ReforgeData.CODEC.encodeStart(JsonOps.INSTANCE, original).getOrThrow();
        ReforgeData decoded = ReforgeData.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();

        assertEquals(original, decoded);
    }

    @Test
    void codecRejectsNonPositiveSchemaVersions() {
        String invalid = "{\"modifier_id\":\"example:broken\",\"seed\":1,\"schema_version\":0}";

        var result = ReforgeData.CODEC.parse(
                JsonOps.INSTANCE, com.google.gson.JsonParser.parseString(invalid));

        assertTrue(result.error().isPresent(), "schema 0 必须被拒绝，而不是静默接受");
    }

    @Test
    void legacySchemaDefaultsToActiveWhileInactiveStateRoundTrips() {
        String legacy = "{\"modifier_id\":\"example:legacy\",\"seed\":7,\"schema_version\":1}";
        ReforgeData oldData = ReforgeData.CODEC.parse(
                        JsonOps.INSTANCE, com.google.gson.JsonParser.parseString(legacy))
                .getOrThrow();
        assertTrue(oldData.active(), "旧存档没有 active 字段时必须保持原来的生效行为");

        ReforgeData inactive = new ReforgeData(
                ResourceLocation.fromNamespaceAndPath("example", "inactive"), 9L, ReforgeData.CURRENT_SCHEMA, false);
        JsonElement encoded = ReforgeData.CODEC.encodeStart(JsonOps.INSTANCE, inactive).getOrThrow();
        ReforgeData decoded = ReforgeData.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();
        assertFalse(decoded.active(), "KEEP_INACTIVE 状态必须跨存档保留");
    }
}
