package com.mutuo.superreforge.definition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;

/** 一个前缀词条，可同时包含任意数量且槽位互相独立的 Attribute 效果。 */
public record ModifierDefinition(
        ResourceLocation level,
        List<ResourceLocation> itemTypes,
        Component name,
        double weight,
        List<AttributeEffectDefinition> attributes) {
    public static final Codec<ModifierDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    ResourceLocation.CODEC.fieldOf("level").forGetter(ModifierDefinition::level),
                    ResourceLocation.CODEC.listOf().fieldOf("item_types").forGetter(ModifierDefinition::itemTypes),
                    ComponentSerialization.CODEC.fieldOf("name").forGetter(ModifierDefinition::name),
                    Codec.DOUBLE.optionalFieldOf("weight", 1.0).forGetter(ModifierDefinition::weight),
                    AttributeEffectDefinition.CODEC.listOf()
                            .optionalFieldOf("attributes", List.of())
                            .forGetter(ModifierDefinition::attributes))
            .apply(instance, ModifierDefinition::new));

    public ModifierDefinition {
        itemTypes = List.copyOf(itemTypes);
        attributes = List.copyOf(attributes);
    }
}
