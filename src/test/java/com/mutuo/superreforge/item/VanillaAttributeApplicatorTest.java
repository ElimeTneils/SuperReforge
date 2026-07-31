package com.mutuo.superreforge.item;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mutuo.superreforge.definition.AttributeOperation;
import com.mutuo.superreforge.definition.SlotTarget;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

/** 防止 JSON 运算或槽位被接到错误的原版枚举。 */
final class VanillaAttributeApplicatorTest {
    @Test
    void mapsAllSupportedOperationsAndMainhandSlot() {
        assertEquals(AttributeModifier.Operation.ADD_VALUE,
                VanillaAttributeApplicator.operation(AttributeOperation.ADD_VALUE));
        assertEquals(AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                VanillaAttributeApplicator.operation(AttributeOperation.ADD_MULTIPLIED_BASE));
        assertEquals(AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
                VanillaAttributeApplicator.operation(AttributeOperation.ADD_MULTIPLIED_TOTAL));
        assertEquals(EquipmentSlotGroup.MAINHAND, VanillaAttributeApplicator.slot(SlotTarget.MAINHAND));
    }

    @Test
    void buildsTheSameStableEffectIdForVanillaAndCuriosBridges() {
        var modifier = ResourceLocation.fromNamespaceAndPath("example", "legendary/blade");

        assertEquals(
                ResourceLocation.fromNamespaceAndPath(
                        "superreforge", "effect/example/legendary/blade/attack_speed_bonus"),
                VanillaAttributeApplicator.stableModifierId(modifier, "Attack Speed Bonus"));
    }

    @Test
    void tooltipPolicyHidesOnlyDisabledEffectsUnlessGlobalDisplayIsOff() {
        var modifierId = ResourceLocation.fromNamespaceAndPath("example", "mixed");
        var visible = new ResolvedEffect(
                "visible", ResourceLocation.withDefaultNamespace("generic.attack_damage"), 1,
                AttributeOperation.ADD_VALUE, List.of(SlotTarget.MAINHAND), true);
        var hidden = new ResolvedEffect(
                "hidden", ResourceLocation.withDefaultNamespace("generic.attack_speed"), 1,
                AttributeOperation.ADD_VALUE, List.of(SlotTarget.MAINHAND), false);
        var modifier = new ResolvedModifier(modifierId, Component.literal("混合"), List.of(visible, hidden));

        assertEquals(
                List.of(VanillaAttributeApplicator.stableModifierId(modifierId, "hidden")),
                VanillaAttributeApplicator.hiddenEffectIds(modifier, true));
        assertEquals(2, VanillaAttributeApplicator.hiddenEffectIds(modifier, false).size());
    }
}
