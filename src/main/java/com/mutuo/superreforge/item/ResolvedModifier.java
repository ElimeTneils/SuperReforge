package com.mutuo.superreforge.item;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** 某个物品在当前定义快照下动态解析出的名称与全部效果。 */
public record ResolvedModifier(ResourceLocation id, Component name, List<ResolvedEffect> effects) {
    public ResolvedModifier {
        effects = List.copyOf(effects);
    }
}
