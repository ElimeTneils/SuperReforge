package com.mutuo.superreforge.reforge;

/** 服务端报价或执行失败的稳定原因，GUI 可映射为翻译键。 */
public enum ReforgeFailure {
    TARGET_EMPTY,
    TARGET_COUNT,
    CATALYST_MISSING,
    MATERIAL_COUNT,
    NO_ITEM_TYPE,
    NO_CANDIDATES,
    EXPERIENCE,
    DEFINITION_SYNC,
    BUSY,
    STALE_STATE
}
