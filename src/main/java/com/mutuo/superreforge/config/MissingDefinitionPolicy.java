package com.mutuo.superreforge.config;

/** reload 后物品保存的词条 ID 已不存在时采用的全局策略。 */
public enum MissingDefinitionPolicy {
    /** 从物品移除失效数据，避免长期积累无意义组件。 */
    REMOVE,
    /** 保留 ID 和种子但停止效果，定义恢复后可重新激活。 */
    KEEP_INACTIVE
}
