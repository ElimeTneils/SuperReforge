package com.mutuo.superreforge.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** 验证约一秒动画的五个关键帧不会因渲染帧率改变。 */
final class ReforgerRenderStateTest {
    @Test
    void hammerRaisesImpactsBouncesAndReturnsToRestAroundTheFixedPivot() {
        ReforgerRenderState start = ReforgerRenderState.fromTicks(20, 20, 0);
        ReforgerRenderState raised = ReforgerRenderState.fromTicks(20, 15, 0);
        ReforgerRenderState impact = ReforgerRenderState.fromTicks(20, 10, 0);
        ReforgerRenderState rebound = ReforgerRenderState.fromTicks(20, 7, 0);
        ReforgerRenderState end = ReforgerRenderState.fromTicks(20, 0, 0);

        assertEquals(-45.0F, start.hammerAngleDegrees(), 0.001F);
        assertEquals(-75.0F, raised.hammerAngleDegrees(), 0.001F);
        assertEquals(ReforgerHammerGeometry.CONTACT_ANGLE_DEGREES, impact.hammerAngleDegrees(), 0.001F);
        assertEquals(-47.0F, rebound.hammerAngleDegrees(), 0.001F);
        assertEquals(-45.0F, end.hammerAngleDegrees(), 0.001F);
        assertEquals(1.0F, impact.coreIntensity(), 0.001F);
        assertEquals(1.0F, end.progress(), 0.001F);
    }

    @Test
    void everySampledAngleKeepsTheHammerFaceAboveTheAnvil() {
        for (int remaining = 20; remaining >= 0; remaining--) {
            ReforgerRenderState state = ReforgerRenderState.fromTicks(20, remaining, 0);
            assertTrue(ReforgerHammerGeometry.hammerFaceY(state.hammerAngleDegrees())
                    >= ReforgerHammerGeometry.ANVIL_TOP);
        }
    }
}
