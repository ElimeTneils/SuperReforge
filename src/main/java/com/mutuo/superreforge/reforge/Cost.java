package com.mutuo.superreforge.reforge;

/** 服务端给一次重铸计算出的最终材料数量和经验数量。 */
public record Cost(int materialCount, int experience) {
    public Cost {
        if (materialCount < 0 || experience < 0) {
            throw new IllegalArgumentException("最终成本不能为负数");
        }
    }
}
