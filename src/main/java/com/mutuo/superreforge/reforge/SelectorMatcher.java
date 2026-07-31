package com.mutuo.superreforge.reforge;

import com.mutuo.superreforge.definition.ItemSelector;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;

/** 执行一个数据选择器；对象内任意规则命中即为匹配。 */
public final class SelectorMatcher {
    private SelectorMatcher() {}

    public static boolean matches(ItemSelector selector, ItemStack stack) {
        var itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (selector.item().filter(itemId::equals).isPresent() || selector.items().contains(itemId)) {
            return true;
        }
        if (selector.tag().isPresent()
                && stack.is(TagKey.create(Registries.ITEM, selector.tag().orElseThrow()))) {
            return true;
        }
        if (selector.curios().filter("any"::equalsIgnoreCase).isPresent() && SelectorHooks.testCurios(stack)) {
            return true;
        }
        return selector.kubejsPredicate().filter(id -> SelectorHooks.testScript(id, stack)).isPresent();
    }
}
