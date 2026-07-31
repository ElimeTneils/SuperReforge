package com.mutuo.superreforge;

import com.mojang.logging.LogUtils;
import com.mutuo.superreforge.config.SuperReforgeConfig;
import com.mutuo.superreforge.definition.DefinitionManager;
import com.mutuo.superreforge.registry.ModDataComponents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Super Reforge 的 NeoForge 模组入口。
 *
 * <p>这个类只负责生命周期编排。注册表、配置、数据加载和兼容层会分别放在独立类中，
 * 避免入口类逐渐变成难以测试的“万能类”。
 */
@Mod(SuperReforge.MOD_ID)
public final class SuperReforge {
    /** 资源位置、网络载荷和 datapack 目录共同使用的稳定命名空间。 */
    public static final String MOD_ID = "superreforge";

    /** 使用 Log4j/SLF4J 统一记录加载、校验和兼容层状态。 */
    public static final Logger LOGGER = LogUtils.getLogger();

    /**
     * NeoForge 创建模组实例时调用。
     *
     * <p>当前引导阶段没有业务注册；后续任务会把各模块注册器接入这里。
     */
    public SuperReforge(IEventBus modBus, ModContainer modContainer) {
        ModDataComponents.register(modBus);
        DefinitionManager.register();
        modContainer.registerConfig(ModConfig.Type.SERVER, SuperReforgeConfig.SERVER_SPEC);
        LOGGER.info("Super Reforge bootstrap initialized");
    }
}
