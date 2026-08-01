package com.mutuo.superreforge.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mutuo.superreforge.definition.AttributeEffectDefinition;
import com.mutuo.superreforge.definition.AttributeOperation;
import com.mutuo.superreforge.definition.ModifierDisplayDefinition;
import com.mutuo.superreforge.definition.SlotTarget;
import com.mutuo.superreforge.definition.ValueDefinition;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

/** 验证候选词条悬停信息完整展示每条 Attribute 的范围、算法和槽位。 */
final class ModifierTooltipFormatterTest {
    @Test
    void formatsFixedAndPercentageRangeEffectsWithoutRollingFutureValues() {
        ResourceLocation modifierId = id("legendary");
        ModifierDisplayDefinition definition = new ModifierDisplayDefinition(
                Component.literal("传说"),
                List.of(
                        effect(
                                "damage",
                                ResourceLocation.withDefaultNamespace("generic.attack_damage"),
                                new ValueDefinition.Fixed(4.0),
                                AttributeOperation.ADD_VALUE,
                                SlotTarget.MAINHAND),
                        effect(
                                "speed",
                                id("curio_speed"),
                                new ValueDefinition.Range(0.02, 0.04),
                                AttributeOperation.ADD_MULTIPLIED_BASE,
                                SlotTarget.CURIOS_ANY)));

        List<Component> lines = ModifierTooltipFormatter.format(modifierId, definition);
        String text = lines.stream().map(Component::getString).collect(Collectors.joining("\n"));

        assertTrue(text.contains("传说"));
        assertTrue(text.contains("test:legendary"));
        assertTrue(text.contains("minecraft:generic.attack_damage"));
        assertTrue(text.contains("test:curio_speed"));
        assertTrue(text.contains("4"));
        assertTrue(text.contains("2%"));
        assertTrue(text.contains("4%"));
        assertTrue(text.contains("gui.superreforge.tooltip.operation.add_value"));
        assertTrue(text.contains("gui.superreforge.tooltip.operation.add_multiplied_base"));
        assertTrue(text.contains("gui.superreforge.tooltip.slot.mainhand"));
        assertTrue(text.contains("gui.superreforge.tooltip.slot.curios_any"));
    }

    @Test
    void reportsMissingSynchronizedDefinitionWithoutCrashing() {
        List<Component> lines = ModifierTooltipFormatter.format(id("missing"), null);
        String text = lines.stream().map(Component::getString).collect(Collectors.joining("\n"));

        assertEquals(3, lines.size());
        assertTrue(text.contains("test:missing"));
        assertTrue(text.contains("gui.superreforge.tooltip.definition_unavailable"));
    }

    private static AttributeEffectDefinition effect(
            String effectId,
            ResourceLocation attribute,
            ValueDefinition amount,
            AttributeOperation operation,
            SlotTarget slot) {
        return new AttributeEffectDefinition(effectId, attribute, amount, operation, List.of(slot), true);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("test", path);
    }
}
