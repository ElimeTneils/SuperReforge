package com.mutuo.superreforge.reforge;

/** 阶段对一种成本独立应用的“先乘后加”修正。 */
public record CostModifier(double multiplier, int addition) {
    public static final CostModifier IDENTITY = new CostModifier(1.0, 0);

    public CostModifier {
        if (!Double.isFinite(multiplier) || multiplier < 0.0) {
            throw new IllegalArgumentException("成本 multiplier 必须是非负有限数");
        }
    }

    /** 按设计使用 floor(base * multiplier) + addition，并把结果下限钳制为 0。 */
    public int apply(int base) {
        double multiplied = Math.floor(Math.max(0, base) * multiplier);
        if (multiplied >= Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        long result = (long) multiplied + addition;
        return (int) Math.clamp(result, 0L, Integer.MAX_VALUE);
    }
}
