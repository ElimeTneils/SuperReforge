package com.mutuo.superreforge.definition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.resources.ResourceLocation;

/** 一种重铸媒介的输入、固定成本、类型限制和可抽取等级权重。 */
public record CatalystDefinition(
        ItemSelector ingredient,
        int count,
        int experience,
        boolean allowSameModifier,
        List<ResourceLocation> allowedItemTypes,
        List<ResourceLocation> deniedItemTypes,
        List<WeightedLevel> levels) {
    public static final Codec<CatalystDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    ItemSelector.CODEC.fieldOf("ingredient").forGetter(CatalystDefinition::ingredient),
                    Codec.INT.fieldOf("count").forGetter(CatalystDefinition::count),
                    Codec.INT.optionalFieldOf("experience", 0).forGetter(CatalystDefinition::experience),
                    Codec.BOOL.optionalFieldOf("allow_same_modifier", false)
                            .forGetter(CatalystDefinition::allowSameModifier),
                    ResourceLocation.CODEC.listOf()
                            .optionalFieldOf("allowed_item_types", List.of())
                            .forGetter(CatalystDefinition::allowedItemTypes),
                    ResourceLocation.CODEC.listOf()
                            .optionalFieldOf("denied_item_types", List.of())
                            .forGetter(CatalystDefinition::deniedItemTypes),
                    WeightedLevel.CODEC.listOf().fieldOf("levels").forGetter(CatalystDefinition::levels))
            .apply(instance, CatalystDefinition::new));

    public CatalystDefinition {
        allowedItemTypes = List.copyOf(allowedItemTypes);
        deniedItemTypes = List.copyOf(deniedItemTypes);
        levels = List.copyOf(levels);
    }
}
