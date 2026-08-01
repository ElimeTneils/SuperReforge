package com.mutuo.superreforge.definition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.Locale;

/** 每条 Attribute effect 可独立选择的装备位置。 */
public enum SlotTarget {
    MAINHAND("mainhand"),
    OFFHAND("offhand"),
    HEAD("head"),
    CHEST("chest"),
    LEGS("legs"),
    FEET("feet"),
    CURIOS_ANY("curios:any");

    public static final Codec<SlotTarget> CODEC = Codec.STRING.comapFlatMap(
            value -> {
                for (SlotTarget target : values()) {
                    if (target.serializedName.equals(value.toLowerCase(Locale.ROOT))) {
                        return DataResult.success(target);
                    }
                }
                return DataResult.error(() -> "未知 Attribute slot: " + value);
            },
            SlotTarget::serializedName);

    private final String serializedName;

    SlotTarget(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
