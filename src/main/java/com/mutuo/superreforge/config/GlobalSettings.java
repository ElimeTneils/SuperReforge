package com.mutuo.superreforge.config;

/**
 * 全局设置的不可变快照。
 *
 * <p>业务逻辑依赖该值对象而不是直接读取 TOML holder，因此单元测试和服务器重载都能获得
 * 一致、可复现的设置视图。
 */
public record GlobalSettings(
        boolean experienceEnabled,
        ExperienceMode experienceMode,
        boolean automaticInitialModifier,
        boolean showAttributeLines,
        int animationTicks,
        boolean creativePlayersPay,
        MissingDefinitionPolicy missingDefinitionPolicy) {
    /** 新世界和配置文件缺项时采用的设计规格默认值。 */
    public static final GlobalSettings DEFAULTS = new GlobalSettings(
            true,
            ExperienceMode.LEVELS,
            false,
            true,
            20,
            false,
            MissingDefinitionPolicy.REMOVE);

    /** 防止动画为零或负数而导致待揭晓事务永远无法正确推进。 */
    public GlobalSettings {
        if (animationTicks < 1) {
            throw new IllegalArgumentException("animationTicks 必须至少为 1");
        }
    }
}
