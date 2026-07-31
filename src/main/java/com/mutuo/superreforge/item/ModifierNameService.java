package com.mutuo.superreforge.item;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** 只在渲染时把前缀放在当前基础名之前，因此铁砧重命名不会移除前缀。 */
public final class ModifierNameService {
    private ModifierNameService() {}

    public static Component prefix(Component prefix, Component currentName) {
        MutableComponent result = Component.empty();
        result.append(prefix.copy());
        result.append(Component.literal(" "));
        result.append(currentName.copy());
        return result;
    }
}
