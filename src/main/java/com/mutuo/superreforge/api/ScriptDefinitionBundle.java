package com.mutuo.superreforge.api;

import com.mutuo.superreforge.definition.DefinitionLayer;
import com.mutuo.superreforge.progress.ProgressStage;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.world.item.ItemStack;

/** 一次成功 KubeJS 加载产生的不可变定义层、物品谓词和全服阶段定义。 */
public record ScriptDefinitionBundle(
        DefinitionLayer definitions,
        Map<String, Predicate<ItemStack>> predicates,
        Map<String, ProgressStage> stages) {
    public ScriptDefinitionBundle {
        predicates = Map.copyOf(predicates);
        stages = Map.copyOf(stages);
    }
}
