package com.mutuo.superreforge.definition;

import java.util.Map;
import net.minecraft.resources.ResourceLocation;

/** 一次成功 reload 后原子发布的全部不可变定义。 */
public record DefinitionSnapshot(
        Map<ResourceLocation, LevelDefinition> levels,
        Map<ResourceLocation, ItemTypeDefinition> itemTypes,
        Map<ResourceLocation, ModifierDefinition> modifiers,
        Map<ResourceLocation, CatalystDefinition> catalysts) {
    public static final DefinitionSnapshot EMPTY =
            new DefinitionSnapshot(Map.of(), Map.of(), Map.of(), Map.of());

    public DefinitionSnapshot {
        levels = Map.copyOf(levels);
        itemTypes = Map.copyOf(itemTypes);
        modifiers = Map.copyOf(modifiers);
        catalysts = Map.copyOf(catalysts);
    }
}
