package com.mutuo.superreforge.block;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

/** 验证动画状态由服务端 tick 驱动并在精确时刻揭晓。 */
final class PendingReforgeTest {
    @Test
    void becomesReadyAfterExactlyConfiguredTicks() {
        PendingReforge pending = new PendingReforge(new ItemStack(Items.DIAMOND_SWORD), 2, 2);

        PendingReforge afterOne = pending.tick();
        PendingReforge afterTwo = afterOne.tick();

        assertFalse(pending.ready());
        assertFalse(afterOne.ready());
        assertTrue(afterTwo.ready());
        assertEquals(0, afterTwo.remainingTicks());
    }
}
