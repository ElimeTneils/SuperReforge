package com.mutuo.superreforge.item;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

/** 验证 reload 清理器只接管 Super Reforge 自己创建的 Attribute modifier。 */
final class AttributeRefreshServiceTest {
    @Test
    void recognizesOnlyOwnedEffectIds() {
        assertTrue(AttributeRefreshService.isOwnedModifier(
                ResourceLocation.fromNamespaceAndPath("superreforge", "effect/example/legendary/damage/mainhand")));
        assertFalse(AttributeRefreshService.isOwnedModifier(
                ResourceLocation.fromNamespaceAndPath("superreforge", "other/example")));
        assertFalse(AttributeRefreshService.isOwnedModifier(
                ResourceLocation.fromNamespaceAndPath("another_mod", "effect/example")));
    }
}
