package com.mutuo.superreforge.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

/** 验证前缀只动态修饰显示名，不覆盖铁砧维护的基础名称。 */
final class ModifierNameServiceTest {
    @Test
    void styledPrefixAppearsBeforeTheCurrentCustomName() {
        Component prefix = Component.literal("传说").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
        Component currentName = Component.literal("Excalibur");

        Component result = ModifierNameService.prefix(prefix, currentName);

        assertEquals("传说 Excalibur", result.getString());
        assertTrue(result.getSiblings().getFirst().getStyle().isBold());
    }
}
