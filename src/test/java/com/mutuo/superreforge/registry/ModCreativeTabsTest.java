package com.mutuo.superreforge.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

/** 验证独立创造页签的可见内容、顺序与本地化标题。 */
final class ModCreativeTabsTest {
    @Test
    void exposesOnlyTheReforgerAndSixCatalystsInProgressionOrder() {
        List<ResourceLocation> visible = ModCreativeTabs.visibleItemIds();

        assertEquals(List.of(
                id("reforger"),
                id("common_reforge_stone"),
                id("refined_reforge_stone"),
                id("supreme_reforge_stone"),
                id("enhancement_stone_4"),
                id("enhancement_stone_5"),
                id("enhancement_stone_6")), visible);
        assertFalse(visible.contains(id("forge_hammer")));
    }

    @Test
    void bothLanguageFilesNameTheIndependentTab() throws IOException {
        assertEquals("Super Reforge", language("zh_cn").get("itemGroup.superreforge").getAsString());
        assertEquals("Super Reforge", language("en_us").get("itemGroup.superreforge").getAsString());
        assertEquals("强化石 4级", language("zh_cn").get("item.superreforge.enhancement_stone_4").getAsString());
        assertEquals("强化石 5级", language("zh_cn").get("item.superreforge.enhancement_stone_5").getAsString());
        assertEquals("强化石 6级", language("zh_cn").get("item.superreforge.enhancement_stone_6").getAsString());
    }

    private static JsonObject language(String locale) throws IOException {
        String path = "assets/superreforge/lang/" + locale + ".json";
        try (var stream = ModCreativeTabsTest.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                throw new IOException("缺少语言资源: " + path);
            }
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("superreforge", path);
    }
}
