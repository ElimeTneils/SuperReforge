package com.mutuo.superreforge.item;

import com.mutuo.superreforge.definition.AttributeOperation;
import com.mutuo.superreforge.definition.SlotTarget;
import java.util.List;
import net.minecraft.resources.ResourceLocation;

/** 已用物品种子求值，但尚未绑定具体 Attribute holder 的单条效果。 */
public record ResolvedEffect(
        String id,
        ResourceLocation attribute,
        double amount,
        AttributeOperation operation,
        List<SlotTarget> slots,
        boolean showInTooltip) {
    public ResolvedEffect {
        slots = List.copyOf(slots);
    }
}
