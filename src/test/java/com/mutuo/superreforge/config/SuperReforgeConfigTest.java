package com.mutuo.superreforge.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** 验证新世界默认配置保持设计规格所承诺的安全行为。 */
final class SuperReforgeConfigTest {
    @Test
    void defaultsRequireTableAndUseLevelExperience() {
        GlobalSettings defaults = GlobalSettings.DEFAULTS;

        assertTrue(defaults.experienceEnabled());
        assertEquals(ExperienceMode.LEVELS, defaults.experienceMode());
        assertFalse(defaults.automaticInitialModifier());
        assertTrue(defaults.showAttributeLines());
        assertEquals(20, defaults.animationTicks());
        assertEquals(MissingDefinitionPolicy.REMOVE, defaults.missingDefinitionPolicy());
    }
}
