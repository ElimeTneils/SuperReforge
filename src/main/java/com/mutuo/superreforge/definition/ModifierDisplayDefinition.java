package com.mutuo.superreforge.definition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

/**
 * 发给远程客户端的只读词条显示定义。
 *
 * <p>故意不包含等级、权重、适用类型、selector 或 KubeJS 谓词；这些服务端权威规则不会下发。
 */
public record ModifierDisplayDefinition(Component name, List<AttributeEffectDefinition> attributes) {
    public static final Codec<ModifierDisplayDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    ComponentSerialization.CODEC.fieldOf("name").forGetter(ModifierDisplayDefinition::name),
                    AttributeEffectDefinition.CODEC.listOf()
                            .fieldOf("attributes")
                            .forGetter(ModifierDisplayDefinition::attributes))
            .apply(instance, ModifierDisplayDefinition::new));

    public ModifierDisplayDefinition {
        attributes = List.copyOf(attributes);
    }

    public static ModifierDisplayDefinition from(ModifierDefinition definition) {
        return new ModifierDisplayDefinition(definition.name(), definition.attributes());
    }
}
