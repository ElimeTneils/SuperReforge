package com.mutuo.superreforge.reforge;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Map;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** 验证可选脚本失败时只让当前选择器不匹配，绝不能中断重铸或数据重载。 */
final class SelectorHooksTest {
    @AfterEach
    void clearPredicates() {
        SelectorHooks.replaceScriptPredicates(Map.of());
    }

    @Test
    void runtimeExceptionFromKubeJsPredicateFailsClosed() {
        SelectorHooks.replaceScriptPredicates(Map.of("broken", stack -> {
            throw new IllegalStateException("script failure");
        }));

        assertFalse(SelectorHooks.testScript("broken", new ItemStack(Items.STICK)));
    }
}
