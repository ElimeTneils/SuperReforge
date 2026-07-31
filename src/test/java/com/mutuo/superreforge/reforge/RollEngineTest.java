package com.mutuo.superreforge.reforge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.mutuo.superreforge.definition.ValueDefinition;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

/** 验证两阶段抽取和旧物品数值重映射均只依赖稳定种子。 */
final class RollEngineTest {
    @Test
    void sameSeedAlwaysChoosesTheSameModifier() {
        CandidatePool pool = new CandidatePool(List.of(
                new CandidateLevel(
                        ResourceLocation.fromNamespaceAndPath("test", "tier"),
                        1,
                        List.of(
                                new WeightedValue<>(ResourceLocation.fromNamespaceAndPath("test", "a"), 1),
                                new WeightedValue<>(ResourceLocation.fromNamespaceAndPath("test", "b"), 2)))));

        assertEquals(RollEngine.roll(pool, 42L), RollEngine.roll(pool, 42L));
    }

    @Test
    void stableEffectSaltRecalculatesInsideTheLatestRange() {
        double oldValue = DeterministicValue.resolve(new ValueDefinition.Range(0.02, 0.04), 99L, "speed");
        double newValue = DeterministicValue.resolve(new ValueDefinition.Range(0.10, 0.20), 99L, "speed");
        double repeated = DeterministicValue.resolve(new ValueDefinition.Range(0.10, 0.20), 99L, "speed");

        assertNotEquals(oldValue, newValue);
        assertEquals(newValue, repeated);
        assertEquals(0.15, DeterministicValue.resolve(new ValueDefinition.Fixed(0.15), 1L, "ignored"));
    }
}
