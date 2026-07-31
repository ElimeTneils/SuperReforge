package com.mutuo.superreforge.api;

import com.mutuo.superreforge.definition.DefinitionLayer;
import com.mutuo.superreforge.definition.DefinitionManager;
import com.mutuo.superreforge.definition.DefinitionSnapshot;
import java.util.Collection;

/** 面向可选脚本层和其他模组的稳定 Java API 门面。 */
public final class SuperReforgeApi {
    private SuperReforgeApi() {}

    public static DefinitionSnapshot definitions() {
        return DefinitionManager.snapshot();
    }

    /** 汇总完整脚本执行结果并一次替换，避免半成品定义暴露给游戏线程。 */
    public static boolean replaceScriptDefinitions(Collection<DefinitionContribution> contributions) {
        DefinitionLayer.Builder builder = DefinitionLayer.builder();
        contributions.forEach(contribution -> contribution.contribute(builder));
        return DefinitionManager.replaceScriptLayer(builder.build());
    }
}
