package com.mutuo.superreforge.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.mutuo.superreforge.definition.LevelDefinition;
import com.mutuo.superreforge.progress.ProgressStage;
import com.mutuo.superreforge.reforge.CostModifier;
import java.util.function.Predicate;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;

/** 验证一次 KubeJS reload 的定义、谓词和阶段都在临时收集器中完整构建。 */
final class ScriptDefinitionCollectorTest {
    @Test
    void buildsOneImmutableReloadBundleAndRejectsDuplicateIds() {
        var collector = new ScriptDefinitionCollector();
        ResourceLocation levelId = ResourceLocation.fromNamespaceAndPath("example", "legendary");
        Predicate<ItemStack> predicate = stack -> !stack.isEmpty();
        ProgressStage stage = new ProgressStage(
                "dragon", 100, new CostModifier(2.0, 1), new CostModifier(1.5, 0));

        collector.addLevel(levelId, new LevelDefinition(6, Component.literal("传说")));
        collector.addPredicate("example:durable", predicate);
        collector.addStage(stage);
        ScriptDefinitionBundle bundle = collector.build();

        assertEquals(6, bundle.definitions().levels().get(levelId).rank());
        assertEquals(predicate, bundle.predicates().get("example:durable"));
        assertEquals(stage, bundle.stages().get("dragon"));
        assertThrows(IllegalArgumentException.class,
                () -> collector.addLevel(levelId, new LevelDefinition(7, Component.literal("重复"))));
    }
}
