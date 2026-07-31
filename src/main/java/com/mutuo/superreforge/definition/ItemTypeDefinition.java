package com.mutuo.superreforge.definition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;

/** 由 include 任意命中、exclude 优先否决的一种可重铸物品类型。 */
public record ItemTypeDefinition(List<ItemSelector> include, List<ItemSelector> exclude) {
    public static final Codec<ItemTypeDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    ItemSelector.CODEC.listOf().optionalFieldOf("include", List.of()).forGetter(ItemTypeDefinition::include),
                    ItemSelector.CODEC.listOf().optionalFieldOf("exclude", List.of()).forGetter(ItemTypeDefinition::exclude))
            .apply(instance, ItemTypeDefinition::new));

    public ItemTypeDefinition {
        include = List.copyOf(include);
        exclude = List.copyOf(exclude);
    }
}
