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
}
