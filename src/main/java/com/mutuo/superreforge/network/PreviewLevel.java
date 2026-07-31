package com.mutuo.superreforge.network;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** GUI 右栏中一个非空等级及其条件词条列表。 */
public record PreviewLevel(
        ResourceLocation id, Component name, double probability, List<PreviewModifier> modifiers) {
    public PreviewLevel {
        modifiers = List.copyOf(modifiers);
    }
}
