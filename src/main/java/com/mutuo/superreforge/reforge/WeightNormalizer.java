package com.mutuo.superreforge.reforge;

import java.util.ArrayList;
import java.util.List;

/** 把任意非负有限相对权重转换为总和 1.0 的真实概率。 */
public final class WeightNormalizer {
    private WeightNormalizer() {}

    public static <T> List<WeightedValue<T>> normalize(List<WeightedValue<T>> values) {
        List<WeightedValue<T>> enabled = new ArrayList<>();
        double total = 0.0;
        for (WeightedValue<T> value : values) {
            if (!Double.isFinite(value.weight()) || value.weight() < 0.0) {
                throw new IllegalArgumentException("weight 必须是非负有限数: " + value.weight());
            }
            if (value.weight() > 0.0) {
                enabled.add(value);
                total += value.weight();
            }
        }
        if (enabled.isEmpty() || !Double.isFinite(total) || total <= 0.0) {
            return List.of();
        }
        double divisor = total;
        return enabled.stream()
                .map(value -> new WeightedValue<>(value.value(), value.weight(), value.weight() / divisor))
                .toList();
    }
}
