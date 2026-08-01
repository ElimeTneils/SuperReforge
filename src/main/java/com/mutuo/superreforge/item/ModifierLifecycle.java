package com.mutuo.superreforge.item;

import com.mutuo.superreforge.config.GlobalSettings;
import com.mutuo.superreforge.config.MissingDefinitionPolicy;
import com.mutuo.superreforge.definition.DefinitionSnapshot;
import com.mutuo.superreforge.reforge.CandidatePool;
import com.mutuo.superreforge.reforge.RollEngine;
import com.mutuo.superreforge.registry.ModDataComponents;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;

/** 对转换后物品执行词条合法性清理，并实现全局可选的首次自动抽取。 */
public final class ModifierLifecycle {
    private ModifierLifecycle() {}

    /**
     * 根据最新定义原地校正一个栈；返回值表示数据组件是否发生变化。
     *
     * <p>数量不为 1 时始终跳过，确保硬绑定木棍等可堆叠物品不会在整叠上共享一个词条。
     */
    public static boolean reconcile(
            ItemStack stack, DefinitionSnapshot snapshot, GlobalSettings settings, long seed) {
        if (stack.isEmpty() || stack.getCount() != 1) {
            return false;
        }

        boolean changed = false;
        ReforgeData saved = stack.get(ModDataComponents.REFORGE_DATA.get());
        if (saved != null) {
            if (ModifierResolver.definitionMatches(stack, snapshot, saved)) {
                if (!saved.active()) {
                    stack.set(ModDataComponents.REFORGE_DATA.get(), saved.withActive(true));
                    return true;
                }
                return false;
            }
            if (settings.missingDefinitionPolicy() == MissingDefinitionPolicy.KEEP_INACTIVE) {
                if (saved.active()) {
                    stack.set(ModDataComponents.REFORGE_DATA.get(), saved.withActive(false));
                    return true;
                }
                return false;
            }
            stack.remove(ModDataComponents.REFORGE_DATA.get());
            changed = true;
        }

        if (!settings.automaticInitialModifier()) {
            return changed;
        }
        var catalyst = snapshot.catalysts().get(settings.automaticCatalyst());
        if (catalyst == null) {
            return changed;
        }
        CandidatePool pool = CandidatePool.build(stack, catalyst, snapshot, Optional.empty());
        if (pool.isEmpty()) {
            return changed;
        }
        var result = RollEngine.roll(pool, seed);
        stack.set(
                ModDataComponents.REFORGE_DATA.get(),
                new ReforgeData(result.modifierId(), result.seed(), ReforgeData.CURRENT_SCHEMA));
        return true;
    }
}
