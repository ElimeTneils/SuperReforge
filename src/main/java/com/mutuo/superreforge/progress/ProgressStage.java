package com.mutuo.superreforge.progress;

import com.mutuo.superreforge.reforge.CostModifier;

/** KubeJS 可定义的一个全服进度阶段；材料与经验修正互相独立。 */
public record ProgressStage(
        String id, int priority, CostModifier materialCost, CostModifier experienceCost) {
    public ProgressStage {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("阶段 ID 不能为空");
        }
    }
}
