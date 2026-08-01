package com.mutuo.superreforge.definition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

/** 一个可由数据包任意命名、排序和着色的品质等级。 */
public record LevelDefinition(int rank, Component name) {
    public static final Codec<LevelDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.INT.fieldOf("rank").forGetter(LevelDefinition::rank),
                    ComponentSerialization.CODEC.fieldOf("name").forGetter(LevelDefinition::name))
            .apply(instance, LevelDefinition::new));
}
