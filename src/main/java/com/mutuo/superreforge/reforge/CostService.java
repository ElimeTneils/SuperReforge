package com.mutuo.superreforge.reforge;

import com.mutuo.superreforge.config.GlobalSettings;
import com.mutuo.superreforge.definition.CatalystDefinition;
import com.mutuo.superreforge.progress.ProgressStage;
import java.util.Optional;

/** 把媒介基础成本、全局经验开关和最高优先级阶段合成为最终报价。 */
public final class CostService {
    private CostService() {}

    public static Cost quote(
            CatalystDefinition catalyst,
            GlobalSettings settings,
            Optional<ProgressStage> highestStage) {
        CostModifier material = highestStage.map(ProgressStage::materialCost).orElse(CostModifier.IDENTITY);
        CostModifier experience = highestStage.map(ProgressStage::experienceCost).orElse(CostModifier.IDENTITY);
        int materialCount = material.apply(catalyst.count());
        int experienceCount = settings.experienceEnabled()
                ? experience.apply(catalyst.experience())
                : 0;
        return new Cost(materialCount, experienceCount);
    }
}
