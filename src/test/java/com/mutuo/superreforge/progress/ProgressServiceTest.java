package com.mutuo.superreforge.progress;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mutuo.superreforge.reforge.CostModifier;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** 验证服务器同时激活多个阶段时只采用最高优先级阶段。 */
final class ProgressServiceTest {
    @Test
    void highestPriorityWinsAndIdBreaksTiesDeterministically() {
        ProgressStage early = new ProgressStage("early", 10, CostModifier.IDENTITY, CostModifier.IDENTITY);
        ProgressStage dragon = new ProgressStage("dragon", 100, new CostModifier(2, 1), new CostModifier(1.5, 0));
        ProgressStage tied = new ProgressStage("z_tied", 100, CostModifier.IDENTITY, CostModifier.IDENTITY);

        Optional<ProgressStage> selected = ProgressService.highestActive(
                List.of(early, tied, dragon), Set.of("early", "dragon", "z_tied"));

        assertEquals("dragon", selected.orElseThrow().id());
    }
}
