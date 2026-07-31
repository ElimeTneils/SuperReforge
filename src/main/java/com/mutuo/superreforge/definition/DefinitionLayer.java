package com.mutuo.superreforge.definition;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

/**
 * 一层定义贡献。
 *
 * <p>datapack 和 KubeJS 各自构建独立层；合并时脚本层后写，因此同 ID 脚本定义覆盖 datapack。
 */
public record DefinitionLayer(
        Map<ResourceLocation, LevelDefinition> levels,
        Map<ResourceLocation, ItemTypeDefinition> itemTypes,
        Map<ResourceLocation, ModifierDefinition> modifiers,
        Map<ResourceLocation, CatalystDefinition> catalysts) {
    public static final DefinitionLayer EMPTY = new DefinitionLayer(Map.of(), Map.of(), Map.of(), Map.of());

    public DefinitionLayer {
        levels = Map.copyOf(levels);
        itemTypes = Map.copyOf(itemTypes);
        modifiers = Map.copyOf(modifiers);
        catalysts = Map.copyOf(catalysts);
    }

    public static Builder builder() {
        return new Builder();
    }

    /** 构建器在同一层内拒绝重复 ID，避免脚本顺序导致静默覆盖。 */
    public static final class Builder {
        private final Map<ResourceLocation, LevelDefinition> levels = new LinkedHashMap<>();
        private final Map<ResourceLocation, ItemTypeDefinition> itemTypes = new LinkedHashMap<>();
        private final Map<ResourceLocation, ModifierDefinition> modifiers = new LinkedHashMap<>();
        private final Map<ResourceLocation, CatalystDefinition> catalysts = new LinkedHashMap<>();

        public Builder level(ResourceLocation id, LevelDefinition definition) {
            putUnique(levels, id, definition, "level");
            return this;
        }

        public Builder itemType(ResourceLocation id, ItemTypeDefinition definition) {
            putUnique(itemTypes, id, definition, "item_type");
            return this;
        }

        public Builder modifier(ResourceLocation id, ModifierDefinition definition) {
            putUnique(modifiers, id, definition, "modifier");
            return this;
        }

        public Builder catalyst(ResourceLocation id, CatalystDefinition definition) {
            putUnique(catalysts, id, definition, "catalyst");
            return this;
        }

        public DefinitionLayer build() {
            return new DefinitionLayer(levels, itemTypes, modifiers, catalysts);
        }

        private static <T> void putUnique(
                Map<ResourceLocation, T> target, ResourceLocation id, T definition, String kind) {
            if (target.putIfAbsent(id, definition) != null) {
                throw new IllegalArgumentException("同一 " + kind + " 层重复定义 ID: " + id);
            }
        }
    }
}
