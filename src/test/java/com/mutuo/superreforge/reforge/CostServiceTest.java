package com.mutuo.superreforge.reforge;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mutuo.superreforge.config.ExperienceMode;
import com.mutuo.superreforge.config.GlobalSettings;
import com.mutuo.superreforge.config.MissingDefinitionPolicy;
import com.mutuo.superreforge.definition.CatalystDefinition;
import com.mutuo.superreforge.definition.ItemSelector;
import com.mutuo.superreforge.progress.ProgressStage;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

/** 验证材料和经验能分别应用乘数/加数，且经验全局关闭后归零。 */
final class CostServiceTest {
    private static final CatalystDefinition CATALYST = new CatalystDefinition(
            ItemSelector.item(ResourceLocation.withDefaultNamespace("diamond")),
            2,
            5,
            false,
            List.of(),
            List.of(),
            List.of());

    @Test
    void stageAdjustsMaterialAndExperienceIndependently() {
        ProgressStage stage = new ProgressStage(
                "dragon", 100, new CostModifier(2.0, 1), new CostModifier(1.5, 2));

        Cost cost = CostService.quote(CATALYST, settings(true), Optional.of(stage));

        assertEquals(5, cost.materialCount());
        assertEquals(9, cost.experience());
    }

    @Test
    void disabledExperienceAlwaysQuotesZeroXp() {
        Cost cost = CostService.quote(CATALYST, settings(false), Optional.empty());

        assertEquals(2, cost.materialCount());
        assertEquals(0, cost.experience());
    }

    private static GlobalSettings settings(boolean xpEnabled) {
        return new GlobalSettings(
                xpEnabled, ExperienceMode.LEVELS, false, true, 20, false, MissingDefinitionPolicy.REMOVE);
    }
}
