package com.mutuo.superreforge.block;

import net.minecraft.world.item.ItemStack;

/** 方块实体持久化的待揭晓结果和动画进度。 */
public record PendingReforge(ItemStack result, int totalTicks, int remainingTicks) {
    public PendingReforge {
        result = result.copy();
        if (totalTicks < 1 || remainingTicks < 0 || remainingTicks > totalTicks) {
            throw new IllegalArgumentException("pending 动画 tick 范围无效");
        }
    }

    public PendingReforge tick() {
        return remainingTicks == 0
                ? this
                : new PendingReforge(result, totalTicks, remainingTicks - 1);
    }

    public boolean ready() {
        return remainingTicks == 0;
    }
}
