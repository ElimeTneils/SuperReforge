package com.mutuo.superreforge.definition;

import com.mutuo.superreforge.SuperReforge;
import com.mutuo.superreforge.api.ScriptDefinitionPublisher;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/** 持有当前有效定义，并保证 datapack/KubeJS 切换对所有读者是原子的。 */
public final class DefinitionManager {
    private static final AtomicReference<DefinitionSnapshot> DATAPACK =
            new AtomicReference<>(DefinitionSnapshot.EMPTY);
    private static final AtomicReference<DefinitionLayer> SCRIPT = new AtomicReference<>(DefinitionLayer.EMPTY);
    private static final AtomicReference<DefinitionSnapshot> ACTIVE =
            new AtomicReference<>(DefinitionSnapshot.EMPTY);
    private static final AtomicReference<Map<ResourceLocation, ModifierDisplayDefinition>> CLIENT_MODIFIERS =
            new AtomicReference<>(Map.of());
    private static final AtomicBoolean DATAPACK_LOADED = new AtomicBoolean();
    private static final AtomicLong GENERATION = new AtomicLong();

    private DefinitionManager() {}

    /** 在服务端资源 reload 事件上安装唯一监听器。 */
    public static void register() {
        NeoForge.EVENT_BUS.addListener(DefinitionManager::addReloadListener);
        NeoForge.EVENT_BUS.addListener(DefinitionManager::onServerStopped);
    }

    private static void addReloadListener(AddReloadListenerEvent event) {
        event.addListener(new DefinitionReloadListener());
    }

    /** 服务器彻底停止后再清服务端层，避免定义跨单人世界或专用服务器会话残留。 */
    private static void onServerStopped(ServerStoppedEvent event) {
        clearServerSession();
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

    /**
     * 客户端退出世界时只清除服务端下发的显示镜像。
     *
     * <p>单人游戏的客户端和内置服务端共用同一个 JVM；这里若同时清除服务端定义，下一次进入世界时就会在
     * datapack reload 完成之前短暂得到空快照，导致物品前缀和属性必须依靠手动 {@code /reload} 才恢复。
     */
    public static void clearClientSession() {
        CLIENT_MODIFIERS.set(Map.of());
    }

    /**
     * 清除已经停止的服务端会话；它与客户端断线清理分开，防止内置服务端仍在工作时被误删定义。
     */
    public static void clearServerSession() {
        // 先在管理器自身锁内完成快照切换，再清脚本伴随状态，避免两个管理器以相反顺序互锁。
        synchronized (DefinitionManager.class) {
            DATAPACK.set(DefinitionSnapshot.EMPTY);
            SCRIPT.set(DefinitionLayer.EMPTY);
            ACTIVE.set(DefinitionSnapshot.EMPTY);
            DATAPACK_LOADED.set(false);
        }
        ScriptDefinitionPublisher.clearServerSession();
    }

    /** 区分“datapack 尚未运行”和“已经成功加载但内容恰好为空”，供 KubeJS 判断是否应延迟重试。 */
    public static boolean hasLoadedDatapack() {
        return DATAPACK_LOADED.get();
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
        DATAPACK_LOADED.set(true);
        ACTIVE.set(merged);
        GENERATION.incrementAndGet();
        return true;
    }

    /** KubeJS 重新运行后用完整新脚本层替换旧层；脚本同 ID 永远覆盖 datapack。 */
    public static synchronized boolean replaceScriptLayer(DefinitionLayer candidate) {
        return replaceScriptLayer(candidate, true);
    }

    /**
     * 首次开服的预校验入口：失败时不写错误日志，由发布器判断这是“等待 datapack”还是确实无效。
     */
    public static synchronized boolean tryReplaceScriptLayer(DefinitionLayer candidate) {
        return replaceScriptLayer(candidate, false);
    }

    private static boolean replaceScriptLayer(DefinitionLayer candidate, boolean reportErrors) {
        DefinitionSnapshot merged = merge(DATAPACK.get(), candidate);
        ValidationReport report = DefinitionValidator.validate(merged);
        if (!report.isValid()) {
            if (reportErrors) {
                report.errors().forEach(error -> SuperReforge.LOGGER.error("拒绝 KubeJS 定义层: {}", error));
            }
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
        DATAPACK_LOADED.set(false);
        GENERATION.set(0L);
    }
}
