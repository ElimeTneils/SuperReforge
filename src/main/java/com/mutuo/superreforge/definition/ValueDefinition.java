package com.mutuo.superreforge.definition;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Attribute 数值可以是常量，也可以是由物品种子确定的闭区间随机值。 */
public sealed interface ValueDefinition permits ValueDefinition.Fixed, ValueDefinition.Range {
    Codec<ValueDefinition> CODEC = Codec.either(Codec.DOUBLE, Range.CODEC).xmap(
            either -> either.map(Fixed::new, range -> range),
            value -> value instanceof Fixed fixed
                    ? Either.left(fixed.value())
                    : Either.right((Range) value));

    /** 永远返回同一个数值。 */
    record Fixed(double value) implements ValueDefinition {}

    /** 使用稳定种子在 min 与 max 之间映射数值。 */
    record Range(double min, double max) implements ValueDefinition {
        private static final Codec<Range> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                        Codec.DOUBLE.fieldOf("min").forGetter(Range::min),
                        Codec.DOUBLE.fieldOf("max").forGetter(Range::max))
                .apply(instance, Range::new));
    }
}
