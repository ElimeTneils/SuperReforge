package com.mutuo.superreforge.api;

import com.mutuo.superreforge.definition.DefinitionManager;
import com.mutuo.superreforge.progress.ProgressService;
import com.mutuo.superreforge.reforge.SelectorHooks;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 原子发布 KubeJS 定义，并处理首次开服时脚本早于 datapack 基础定义执行的时序。
 *
 * <p>KubeJS 的 {@code server_scripts} 可能先引用 datapack 中声明的等级或物品类型。此时第一次校验失败并不代表
 * 脚本有错，所以先保存完整 bundle；datapack 发布完成后再自动重试。定义层、物品谓词和进度阶段只会一起生效，
 * 不会留下“词条已更新但选择器仍是旧版”的半发布状态。
 */
public final class ScriptDefinitionPublisher {
    /** 调用方据此决定立即同步客户端，或等待 datapack reload 完成。 */
    public enum Result {
        PUBLISHED,
        DEFERRED,
        REJECTED
    }

    private static ScriptDefinitionBundle pending;

    private ScriptDefinitionPublisher() {}

    /** 尝试立即发布；若基础定义尚未就绪，则保存为待重试 bundle。 */
    public static synchronized Result publish(ScriptDefinitionBundle bundle) {
        ScriptDefinitionBundle candidate = Objects.requireNonNull(bundle, "bundle");
        if (!DefinitionManager.tryReplaceScriptLayer(candidate.definitions())) {
            // datapack 已经完成后仍不合法，就属于真实配置错误；记录详细校验日志且不覆盖上一层。
            if (DefinitionManager.hasLoadedDatapack()) {
                // datapack 可能刚好在两次检查之间完成；第二次若成功就按正常发布处理。
                if (DefinitionManager.replaceScriptLayer(candidate.definitions())) {
                    installCompanions(candidate);
                    pending = null;
                    return Result.PUBLISHED;
                }
                pending = null;
                return Result.REJECTED;
            }
            // 首次启动尚无基础等级/类型，完整保存并等待 DefinitionReloadListener 自动重试。
            pending = candidate;
            return Result.DEFERRED;
        }
        installCompanions(candidate);
        pending = null;
        return Result.PUBLISHED;
    }

    /** datapack 成功发布后重试最近一次暂缓的 KubeJS bundle。 */
    public static synchronized boolean retryPending() {
        if (pending == null || !DefinitionManager.replaceScriptLayer(pending.definitions())) {
            return false;
        }
        installCompanions(pending);
        pending = null;
        return true;
    }

    /** 新一轮 server_scripts 加载开始时丢弃旧候选，防止已删除脚本稍后复活。 */
    public static synchronized void discardPending() {
        pending = null;
    }

    /** 服务端停止后清掉所有脚本会话状态，确保下一个世界从干净状态开始。 */
    public static synchronized void clearServerSession() {
        pending = null;
        SelectorHooks.replaceScriptPredicates(Map.of());
        ProgressService.replaceStages(List.of());
    }

    /** 只有定义层校验通过后，才提交与它配套的 KubeJS 谓词和全服进度阶段。 */
    private static void installCompanions(ScriptDefinitionBundle bundle) {
        SelectorHooks.replaceScriptPredicates(bundle.predicates());
        ProgressService.replaceStages(bundle.stages().values());
    }
}
