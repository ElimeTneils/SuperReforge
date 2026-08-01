package com.mutuo.superreforge.reforge;

import com.mutuo.superreforge.definition.ValueDefinition;
import java.util.SplittableRandom;

/** 用物品种子与稳定 effect ID 派生数值，使定义范围变化后旧物品可确定性重算。 */
public final class DeterministicValue {
    private DeterministicValue() {}

    public static double resolve(ValueDefinition definition, long itemSeed, String effectId) {
        if (definition instanceof ValueDefinition.Fixed fixed) {
            return fixed.value();
        }
        ValueDefinition.Range range = (ValueDefinition.Range) definition;
        long derivedSeed = mix64(itemSeed ^ stableStringHash(effectId));
        return range.min() + new SplittableRandom(derivedSeed).nextDouble() * (range.max() - range.min());
    }

    private static long stableStringHash(String value) {
        long hash = 0xcbf29ce484222325L;
        for (int index = 0; index < value.length(); index++) {
            hash ^= value.charAt(index);
            hash *= 0x100000001b3L;
        }
        return hash;
    }

    private static long mix64(long value) {
        value = (value ^ (value >>> 30)) * 0xbf58476d1ce4e5b9L;
        value = (value ^ (value >>> 27)) * 0x94d049bb133111ebL;
        return value ^ (value >>> 31);
    }
}
