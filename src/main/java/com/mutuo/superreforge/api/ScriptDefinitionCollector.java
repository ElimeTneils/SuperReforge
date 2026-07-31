package com.mutuo.superreforge.api;

import com.mutuo.superreforge.definition.CatalystDefinition;
import com.mutuo.superreforge.definition.DefinitionLayer;
import com.mutuo.superreforge.definition.ItemTypeDefinition;
import com.mutuo.superreforge.definition.LevelDefinition;
import com.mutuo.superreforge.definition.ModifierDefinition;
import com.mutuo.superreforge.progress.ProgressStage;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * KubeJS 一次 server_scripts reload 的临时收集器。
 *
 * <p>脚本执行期间只写这里；直到全部脚本无错误且定义通过交叉校验，插件才会原子发布构建结果。
 */
public final class ScriptDefinitionCollector {
    private final DefinitionLayer.Builder definitions = DefinitionLayer.builder();
    private final Map<String, Predicate<ItemStack>> predicates = new LinkedHashMap<>();
    private final Map<String, ProgressStage> stages = new LinkedHashMap<>();

    public synchronized void addLevel(ResourceLocation id, LevelDefinition definition) {
        definitions.level(id, definition);
    }

    public synchronized void addItemType(ResourceLocation id, ItemTypeDefinition definition) {
        definitions.itemType(id, definition);
    }

    public synchronized void addModifier(ResourceLocation id, ModifierDefinition definition) {
        definitions.modifier(id, definition);
    }

    public synchronized void addCatalyst(ResourceLocation id, CatalystDefinition definition) {
        definitions.catalyst(id, definition);
    }

    public synchronized void addPredicate(String id, Predicate<ItemStack> predicate) {
        requireId(id, "KubeJS predicate");
        if (predicates.putIfAbsent(id, predicate) != null) {
            throw new IllegalArgumentException("重复 KubeJS predicate ID: " + id);
        }
    }

    public synchronized void addStage(ProgressStage stage) {
        if (stages.putIfAbsent(stage.id(), stage) != null) {
            throw new IllegalArgumentException("重复进度阶段 ID: " + stage.id());
        }
    }

    /** 构建快照时复制全部容器，后续脚本不能修改已经准备发布的内容。 */
    public synchronized ScriptDefinitionBundle build() {
        return new ScriptDefinitionBundle(definitions.build(), predicates, stages);
    }

    private static void requireId(String id, String kind) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException(kind + " ID 不能为空");
        }
    }
}
