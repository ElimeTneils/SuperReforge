package com.mutuo.superreforge.reforge;

import com.mutuo.superreforge.SuperReforge;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import net.minecraft.world.item.ItemStack;

/** 为不直接依赖 Curios/KubeJS 的核心选择器保存可选匹配钩子。 */
public final class SelectorHooks {
    private static final Map<String, Predicate<ItemStack>> SCRIPT_PREDICATES = new ConcurrentHashMap<>();
    private static final Map<String, Long> LAST_ERROR_LOG = new ConcurrentHashMap<>();
    private static final long ERROR_LOG_INTERVAL_NANOS = TimeUnit.SECONDS.toNanos(60);
    private static volatile Predicate<ItemStack> curiosMatcher = stack -> false;

    private SelectorHooks() {}

    public static boolean testScript(String id, ItemStack stack) {
        Predicate<ItemStack> predicate = SCRIPT_PREDICATES.get(id);
        if (predicate == null) {
            return false;
        }
        try {
            return predicate.test(stack);
        } catch (RuntimeException error) {
            // 脚本是可选匹配条件：异常时安全地视为“不匹配”，并限制日志频率防止刷屏。
            long now = System.nanoTime();
            Long previous = LAST_ERROR_LOG.putIfAbsent(id, now);
            if (previous == null || now - previous >= ERROR_LOG_INTERVAL_NANOS) {
                LAST_ERROR_LOG.put(id, now);
                SuperReforge.LOGGER.error("KubeJS predicate {} failed; treating it as false", id, error);
            }
            return false;
        }
    }

    public static boolean testCurios(ItemStack stack) {
        return curiosMatcher.test(stack);
    }

    /** KubeJS reload 时先清空再登记，避免已删除脚本谓词残留。 */
    public static void replaceScriptPredicates(Map<String, Predicate<ItemStack>> predicates) {
        SCRIPT_PREDICATES.clear();
        SCRIPT_PREDICATES.putAll(predicates);
        LAST_ERROR_LOG.clear();
    }

    /** Curios 可选兼容层加载后注入实际栏位匹配逻辑。 */
    public static void setCuriosMatcher(Predicate<ItemStack> matcher) {
        curiosMatcher = matcher;
    }
}
