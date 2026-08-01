package com.mutuo.superreforge.reforge;

import net.minecraft.world.item.ItemStack;

/** 纯函数生成的结果和媒介余量；调用者通过一次提交把它们写入方块实体。 */
public record PreparedReforge(ItemStack result, ItemStack catalystRemainder) {}
