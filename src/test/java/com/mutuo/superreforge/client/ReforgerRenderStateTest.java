package com.mutuo.superreforge.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** 验证约一秒动画的关键时间点不会因渲染帧率改变。 */
final class ReforgerRenderStateTest {
    @Test
    void hammerRaisesImpactsBouncesAndReturnsToRest() {
        ReforgerRenderState start = ReforgerRenderState.fromTicks(20, 20, 0);
        ReforgerRenderState raised = ReforgerRenderState.fromTicks(20, 15, 0);
        ReforgerRenderState impact = ReforgerRenderState.fromTicks(20, 10, 0);
        ReforgerRenderState rebound = ReforgerRenderState.fromTicks(20, 7, 0);
        ReforgerRenderState end = ReforgerRenderState.fromTicks(20, 0, 0);

        assertEquals(ReforgerHammerGeometry.REST_ORIGIN_Y, start.hammerHeight(), 0.001F);
        assertEquals(ReforgerHammerGeometry.RAISED_ORIGIN_Y, raised.hammerHeight(), 0.001F);
        assertEquals(ReforgerHammerGeometry.CONTACT_ORIGIN_Y, impact.hammerHeight(), 0.001F);
        assertTrue(rebound.hammerHeight() > impact.hammerHeight());
        assertEquals(ReforgerHammerGeometry.REST_ORIGIN_Y, end.hammerHeight(), 0.001F);
        assertEquals(1.0F, impact.coreIntensity(), 0.001F);
        assertEquals(1.0F, end.progress(), 0.001F);
    }

    @Test
    void everySampledHeightStaysAboveTheContactPoint() {
        for (int remaining = 20; remaining >= 0; remaining--) {
            ReforgerRenderState state = ReforgerRenderState.fromTicks(20, remaining, 0);
            assertTrue(state.hammerHeight() >= ReforgerHammerGeometry.CONTACT_ORIGIN_Y);
        }
    }
}
