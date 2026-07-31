package com.mutuo.superreforge.reforge;

import com.mutuo.superreforge.definition.DefinitionSnapshot;
import com.mutuo.superreforge.definition.ItemTypeDefinition;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** 解析物品当前匹配的所有类型；不采用“第一个命中”，因此词条池可以合并。 */
public final class ItemTypeResolver {
    private ItemTypeResolver() {}

    public static Set<ResourceLocation> resolve(ItemStack stack, DefinitionSnapshot snapshot) {
        Set<ResourceLocation> resolved = new LinkedHashSet<>();
        snapshot.itemTypes().forEach((id, definition) -> {
            if (matches(definition, stack)) {
                resolved.add(id);
            }
        });
        return Set.copyOf(resolved);
    }

    private static boolean matches(ItemTypeDefinition definition, ItemStack stack) {
        boolean included = definition.include().stream().anyMatch(selector -> SelectorMatcher.matches(selector, stack));
        boolean excluded = definition.exclude().stream().anyMatch(selector -> SelectorMatcher.matches(selector, stack));
        return included && !excluded;
    }
}
