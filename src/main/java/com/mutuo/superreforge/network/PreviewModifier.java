package com.mutuo.superreforge.network;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** GUI 右栏中一个词条的服务端真实总概率。 */
public record PreviewModifier(ResourceLocation id, Component name, double probability) {}
