package com.mutuo.superreforge.reforge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

/** 保护“输入任意相对权重，自动除以合计”的核心概率契约。 */
final class WeightNormalizerTest {
    @Test
    void normalizesTwentyThirtyThirtyWithoutRequiringOneHundred() {
        List<WeightedValue<String>> normalized = WeightNormalizer.normalize(List.of(
                new WeightedValue<>("a", 20),
                new WeightedValue<>("b", 30),
                new WeightedValue<>("c", 30)));

        assertEquals(0.25, normalized.get(0).probability(), 1.0e-12);
        assertEquals(0.375, normalized.get(1).probability(), 1.0e-12);
        assertEquals(0.375, normalized.get(2).probability(), 1.0e-12);
    }

    @Test
    void dropsZeroWeightsAndRejectsNegativeOrNonFiniteWeights() {
        assertEquals(
                List.of("enabled"),
                WeightNormalizer.normalize(List.of(
                                new WeightedValue<>("off", 0),
                                new WeightedValue<>("enabled", 4)))
                        .stream()
                        .map(WeightedValue::value)
                        .toList());
        assertThrows(IllegalArgumentException.class, () ->
                WeightNormalizer.normalize(List.of(new WeightedValue<>("bad", -1))));
        assertThrows(IllegalArgumentException.class, () ->
                WeightNormalizer.normalize(List.of(new WeightedValue<>("bad", Double.NaN))));
    }
}
