package com.mutuo.superreforge;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/** 验证交付给整合包的纯 Curios 八级配置、固定属性和六种强化石概率。 */
final class CurioProgressionKubeJsExampleTest {
    private static final Path ROOT = findProjectRoot();
    private static final Path SCRIPT = ROOT.resolve("examples/kubejs/superreforge_curio_progression.js");
    private static final Pattern SPEC = Pattern.compile(
            "const SR_CURIO_SPEC = JSON\\.parse\\(\\s*`(.*?)`\\s*\\)", Pattern.DOTALL);
    private static final List<Integer> MODIFIER_WEIGHTS = List.of(55, 25, 15, 5);
    private static final List<String> CATALYST_ITEMS = List.of(
            "superreforge:common_reforge_stone",
            "superreforge:refined_reforge_stone",
            "superreforge:supreme_reforge_stone",
            "minecraft:netherite_ingot",
            "minecraft:nether_star",
            "minecraft:dragon_breath");
    private static final Map<Integer, List<List<Integer>>> CATALYST_LEVELS = Map.of(
            1, List.of(List.of(1, 15), List.of(2, 25), List.of(3, 45), List.of(4, 10), List.of(5, 5)),
            2, List.of(List.of(3, 25), List.of(4, 55), List.of(5, 15), List.of(6, 5)),
            3, List.of(List.of(5, 55), List.of(6, 35), List.of(7, 10)),
            4, List.of(List.of(5, 27), List.of(6, 55), List.of(7, 17), List.of(8, 1)),
            5, List.of(List.of(7, 95), List.of(8, 5)),
            6, List.of(List.of(8, 100)));

    @Test
    void definesEightRanksAndThirtyTwoFixedCurioModifiers() throws IOException {
        JsonObject spec = spec();
        assertEquals(8, spec.getAsJsonArray("levels").size());
        assertEquals(32, spec.getAsJsonArray("modifiers").size());

        Set<String> ids = new HashSet<>();
        for (int rank = 1; rank <= 8; rank++) {
            final int expectedRank = rank;
            List<JsonObject> ranked = spec.getAsJsonArray("modifiers").asList().stream()
                    .map(JsonElement::getAsJsonObject)
                    .filter(modifier -> modifier.get("rank").getAsInt() == expectedRank)
                    .toList();
            assertEquals(4, ranked.size(), "每级必须恰好四条词条");
            assertEquals(MODIFIER_WEIGHTS, ranked.stream()
                    .map(modifier -> modifier.get("weight").getAsInt())
                    .toList());
            ranked.forEach(modifier -> {
                assertTrue(ids.add(modifier.get("id").getAsString()), "词条 ID 必须唯一");
                modifier.getAsJsonArray("attributes").forEach(effect ->
                        assertTrue(effect.getAsJsonObject().get("amount").isJsonPrimitive(),
                                "当前配置的所有属性必须使用固定值"));
            });
        }
    }

    @Test
    void preservesRequestedNamesAndUsefulExternalAttributes() throws IOException {
        JsonObject spec = spec();
        List<String> names = spec.getAsJsonArray("modifiers").asList().stream()
                .map(element -> element.getAsJsonObject().get("name").getAsString())
                .toList();
        for (String name : List.of(
                "厚重", "贪婪", "幸运", "史诗", "非凡", "传说", "运动员", "审判者",
                "才华横溢", "致命一击", "不朽的训练家", "最后的守护者", "绝世的战斗家", "规则的缔造者")) {
            assertTrue(names.contains(name), "缺少前缀：" + name);
        }

        Set<String> attributes = new HashSet<>();
        spec.getAsJsonArray("modifiers").forEach(modifier -> modifier.getAsJsonObject()
                .getAsJsonArray("attributes")
                .forEach(effect -> attributes.add(effect.getAsJsonObject().get("attribute").getAsString())));
        for (String id : List.of(
                "critical_strike:chance",
                "critical_strike:damage",
                "ranged_weapon:damage",
                "scguns:additional_bullet_damage",
                "minecraft:generic.movement_efficiency",
                "minecraft:player.mining_efficiency",
                "neoforge:creative_flight")) {
            assertTrue(attributes.contains(id), "缺少 Attribute：" + id);
        }

        JsonObject trainer = modifier(spec, "mutuo:curio_8_1");
        long blockRangeCount = trainer.getAsJsonArray("attributes").asList().stream()
                .map(JsonElement::getAsJsonObject)
                .filter(effect -> "minecraft:player.block_interaction_range"
                        .equals(effect.get("attribute").getAsString()))
                .count();
        assertEquals(1, blockRangeCount, "重复输入的方块交互距离必须去重，避免意外叠加为 +4");
    }

    @Test
    void definesExactSixStoneLevelWeights() throws IOException {
        JsonArray catalysts = spec().getAsJsonArray("catalysts");
        assertEquals(6, catalysts.size());
        for (int index = 0; index < catalysts.size(); index++) {
            int stone = index + 1;
            JsonObject catalyst = catalysts.get(index).getAsJsonObject();
            assertEquals(CATALYST_ITEMS.get(index), catalyst.get("item").getAsString(),
                    "强化石" + stone + "级的实际物品映射错误");
            List<List<Integer>> actual = catalyst.getAsJsonArray("levels").asList().stream()
                    .map(JsonElement::getAsJsonArray)
                    .map(pair -> List.of(pair.get(0).getAsInt(), pair.get(1).getAsInt()))
                    .toList();
            assertEquals(CATALYST_LEVELS.get(stone), actual, "强化石 " + stone + " 级权重错误");
        }
    }

    @Test
    void disablesBuiltInsAndTargetsOnlyCuriosAny() throws IOException {
        String source = Files.readString(SCRIPT, StandardCharsets.UTF_8);
        for (String required : List.of(
                "['melee', 'ranged', 'curio']",
                "weight: 0",
                "item_types: ['mutuo:disabled_builtin']",
                "item_types: ['mutuo:curio_only']",
                "slots: ['curios:any']",
                "allowed_item_types: ['mutuo:curio_only']",
                "count: 1",
                "experience: 0")) {
            assertTrue(source.contains(required), "脚本缺少约束：" + required);
        }
        assertFalse(source.contains("({ item })"), "KubeJS 2101 Rhino 不应使用对象属性简写");
    }

    private static JsonObject modifier(JsonObject spec, String id) {
        return spec.getAsJsonArray("modifiers").asList().stream()
                .map(JsonElement::getAsJsonObject)
                .filter(modifier -> id.equals(modifier.get("id").getAsString()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("缺少词条：" + id));
    }

    private static JsonObject spec() throws IOException {
        String source = Files.readString(SCRIPT, StandardCharsets.UTF_8);
        Matcher matcher = SPEC.matcher(source);
        assertTrue(matcher.find(), "脚本必须包含可解析的 SR_CURIO_SPEC");
        return JsonParser.parseString(matcher.group(1)).getAsJsonObject();
    }

    private static Path findProjectRoot() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.isRegularFile(current.resolve("settings.gradle"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("找不到项目根目录");
    }
}
