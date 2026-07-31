package com.mutuo.superreforge.reforge;

import java.util.List;
import java.util.SplittableRandom;
import net.minecraft.resources.ResourceLocation;

/** 先抽等级、再在该等级内抽词条的服务端确定性引擎。 */
public final class RollEngine {
    private RollEngine() {}

    public static RollResult roll(CandidatePool pool, long seed) {
        if (pool.isEmpty()) {
            throw new IllegalArgumentException("候选池为空，不能抽取词条");
        }
        SplittableRandom random = new SplittableRandom(seed);
        List<WeightedValue<CandidateLevel>> levels = WeightNormalizer.normalize(pool.levels().stream()
                .map(level -> new WeightedValue<>(level, level.weight()))
                .toList());
        CandidateLevel chosenLevel = choose(levels, random.nextDouble());
        ResourceLocation modifierId = choose(
                WeightNormalizer.normalize(chosenLevel.modifiers()), random.nextDouble());
        return new RollResult(modifierId, seed);
    }

    private static <T> T choose(List<WeightedValue<T>> values, double roll) {
        double cumulative = 0.0;
        for (WeightedValue<T> value : values) {
            cumulative += value.probability();
            if (roll < cumulative) {
                return value.value();
            }
        }
        // 浮点累积可能只差最后一个 ulp；回退到最后条目而不是制造随机失败。
        return values.getLast().value();
    }
}
