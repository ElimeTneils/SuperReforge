package com.mutuo.superreforge.definition;

import com.mutuo.superreforge.SuperReforge;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

/** 持有当前有效定义，并保证 datapack/KubeJS 切换对所有读者是原子的。 */
public final class DefinitionManager {
    private static final AtomicReference<DefinitionSnapshot> DATAPACK =
            new AtomicReference<>(DefinitionSnapshot.EMPTY);
    private static final AtomicReference<DefinitionLayer> SCRIPT = new AtomicReference<>(DefinitionLayer.EMPTY);
    private static final AtomicReference<DefinitionSnapshot> ACTIVE =
            new AtomicReference<>(DefinitionSnapshot.EMPTY);
    private static final AtomicReference<Map<ResourceLocation, ModifierDisplayDefinition>> CLIENT_MODIFIERS =
            new AtomicReference<>(Map.of());
    private static final AtomicLong GENERATION = new AtomicLong();

    private DefinitionManager() {}

    /** 在服务端资源 reload 事件上安装唯一监听器。 */
    public static void register() {
        NeoForge.EVENT_BUS.addListener(DefinitionManager::addReloadListener);
    }

    private static void addReloadListener(AddReloadListenerEvent event) {
        event.addListener(new DefinitionReloadListener());
    }

    public static DefinitionSnapshot snapshot() {
        return ACTIVE.get();
    }

    /** 每次成功发布完整服务端显示状态都会递增，供分块同步和 ACK 防陈旧。 */
    public static long generation() {
        return GENERATION.get();
    }

    /** 远程客户端只读显示镜像；不含服务端 selector、概率或 KubeJS 谓词。 */
    public static Map<ResourceLocation, ModifierDisplayDefinition> clientModifiers() {
        return CLIENT_MODIFIERS.get();
    }

    public static void installClientModifiers(Map<ResourceLocation, ModifierDisplayDefinition> modifiers) {
        CLIENT_MODIFIERS.set(Map.copyOf(modifiers));
    }

    /** 客户端退出世界时清除镜像和可能来自上一单人世界的同进程服务端快照。 */
    public static synchronized void clearClientSession() {
        DATAPACK.set(DefinitionSnapshot.EMPTY);
        SCRIPT.set(DefinitionLayer.EMPTY);
        ACTIVE.set(DefinitionSnapshot.EMPTY);
        CLIENT_MODIFIERS.set(Map.of());
    }

    /**
     * 尝试发布新的 datapack 层。
     *
     * <p>候选与当前脚本层合并后才校验；失败时三个引用均保持不变。
     */
    public static synchronized boolean publishDatapack(DefinitionSnapshot candidate) {
        DefinitionSnapshot merged = merge(candidate, SCRIPT.get());
        ValidationReport report = DefinitionValidator.validate(merged);
        if (!report.isValid()) {
            report.errors().forEach(error -> SuperReforge.LOGGER.error("拒绝定义 reload: {}", error));
            return false;
        }
        DATAPACK.set(candidate);
        ACTIVE.set(merged);
        GENERATION.incrementAndGet();
        return true;
    }

    /** KubeJS 重新运行后用完整新脚本层替换旧层；脚本同 ID 永远覆盖 datapack。 */
    public static synchronized boolean replaceScriptLayer(DefinitionLayer candidate) {
        DefinitionSnapshot merged = merge(DATAPACK.get(), candidate);
        ValidationReport report = DefinitionValidator.validate(merged);
        if (!report.isValid()) {
            report.errors().forEach(error -> SuperReforge.LOGGER.error("拒绝 KubeJS 定义层: {}", error));
            return false;
        }
        SCRIPT.set(candidate);
        ACTIVE.set(merged);
        GENERATION.incrementAndGet();
        return true;
    }

    private static DefinitionSnapshot merge(DefinitionSnapshot datapack, DefinitionLayer script) {
        return new DefinitionSnapshot(
                overlay(datapack.levels(), script.levels()),
                overlay(datapack.itemTypes(), script.itemTypes()),
                overlay(datapack.modifiers(), script.modifiers()),
                overlay(datapack.catalysts(), script.catalysts()));
    }

    private static <T> Map<ResourceLocation, T> overlay(
            Map<ResourceLocation, T> lower, Map<ResourceLocation, T> higher) {
        Map<ResourceLocation, T> merged = new LinkedHashMap<>(lower);
        merged.putAll(higher);
        return Map.copyOf(merged);
    }

    /** 仅供同包测试恢复静态状态，生产逻辑不会调用。 */
    static synchronized void resetForTests() {
        DATAPACK.set(DefinitionSnapshot.EMPTY);
        SCRIPT.set(DefinitionLayer.EMPTY);
        ACTIVE.set(DefinitionSnapshot.EMPTY);
        CLIENT_MODIFIERS.set(Map.of());
        GENERATION.set(0L);
    }
}
