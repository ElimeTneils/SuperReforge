package com.mutuo.superreforge.reforge;

import net.minecraft.resources.ResourceLocation;

/** 一次抽取输出；相同 seed 与候选快照得到相同 modifierId。 */
public record RollResult(ResourceLocation modifierId, long seed) {}
