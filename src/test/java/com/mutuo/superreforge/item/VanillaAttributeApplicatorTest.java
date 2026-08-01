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
    void buildsSlotSpecificStableEffectIdsForVanillaAndCuriosBridges() {
        var modifier = ResourceLocation.fromNamespaceAndPath("example", "legendary/blade");

        assertEquals(
                ResourceLocation.fromNamespaceAndPath(
                        "superreforge", "effect/example/legendary/blade/attack_speed/mainhand"),
                VanillaAttributeApplicator.stableModifierId(modifier, "attack_speed", SlotTarget.MAINHAND));
        assertEquals(
                ResourceLocation.fromNamespaceAndPath(
                        "superreforge", "effect/example/legendary/blade/attack_speed/curio/charm/2"),
                VanillaAttributeApplicator.curiosModifierId(
                        modifier,
                        "attack_speed",
                        "charm",
                        2));
    }

    @Test
    void twoCuriosOfTheSameTypeUseDifferentModifierIds() {
        var modifier = ResourceLocation.fromNamespaceAndPath("example", "legendary/ring");

        var first = VanillaAttributeApplicator.curiosModifierId(modifier, "health", "ring", 0);
        var second = VanillaAttributeApplicator.curiosModifierId(modifier, "health", "ring", 1);

        org.junit.jupiter.api.Assertions.assertNotEquals(first, second);

        // 使用真实 AttributeInstance 证明两个同类栏位不会仅停留在“ID 看起来不同”，而是会同时计入数值。
        var instance = new net.minecraft.world.entity.ai.attributes.AttributeInstance(
                net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH, ignored -> {});
        instance.setBaseValue(10.0);
        instance.addOrUpdateTransientModifier(new AttributeModifier(
                first, 2.0, AttributeModifier.Operation.ADD_VALUE));
        instance.addOrUpdateTransientModifier(new AttributeModifier(
                second, 3.0, AttributeModifier.Operation.ADD_VALUE));
        assertEquals(15.0, instance.getValue());
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
                List.of(VanillaAttributeApplicator.stableModifierId(
                        modifierId, "hidden", SlotTarget.MAINHAND)),
                VanillaAttributeApplicator.hiddenEffectIds(modifier, true));
        assertEquals(2, VanillaAttributeApplicator.hiddenEffectIds(modifier, false).size());
    }
}
