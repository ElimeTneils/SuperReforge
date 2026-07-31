package com.mutuo.superreforge.item;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mutuo.superreforge.definition.AttributeOperation;
import com.mutuo.superreforge.definition.SlotTarget;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
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
}
