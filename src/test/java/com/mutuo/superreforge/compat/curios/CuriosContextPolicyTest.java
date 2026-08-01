package com.mutuo.superreforge.compat.curios;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** 确保 Curios 用于创意物品栏或 JEI 索引的合成上下文不会被当成实体上下文解引用。 */
final class CuriosContextPolicyTest {
    @Test
    void nullEntityIsASyntheticClientTooltipContext() {
        assertEquals(
                CuriosContextPolicy.Kind.SYNTHETIC_CLIENT,
                CuriosContextPolicy.classify(null));
    }
}
