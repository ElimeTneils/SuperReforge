package com.mutuo.superreforge.compat.kubejs;

import com.mutuo.superreforge.SuperReforge;
import com.mutuo.superreforge.api.ScriptDefinitionCollector;
import com.mutuo.superreforge.api.ScriptDefinitionPublisher;
import com.mutuo.superreforge.network.ModNetwork;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingRegistry;
import dev.latvian.mods.kubejs.script.ScriptManager;
import dev.latvian.mods.kubejs.script.ScriptType;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * KubeJS 2101 插件入口。
 *
 * <p>它只会由带 `kubejs` 条件的 kubejs.plugins.txt 加载；KubeJS 未安装时 JVM 永远不会解析本类。
 */
public final class SuperReforgeKubeJSPlugin implements KubeJSPlugin {
    private static final KubeReforgeBindings BINDINGS = new KubeReforgeBindings();
    private ScriptDefinitionCollector collector;

    @Override
    public void registerBindings(BindingRegistry bindings) {
        if (bindings.type() == ScriptType.SERVER) {
            bindings.add("SuperReforge", BINDINGS);
        }
    }

    /** 每次 server_scripts reload 使用全新临时容器，已删除脚本不会残留旧定义。 */
    @Override
    public void beforeScriptsLoaded(ScriptManager manager) {
        if (manager.scriptType == ScriptType.SERVER) {
            // 新脚本加载会完全替代旧脚本，因此旧的待重试候选也必须同时作废。
            ScriptDefinitionPublisher.discardPending();
            collector = new ScriptDefinitionCollector();
            BINDINGS.begin(collector);
        }
    }

    /**
     * 所有脚本成功后才一次性发布；脚本错误或定义交叉校验失败都会保留上一份有效快照。
     */
    @Override
    public void afterScriptsLoaded(ScriptManager manager) {
        if (manager.scriptType != ScriptType.SERVER) {
            return;
        }
        try {
            if (!manager.scriptType.console.errors.isEmpty()) {
                SuperReforge.LOGGER.error(
                        "KubeJS server_scripts contains {} error(s); keeping the previous Super Reforge layer",
                        manager.scriptType.console.errors.size());
                return;
            }
            var bundle = collector.build();
            ScriptDefinitionPublisher.Result result = ScriptDefinitionPublisher.publish(bundle);
            if (result == ScriptDefinitionPublisher.Result.DEFERRED) {
                // 首次开服时 datapack 基础等级/类型可能尚未发布；reload listener 会在稍后自动重试。
                SuperReforge.LOGGER.info(
                        "KubeJS Super Reforge layer is waiting for datapack definitions; it will be retried automatically");
                return;
            }
            if (result == ScriptDefinitionPublisher.Result.REJECTED) {
                SuperReforge.LOGGER.error("KubeJS Super Reforge definitions failed validation; previous layer kept");
                return;
            }
            // server_scripts 可独立 reload；完整发布后必须主动重发新代次，不能等待下一次 datapack sync。
            var server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                server.execute(() -> ModNetwork.synchronizeAll(server));
            }
            SuperReforge.LOGGER.info(
                    "Published KubeJS layer: {} level(s), {} type(s), {} modifier(s), {} catalyst(s), {} predicate(s), {} stage(s)",
                    bundle.definitions().levels().size(),
                    bundle.definitions().itemTypes().size(),
                    bundle.definitions().modifiers().size(),
                    bundle.definitions().catalysts().size(),
                    bundle.predicates().size(),
                    bundle.stages().size());
        } catch (RuntimeException error) {
            SuperReforge.LOGGER.error("Failed to publish KubeJS Super Reforge layer; previous layer kept", error);
        } finally {
            collector = null;
            BINDINGS.end();
        }
    }
}
