package com.mutuo.superreforge.definition;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

/**
 * 防止随 JAR 发布的示例数据和 3D 模型在改动后悄悄失效。
 *
 * <p>这里直接用生产环境的 Codec 读取资源，而不是只检查 JSON 是否能被 Gson 解析。
 */
final class DefaultResourcesTest {
    private static final List<String> LEVELS = List.of(
            "worn", "common", "fine", "rare", "epic", "legendary", "mythic", "divine");
    private static final List<String> TYPES = List.of(
            "sword", "axe", "bow", "crossbow", "trident", "mace", "curio");
    private static final List<String> CATALYSTS =
            List.of("common_reforge_stone", "refined_reforge_stone", "supreme_reforge_stone");

    @Test
    void everyBundledDefinitionDecodesWithItsProductionCodec() {
        LEVELS.forEach(id -> decode("levels", id, LevelDefinition.CODEC));
        TYPES.forEach(id -> decode("item_types", id, ItemTypeDefinition.CODEC));
        CATALYSTS.forEach(id -> decode("catalysts", id, CatalystDefinition.CODEC));

        // 三套词条分别覆盖近战、远程与 Curios；每套都提供完整的 1~8 级案例。
        for (String family : List.of("melee", "ranged", "curio")) {
            for (int rank = 1; rank <= 8; rank++) {
                decode("modifiers", family + "_" + rank, ModifierDefinition.CODEC);
            }
        }
    }

    @Test
    void bundledDefinitionsHaveNoBrokenCrossReferences() {
        Map<ResourceLocation, LevelDefinition> levels = loadAll("levels", LEVELS, LevelDefinition.CODEC);
        Map<ResourceLocation, ItemTypeDefinition> types = loadAll("item_types", TYPES, ItemTypeDefinition.CODEC);
        Map<ResourceLocation, CatalystDefinition> catalysts =
                loadAll("catalysts", CATALYSTS, CatalystDefinition.CODEC);
        Map<ResourceLocation, ModifierDefinition> modifiers = new LinkedHashMap<>();
        for (String family : List.of("melee", "ranged", "curio")) {
            for (int rank = 1; rank <= 8; rank++) {
                String id = family + "_" + rank;
                modifiers.put(ResourceLocation.fromNamespaceAndPath("superreforge", id),
                        decode("modifiers", id, ModifierDefinition.CODEC));
            }
        }

        ValidationReport report = DefinitionValidator.validate(
                new DefinitionSnapshot(levels, types, modifiers, catalysts));

        assertTrue(report.isValid(), () -> "内置定义存在交叉引用错误：" + report.errors());
    }

    @Test
    void reforgerModelIsActuallyThreeDimensional() {
        JsonObject model = resourceJson("assets/superreforge/models/block/reforger.json");

        assertTrue(model.getAsJsonArray("elements").size() >= 18,
                "熔核锻台应由足够多的独立立方体组成，而不是普通整方块");
        assertTrue(model.getAsJsonObject("textures").has("core"), "模型必须包含发光熔核材质层");
    }

    @Test
    void reforgerBlockstateRotatesTheModelInAllFourDirections() {
        JsonObject variants = resourceJson("assets/superreforge/blockstates/reforger.json")
                .getAsJsonObject("variants");

        assertTrue(variants.has("facing=north"));
        assertTrue(variants.has("facing=east"));
        assertTrue(variants.has("facing=south"));
        assertTrue(variants.has("facing=west"));
    }

    private static <T> Map<ResourceLocation, T> loadAll(String folder, List<String> ids, Codec<T> codec) {
        Map<ResourceLocation, T> values = new LinkedHashMap<>();
        ids.forEach(id -> values.put(
                ResourceLocation.fromNamespaceAndPath("superreforge", id), decode(folder, id, codec)));
        return values;
    }

    private static <T> T decode(String folder, String id, Codec<T> codec) {
        JsonObject json = resourceJson("data/superreforge/superreforge/" + folder + "/" + id + ".json");
        return codec.parse(JsonOps.INSTANCE, json)
                .getOrThrow(error -> new AssertionError(folder + "/" + id + " 解码失败：" + error));
    }

    private static JsonObject resourceJson(String path) {
        var stream = DefaultResourcesTest.class.getClassLoader().getResourceAsStream(path);
        assertNotNull(stream, "缺少随模组发布的资源：" + path);
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception exception) {
            throw new AssertionError("无法读取资源：" + path, exception);
        }
    }
}
