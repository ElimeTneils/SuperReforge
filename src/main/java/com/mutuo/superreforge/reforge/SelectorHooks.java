package com.mutuo.superreforge.reforge;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import net.minecraft.world.item.ItemStack;

/** 为不直接依赖 Curios/KubeJS 的核心选择器保存可选匹配钩子。 */
public final class SelectorHooks {
    private static final Map<String, Predicate<ItemStack>> SCRIPT_PREDICATES = new ConcurrentHashMap<>();
    private static volatile Predicate<ItemStack> curiosMatcher = stack -> false;

    private SelectorHooks() {}

    public static boolean testScript(String id, ItemStack stack) {
        Predicate<ItemStack> predicate = SCRIPT_PREDICATES.get(id);
        return predicate != null && predicate.test(stack);
    }

    public static boolean testCurios(ItemStack stack) {
        return curiosMatcher.test(stack);
    }

    /** KubeJS reload 时先清空再登记，避免已删除脚本谓词残留。 */
    public static void replaceScriptPredicates(Map<String, Predicate<ItemStack>> predicates) {
        SCRIPT_PREDICATES.clear();
        SCRIPT_PREDICATES.putAll(predicates);
    }

    /** Curios 可选兼容层加载后注入实际栏位匹配逻辑。 */
    public static void setCuriosMatcher(Predicate<ItemStack> matcher) {
        curiosMatcher = matcher;
    }
}
