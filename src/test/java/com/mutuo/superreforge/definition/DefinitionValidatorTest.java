package com.mutuo.superreforge.definition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

/** 验证坏数据会在发布快照前被一次性拒绝。 */
final class DefinitionValidatorTest {
    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("test", path);
    }

    @Test
    void reportsNegativeWeightUnknownLevelAndDuplicateEffectIdsTogether() {
        ModifierDefinition broken = new ModifierDefinition(
                id("missing_level"),
                List.of(id("sword")),
                Component.literal("损坏"),
                -2.0,
                List.of(
                        new AttributeEffectDefinition(
                                "same",
                                ResourceLocation.withDefaultNamespace("generic.attack_damage"),
                                new ValueDefinition.Fixed(1.0),
                                AttributeOperation.ADD_VALUE,
                                List.of(SlotTarget.MAINHAND),
                                true),
                        new AttributeEffectDefinition(
                                "same",
                                ResourceLocation.withDefaultNamespace("generic.attack_speed"),
                                new ValueDefinition.Range(2.0, 1.0),
                                AttributeOperation.ADD_VALUE,
                                List.of(SlotTarget.MAINHAND),
                                true)));
        DefinitionSnapshot snapshot = new DefinitionSnapshot(
                Map.of(),
                Map.of(id("sword"), new ItemTypeDefinition(List.of(), List.of())),
                Map.of(id("broken"), broken),
                Map.of());

        ValidationReport report = DefinitionValidator.validate(snapshot);

        assertEquals(4, report.errors().size());
        assertTrue(report.errors().stream().anyMatch(message -> message.contains("weight")));
        assertTrue(report.errors().stream().anyMatch(message -> message.contains("missing_level")));
        assertTrue(report.errors().stream().anyMatch(message -> message.contains("same")));
        assertTrue(report.errors().stream().anyMatch(message -> message.contains("min")));
    }

    @Test
    void acceptsZeroWeightAsAnExplicitDisabledEntry() {
        DefinitionSnapshot snapshot = new DefinitionSnapshot(
                Map.of(id("tier"), new LevelDefinition(1, Component.literal("一级"))),
                Map.of(id("sword"), new ItemTypeDefinition(List.of(), List.of())),
                Map.of(id("disabled"), new ModifierDefinition(
                        id("tier"), List.of(id("sword")), Component.literal("禁用"), 0.0, List.of())),
                Map.of());

        assertTrue(DefinitionValidator.validate(snapshot).isValid());
    }

    @Test
    void rejectsUnsafeEffectIdsAndDuplicateSlots() {
        ModifierDefinition broken = new ModifierDefinition(
                id("tier"),
                List.of(id("sword")),
                Component.literal("Broken"),
                1.0,
                List.of(new AttributeEffectDefinition(
                        "Attack Speed",
                        ResourceLocation.withDefaultNamespace("generic.attack_speed"),
                        new ValueDefinition.Fixed(1.0),
                        AttributeOperation.ADD_VALUE,
                        List.of(SlotTarget.MAINHAND, SlotTarget.MAINHAND),
                        true)));
        DefinitionSnapshot snapshot = new DefinitionSnapshot(
                Map.of(id("tier"), new LevelDefinition(1, Component.literal("Tier"))),
                Map.of(id("sword"), new ItemTypeDefinition(List.of(), List.of())),
                Map.of(id("broken"), broken),
                Map.of());

        ValidationReport report = DefinitionValidator.validate(snapshot);

        assertEquals(2, report.errors().size());
        assertTrue(report.errors().stream().anyMatch(message -> message.contains("effect id")));
        assertTrue(report.errors().stream().anyMatch(message -> message.contains("slots")));
    }

    @Test
    void rejectsEmptyPoolsAndMalformedOptionalSelectors() {
        ItemSelector malformed = new ItemSelector(
                java.util.Optional.empty(), List.of(), java.util.Optional.empty(),
                java.util.Optional.of("ring"), java.util.Optional.of(" "));
        DefinitionSnapshot snapshot = new DefinitionSnapshot(
                Map.of(id("tier"), new LevelDefinition(1, Component.literal("Tier"))),
                Map.of(id("bad_type"), new ItemTypeDefinition(List.of(malformed), List.of())),
                Map.of(id("bad_modifier"), new ModifierDefinition(
                        id("tier"), List.of(), Component.literal("Bad"), 1.0, List.of())),
                Map.of(id("bad_catalyst"), new CatalystDefinition(
                        ItemSelector.item(ResourceLocation.withDefaultNamespace("amethyst_shard")),
                        1, 0, false, List.of(), List.of(), List.of())));

        ValidationReport report = DefinitionValidator.validate(snapshot);

        assertEquals(4, report.errors().size());
        assertTrue(report.errors().stream().anyMatch(message -> message.contains("item_types")));
        assertTrue(report.errors().stream().anyMatch(message -> message.contains("levels")));
        assertTrue(report.errors().stream().anyMatch(message -> message.contains("curios")));
        assertTrue(report.errors().stream().anyMatch(message -> message.contains("kubejs_predicate")));
    }
}
