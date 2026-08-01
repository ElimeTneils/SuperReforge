package com.mutuo.superreforge.definition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.resources.ResourceLocation;

/** 一个词条内部的单条 Attribute 效果；ID 用于稳定派生随机值和 modifier 资源 ID。 */
public record AttributeEffectDefinition(
        String id,
        ResourceLocation attribute,
        ValueDefinition amount,
        AttributeOperation operation,
        List<SlotTarget> slots,
        boolean showInTooltip) {
    public static final Codec<AttributeEffectDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.STRING.fieldOf("id").forGetter(AttributeEffectDefinition::id),
                    ResourceLocation.CODEC.fieldOf("attribute").forGetter(AttributeEffectDefinition::attribute),
                    ValueDefinition.CODEC.fieldOf("amount").forGetter(AttributeEffectDefinition::amount),
                    AttributeOperation.CODEC.fieldOf("operation").forGetter(AttributeEffectDefinition::operation),
                    SlotTarget.CODEC.listOf().fieldOf("slots").forGetter(AttributeEffectDefinition::slots),
                    Codec.BOOL.optionalFieldOf("show_in_tooltip", true)
                            .forGetter(AttributeEffectDefinition::showInTooltip))
            .apply(instance, AttributeEffectDefinition::new));

    public AttributeEffectDefinition {
        slots = List.copyOf(slots);
    }
}
