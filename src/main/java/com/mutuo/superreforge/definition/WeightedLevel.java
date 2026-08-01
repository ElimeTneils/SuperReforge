package com.mutuo.superreforge.definition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

/** 媒介中一个品质等级的相对权重；最终概率由所有有效等级自动归一化。 */
public record WeightedLevel(ResourceLocation level, double weight) {
    public static final Codec<WeightedLevel> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    ResourceLocation.CODEC.fieldOf("level").forGetter(WeightedLevel::level),
                    Codec.DOUBLE.fieldOf("weight").forGetter(WeightedLevel::weight))
            .apply(instance, WeightedLevel::new));
}
