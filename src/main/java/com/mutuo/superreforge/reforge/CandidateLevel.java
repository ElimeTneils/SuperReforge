package com.mutuo.superreforge.reforge;

import java.util.List;
import net.minecraft.resources.ResourceLocation;

/** 两阶段抽取中的一个非空等级桶。 */
public record CandidateLevel(
        ResourceLocation level, double weight, List<WeightedValue<ResourceLocation>> modifiers) {
    public CandidateLevel {
        modifiers = List.copyOf(modifiers);
    }
}
