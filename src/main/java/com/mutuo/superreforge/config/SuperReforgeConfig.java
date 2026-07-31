package com.mutuo.superreforge.config;

import org.apache.commons.lang3.tuple.Pair;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 声明服务器级 TOML 配置。
 *
 * <p>文件最终位于世界的 {@code serverconfig/superreforge-server.toml}；这里只放跨内容包的
 * 全局行为，等级、词条、媒介和类型仍由 datapack/KubeJS 定义。
 */
public final class SuperReforgeConfig {
    public static final ServerValues SERVER;
    public static final ModConfigSpec SERVER_SPEC;

    static {
        Pair<ServerValues, ModConfigSpec> configured =
                new ModConfigSpec.Builder().configure(ServerValues::new);
        SERVER = configured.getLeft();
        SERVER_SPEC = configured.getRight();
    }

    private SuperReforgeConfig() {}

    /** 将当前 TOML holder 复制为业务代码使用的不可变快照。 */
    public static GlobalSettings snapshot() {
        return new GlobalSettings(
                SERVER.experienceEnabled.get(),
                SERVER.experienceMode.get(),
                SERVER.automaticInitialModifier.get(),
                SERVER.showAttributeLines.get(),
                SERVER.animationTicks.get(),
                SERVER.creativePlayersPay.get(),
                SERVER.missingDefinitionPolicy.get());
    }

    /** 保存 NeoForge 配置值；每个注释都会原样帮助服主理解生成的 TOML。 */
    public static final class ServerValues {
        private final ModConfigSpec.BooleanValue experienceEnabled;
        private final ModConfigSpec.EnumValue<ExperienceMode> experienceMode;
        private final ModConfigSpec.BooleanValue automaticInitialModifier;
        private final ModConfigSpec.BooleanValue showAttributeLines;
        private final ModConfigSpec.IntValue animationTicks;
        private final ModConfigSpec.BooleanValue creativePlayersPay;
        private final ModConfigSpec.EnumValue<MissingDefinitionPolicy> missingDefinitionPolicy;

        private ServerValues(ModConfigSpec.Builder builder) {
            GlobalSettings defaults = GlobalSettings.DEFAULTS;
            builder.push("general");
            experienceEnabled = builder
                    .comment("是否启用重铸经验消耗。关闭后只消耗媒介。")
                    .define("experienceEnabled", defaults.experienceEnabled());
            experienceMode = builder
                    .comment("LEVELS 按等级扣除；POINTS 按精确经验点扣除。")
                    .defineEnum("experienceMode", defaults.experienceMode());
            automaticInitialModifier = builder
                    .comment("是否允许物品不经过熔核锻台自动获得首个词条。")
                    .define("automaticInitialModifier", defaults.automaticInitialModifier());
            showAttributeLines = builder
                    .comment("是否显示原版和 Curios 自动生成的 Attribute tooltip 行。")
                    .define("showAttributeLines", defaults.showAttributeLines());
            animationTicks = builder
                    .comment("锻锤落下到结果揭晓的持续 tick；20 tick 约为一秒。")
                    .defineInRange("animationTicks", defaults.animationTicks(), 1, 20 * 60);
            creativePlayersPay = builder
                    .comment("创造模式玩家是否仍需支付媒介和经验。")
                    .define("creativePlayersPay", defaults.creativePlayersPay());
            missingDefinitionPolicy = builder
                    .comment("词条定义消失时 REMOVE 删除数据，KEEP_INACTIVE 保留但停用。")
                    .defineEnum("missingDefinitionPolicy", defaults.missingDefinitionPolicy());
            builder.pop();
        }
    }
}
