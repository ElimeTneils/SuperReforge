package com.mutuo.superreforge;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/** Guards the published KubeJS combat example against losing its optional Attribute integrations. */
final class CombatKubeJsExampleTest {
    private static final Path ROOT = findProjectRoot();
    private static final Pattern COMBAT_SPEC = Pattern.compile(
            "const SR_COMBAT_SPEC = JSON\\.parse\\(\\s*`(.*?)`\\s*\\)", Pattern.DOTALL);
    private static final Map<String, List<List<Double>>> EXPECTED_RANGES = Map.of(
            "criticalChance", List.of(List.of(0.01, 0.02), List.of(0.02, 0.035), List.of(0.035, 0.05), List.of(0.05, 0.07), List.of(0.07, 0.095), List.of(0.095, 0.125), List.of(0.125, 0.16), List.of(0.16, 0.20)),
            "criticalDamage", List.of(List.of(0.03, 0.05), List.of(0.05, 0.08), List.of(0.08, 0.12), List.of(0.12, 0.17), List.of(0.17, 0.23), List.of(0.23, 0.30), List.of(0.30, 0.38), List.of(0.38, 0.48)),
            "rangedDamage", List.of(List.of(0.02, 0.03), List.of(0.03, 0.05), List.of(0.05, 0.08), List.of(0.08, 0.11), List.of(0.11, 0.15), List.of(0.15, 0.20), List.of(0.20, 0.25), List.of(0.25, 0.32)),
            "haste", List.of(List.of(0.02, 0.03), List.of(0.03, 0.05), List.of(0.05, 0.08), List.of(0.08, 0.11), List.of(0.11, 0.15), List.of(0.15, 0.19), List.of(0.19, 0.23), List.of(0.23, 0.28)),
            "velocity", List.of(List.of(0.01, 0.02), List.of(0.02, 0.035), List.of(0.035, 0.05), List.of(0.05, 0.075), List.of(0.075, 0.10), List.of(0.10, 0.13), List.of(0.13, 0.16), List.of(0.16, 0.20)),
            "pullTime", List.of(List.of(-0.03, 0.02), List.of(-0.05, 0.03), List.of(-0.07, 0.05), List.of(-0.10, 0.07), List.of(-0.13, 0.10), List.of(-0.16, 0.13), List.of(-0.19, 0.16), List.of(-0.22, 0.19)));
    private static final List<String> EXPECTED_LEVEL_NAMES =
            List.of("等级 1", "等级 2", "等级 3", "等级 4", "等级 5", "等级 6", "等级 7", "等级 8");
    private static final List<String> EXPECTED_ARMOR_NAMES =
            List.of("坚韧", "守势", "铁壁", "不屈", "磐石", "圣佑", "不灭", "永恒");
    private static final Map<String, List<String>> EXPECTED_POOL_NAMES = Map.of(
            "melee", List.of("锐意", "强袭", "猎杀", "致命", "狂战", "破军", "弑神", "终焉"),
            "ranged", List.of("稳弦", "劲射", "疾羽", "鹰眼", "风行", "穿云", "逐星", "天穹"),
            "helmet", EXPECTED_ARMOR_NAMES,
            "chestplate", EXPECTED_ARMOR_NAMES,
            "leggings", EXPECTED_ARMOR_NAMES,
            "boots", EXPECTED_ARMOR_NAMES,
            "tool", List.of("熟练", "利落", "精工", "迅捷", "大师", "奇迹", "神匠", "创世"),
            "curio", List.of("微光", "灵辉", "祝福", "守护", "星辉", "命运", "神谕", "超越"));

    @Test
    void shipsCombatExampleWithEveryOptionalAttributePool() throws IOException {
        String source = Files.readString(
                ROOT.resolve("examples/kubejs/superreforge_combat_attributes.js"), StandardCharsets.UTF_8);
        for (String required : List.of(
                "superreforge:worn",
                "superreforge:divine",
                "example:helmet",
                "example:chestplate",
                "example:leggings",
                "example:boots",
                "example:tool",
                "critical_strike:chance",
                "critical_strike:damage",
                "ranged_weapon:damage",
                "ranged_weapon:haste",
                "ranged_weapon:velocity",
                "ranged_weapon:pull_time",
                "add_multiplied_base",
                "add_multiplied_total",
                "curios:any")) {
            assertTrue(source.contains(required), "combat KubeJS example is missing: " + required);
        }
        assertTrue(source.contains("include: itemType.selectors.map(tag => ({ tag: tag }))"),
                "selector objects must use explicit keys supported by KubeJS 2101 Rhino");
        assertFalse(source.contains("({ tag }))"),
                "KubeJS 2101 Rhino does not support object property shorthand here");
    }

    @Test
    void publishesSixtyFourStructuredCombatModifiersWithExactArmorSlots() throws IOException {
        JsonObject spec = combatSpec();
        JsonArray levels = spec.getAsJsonArray("levels");
        assertTrue(levels.size() == 8, "combat specification must define eight ranks");
        for (int index = 0; index < levels.size(); index++) {
            JsonObject level = levels.get(index).getAsJsonObject();
            int rank = index + 1;
            assertTrue(level.get("rank").getAsInt() == rank, "rank order changed");
            assertTrue(level.get("weight").getAsInt() == 100 - rank * 8, "rank weight changed");
        }

        JsonArray pools = spec.getAsJsonArray("pools");
        assertTrue(pools.size() == 8, "combat specification must define eight pools");
        Set<String> modifierIds = new HashSet<>();
        for (JsonElement poolElement : pools) {
            String poolId = poolElement.getAsJsonObject().get("id").getAsString();
            for (JsonElement levelElement : levels) {
                int rank = levelElement.getAsJsonObject().get("rank").getAsInt();
                assertTrue(modifierIds.add("example:combat_" + poolId + "_" + rank), "duplicate modifier ID");
            }
        }
        assertTrue(modifierIds.size() == 64, "eight pools across eight ranks must produce sixty-four modifier IDs");
        assertItemType(spec.getAsJsonArray("itemTypes"), "example:helmet", List.of("minecraft:head_armor"));
        assertItemType(spec.getAsJsonArray("itemTypes"), "example:chestplate", List.of("minecraft:chest_armor"));
        assertItemType(spec.getAsJsonArray("itemTypes"), "example:leggings", List.of("minecraft:leg_armor"));
        assertItemType(spec.getAsJsonArray("itemTypes"), "example:boots", List.of("minecraft:foot_armor"));
        assertItemType(spec.getAsJsonArray("itemTypes"), "example:tool", List.of(
                "minecraft:pickaxes", "minecraft:shovels", "minecraft:hoes"));
        assertPool(pools, "melee", List.of("superreforge:sword", "superreforge:axe", "superreforge:trident", "superreforge:mace"), List.of("mainhand"), false);
        assertPool(pools, "ranged", List.of("superreforge:bow", "superreforge:crossbow"), List.of("mainhand"), true);
        assertPool(pools, "helmet", List.of("example:helmet"), List.of("head"), false);
        assertPool(pools, "chestplate", List.of("example:chestplate"), List.of("chest"), false);
        assertPool(pools, "leggings", List.of("example:leggings"), List.of("legs"), false);
        assertPool(pools, "boots", List.of("example:boots"), List.of("feet"), false);
        assertPool(pools, "tool", List.of("example:tool"), List.of("mainhand"), false);
        assertPool(pools, "curio", List.of("superreforge:curio"), List.of("curios:any"), false);

        JsonArray ranges = spec.getAsJsonArray("ranges");
        assertTrue(ranges.size() == 8, "each rank needs one range row");
        for (int index = 0; index < ranges.size(); index++) {
            JsonElement rowElement = ranges.get(index);
            JsonObject row = rowElement.getAsJsonObject();
            for (String field : List.of("criticalChance", "criticalDamage", "rangedDamage", "haste", "velocity", "pullTime")) {
                JsonArray values = row.getAsJsonArray(field);
                assertTrue(values.size() == 2 && values.get(0).getAsDouble() <= values.get(1).getAsDouble(),
                        "range must be ordered: " + field);
                List<Double> expected = EXPECTED_RANGES.get(field).get(index);
                assertTrue(values.get(0).getAsDouble() == expected.get(0) && values.get(1).getAsDouble() == expected.get(1),
                        "wrong range for rank " + (index + 1) + ": " + field);
            }
        }
    }

    @Test
    void publishesReadableUtf8LevelAndPoolNames() throws IOException {
        String source = Files.readString(
                ROOT.resolve("examples/kubejs/superreforge_combat_attributes.js"), StandardCharsets.UTF_8);
        JsonObject spec = combatSpec();
        assertTrue(spec.getAsJsonArray("levels").asList().stream()
                        .map(element -> element.getAsJsonObject().get("name").getAsString())
                        .toList()
                        .equals(EXPECTED_LEVEL_NAMES),
                "combat levels must use the published Chinese names");
        for (var expected : EXPECTED_POOL_NAMES.entrySet()) {
            JsonObject pool = spec.getAsJsonArray("pools").asList().stream()
                    .map(JsonElement::getAsJsonObject)
                    .filter(candidate -> expected.getKey().equals(candidate.get("id").getAsString()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("missing pool: " + expected.getKey()));
            assertTrue(strings(pool.getAsJsonArray("names")).equals(expected.getValue()),
                    "combat pool has corrupted names: " + expected.getKey());
        }
        for (String mojibake : List.of("绛夌骇", "閿愭剰", "鍔插皠", "鍧氶煣", "鐔熺粌", "寰厜", "�")) {
            assertFalse(source.contains(mojibake), "combat script contains mojibake: " + mojibake);
        }
    }

    private static void assertPool(
            JsonArray pools, String id, List<String> itemTypes, List<String> slots, boolean ranged) {
        JsonObject pool = pools.asList().stream()
                .map(JsonElement::getAsJsonObject)
                .filter(candidate -> id.equals(candidate.get("id").getAsString()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("missing pool: " + id));
        assertTrue(pool.getAsJsonArray("names").size() == 8, "pool needs eight modifier names: " + id);
        assertTrue(strings(pool.getAsJsonArray("itemTypes")).equals(itemTypes), "wrong selectors: " + id);
        assertTrue(strings(pool.getAsJsonArray("slots")).equals(slots), "wrong slots: " + id);
        JsonArray effects = pool.getAsJsonArray("effects");
        assertEffect(effects, "critical_strike:chance", "add_multiplied_base", "criticalChance");
        assertEffect(effects, "critical_strike:damage", "add_multiplied_base", "criticalDamage");
        assertTrue(effects.size() == (ranged ? 6 : 2), "wrong effect count: " + id);
        if (ranged) {
            assertEffect(effects, "ranged_weapon:damage", "add_multiplied_total", "rangedDamage");
            assertEffect(effects, "ranged_weapon:haste", "add_multiplied_base", "haste");
            assertEffect(effects, "ranged_weapon:velocity", "add_multiplied_total", "velocity");
            assertEffect(effects, "ranged_weapon:pull_time", "add_multiplied_total", "pullTime");
        }
    }

    private static void assertItemType(JsonArray itemTypes, String id, List<String> selectors) {
        JsonObject itemType = itemTypes.asList().stream()
                .map(JsonElement::getAsJsonObject)
                .filter(candidate -> id.equals(candidate.get("id").getAsString()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("missing item type: " + id));
        assertTrue(strings(itemType.getAsJsonArray("selectors")).equals(selectors), "wrong selectors: " + id);
    }

    private static void assertEffect(JsonArray effects, String attribute, String operation, String range) {
        JsonObject effect = effects.asList().stream()
                .map(JsonElement::getAsJsonObject)
                .filter(candidate -> attribute.equals(candidate.get("attribute").getAsString()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("missing effect: " + attribute));
        assertTrue(operation.equals(effect.get("operation").getAsString()), "wrong operation: " + attribute);
        assertTrue(range.equals(effect.get("range").getAsString()), "wrong range: " + attribute);
    }

    private static List<String> strings(JsonArray values) {
        return values.asList().stream().map(JsonElement::getAsString).toList();
    }

    private static JsonObject combatSpec() throws IOException {
        String source = Files.readString(
                ROOT.resolve("examples/kubejs/superreforge_combat_attributes.js"), StandardCharsets.UTF_8);
        Matcher matcher = COMBAT_SPEC.matcher(source);
        assertTrue(matcher.find(), "combat example must expose its executable structured specification");
        return JsonParser.parseString(matcher.group(1)).getAsJsonObject();
    }

    private static Path findProjectRoot() {
        Path cursor = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (cursor != null && !Files.isRegularFile(cursor.resolve("gradlew.bat"))) {
            cursor = cursor.getParent();
        }
        if (cursor == null) {
            throw new IllegalStateException("Unable to locate the Super Reforge project root");
        }
        return cursor;
    }
}
