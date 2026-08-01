package com.mutuo.superreforge.reforge;

import com.mutuo.superreforge.definition.CatalystDefinition;
import net.minecraft.resources.ResourceLocation;

/** 对当前输入和当前定义快照计算出的服务端报价。 */
public record ReforgeQuote(
        ResourceLocation catalystId,
        CatalystDefinition catalyst,
        Cost cost,
        CandidatePool candidates) {}
