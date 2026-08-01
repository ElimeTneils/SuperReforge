package com.mutuo.superreforge.definition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** 验证 datapack 与脚本定义只会以完整、原子的快照对外可见。 */
final class DefinitionManagerTest {
    private static final ResourceLocation TIER =
            ResourceLocation.fromNamespaceAndPath("example", "tier");

    @BeforeEach
    void resetManager() {
        DefinitionManager.resetForTests();
    }

    @Test
    void scriptLayerOverridesDatapackDefinitionWithTheSameId() {
        DefinitionSnapshot datapack = new DefinitionSnapshot(
                Map.of(TIER, new LevelDefinition(1, Component.literal("数据包"))),
                Map.of(), Map.of(), Map.of());
        DefinitionLayer script = DefinitionLayer.builder()
                .level(TIER, new LevelDefinition(9, Component.literal("脚本")))
                .build();

        assertTrue(DefinitionManager.publishDatapack(datapack));
        assertTrue(DefinitionManager.replaceScriptLayer(script));

        assertEquals(9, DefinitionManager.snapshot().levels().get(TIER).rank());
        assertEquals("脚本", DefinitionManager.snapshot().levels().get(TIER).name().getString());
    }

    @Test
    void rejectedReloadKeepsTheLastValidSnapshot() {
        DefinitionSnapshot valid = new DefinitionSnapshot(
                Map.of(TIER, new LevelDefinition(1, Component.literal("有效"))),
                Map.of(), Map.of(), Map.of());
        ModifierDefinition invalidModifier = new ModifierDefinition(
                ResourceLocation.fromNamespaceAndPath("example", "missing"),
                List.of(), Component.literal("无效"), 1.0, List.of());
        DefinitionSnapshot invalid = new DefinitionSnapshot(
                Map.of(), Map.of(), Map.of(ResourceLocation.fromNamespaceAndPath("example", "bad"), invalidModifier), Map.of());

        assertTrue(DefinitionManager.publishDatapack(valid));
        assertFalse(DefinitionManager.publishDatapack(invalid));

        assertEquals("有效", DefinitionManager.snapshot().levels().get(TIER).name().getString());
    }

    @Test
    void layerBuilderRejectsDuplicateIdsInsteadOfSilentlyReplacingThem() {
        DefinitionLayer.Builder builder = DefinitionLayer.builder()
                .level(TIER, new LevelDefinition(1, Component.literal("首次")));

        assertThrows(IllegalArgumentException.class, () ->
                builder.level(TIER, new LevelDefinition(2, Component.literal("重复"))));
    }

    @Test
    void clientModifierMirrorCanBeInstalledAndClearedIndependently() {
        ResourceLocation modifierId = ResourceLocation.fromNamespaceAndPath("example", "client_visible");
        DefinitionSnapshot serverSnapshot = new DefinitionSnapshot(
                Map.of(TIER, new LevelDefinition(1, Component.literal("服务端保留"))),
                Map.of(), Map.of(), Map.of());
        ModifierDefinition modifier = new ModifierDefinition(
                TIER, List.of(ResourceLocation.fromNamespaceAndPath("example", "type")),
                Component.literal("客户端可见"), 1.0, List.of());

        assertTrue(DefinitionManager.publishDatapack(serverSnapshot));
        ModifierDisplayDefinition display = ModifierDisplayDefinition.from(modifier);
        DefinitionManager.installClientModifiers(Map.of(modifierId, display));
        assertEquals(display, DefinitionManager.clientModifiers().get(modifierId));

        DefinitionManager.clearClientSession();
        assertTrue(DefinitionManager.clientModifiers().isEmpty());
        assertEquals("服务端保留", DefinitionManager.snapshot().levels().get(TIER).name().getString());
    }

    @Test
    void successfulServerPublicationsAdvanceSnapshotGeneration() {
        long before = DefinitionManager.generation();
        DefinitionSnapshot datapack = new DefinitionSnapshot(
                Map.of(TIER, new LevelDefinition(1, Component.literal("版本"))),
                Map.of(), Map.of(), Map.of());

        assertTrue(DefinitionManager.publishDatapack(datapack));
        assertEquals(before + 1, DefinitionManager.generation());
    }
}
