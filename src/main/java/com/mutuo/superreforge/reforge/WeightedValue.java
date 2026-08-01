package com.mutuo.superreforge.reforge;

/** 一个相对权重条目；归一化后 probability 保存真实展示概率。 */
public record WeightedValue<T>(T value, double weight, double probability) {
    public WeightedValue(T value, double weight) {
        this(value, weight, Double.NaN);
    }
}
