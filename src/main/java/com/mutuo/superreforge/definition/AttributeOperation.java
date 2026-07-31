package com.mutuo.superreforge.definition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.Locale;

/** 与 Minecraft AttributeModifier 三种运算方式一一对应的稳定 JSON 名称。 */
public enum AttributeOperation {
    ADD_VALUE("add_value"),
    ADD_MULTIPLIED_BASE("add_multiplied_base"),
    ADD_MULTIPLIED_TOTAL("add_multiplied_total");

    public static final Codec<AttributeOperation> CODEC = Codec.STRING.comapFlatMap(
            value -> {
                for (AttributeOperation operation : values()) {
                    if (operation.serializedName.equals(value.toLowerCase(Locale.ROOT))) {
                        return DataResult.success(operation);
                    }
                }
                return DataResult.error(() -> "未知 Attribute operation: " + value);
            },
            AttributeOperation::serializedName);

    private final String serializedName;

    AttributeOperation(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
