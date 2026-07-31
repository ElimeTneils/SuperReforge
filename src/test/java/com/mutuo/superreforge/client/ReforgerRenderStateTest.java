package com.mutuo.superreforge.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** 验证约一秒动画的关键时间点不会因渲染帧率改变。 */
final class ReforgerRenderStateTest {
    @Test
    void hammerFallsAndCorePeaksAtImpact() {
        ReforgerRenderState start = ReforgerRenderState.fromTicks(20, 20, 0);
        ReforgerRenderState impact = ReforgerRenderState.fromTicks(20, 10, 0);
        ReforgerRenderState end = ReforgerRenderState.fromTicks(20, 0, 0);

        assertTrue(start.hammerHeight() > impact.hammerHeight());
        assertEquals(1.0F, impact.coreIntensity(), 0.001F);
        assertEquals(1.0F, end.progress(), 0.001F);
    }
}
