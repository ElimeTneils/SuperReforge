package com.mutuo.superreforge.api;

import com.mutuo.superreforge.definition.DefinitionLayer;

/** KubeJS 或其他兼容层向一次临时 builder 写入定义的最小公共接口。 */
@FunctionalInterface
public interface DefinitionContribution {
    void contribute(DefinitionLayer.Builder builder);
}
