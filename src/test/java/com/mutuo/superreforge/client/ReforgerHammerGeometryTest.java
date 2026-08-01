package com.mutuo.superreforge.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

/** 验证动力锻锤在四个方向都位于中央砧面上方，最低点只接触而不穿入。 */
final class ReforgerHammerGeometryTest {
    @Test
    void contactPointTouchesTheAnvilWithoutEnteringIt() {
        float headBottom = ReforgerHammerGeometry.headBottom(
                ReforgerHammerGeometry.CONTACT_ORIGIN_Y);

        assertTrue(headBottom >= ReforgerHammerGeometry.ANVIL_TOP);
        assertTrue(headBottom - ReforgerHammerGeometry.ANVIL_TOP < 1.0F / 16.0F);
        assertTrue(ReforgerHammerGeometry.headBottom(ReforgerHammerGeometry.REST_ORIGIN_Y) > headBottom);
    }

    @Test
    void scaledHammerHeadFitsInsideTheCentralAnvilFootprint() {
        assertTrue(ReforgerHammerGeometry.headMinX() >= 4.0F / 16.0F);
        assertTrue(ReforgerHammerGeometry.headMaxX() <= 12.0F / 16.0F);
        assertTrue(ReforgerHammerGeometry.headMinZ() >= 5.0F / 16.0F);
        assertTrue(ReforgerHammerGeometry.headMaxZ() <= 11.0F / 16.0F);
    }

    @Test
    void blockFacingChangesOnlyHorizontalYaw() {
        assertEquals(0.0F, ReforgerHammerGeometry.yawDegrees(Direction.NORTH));
        assertEquals(90.0F, ReforgerHammerGeometry.yawDegrees(Direction.EAST));
        assertEquals(180.0F, ReforgerHammerGeometry.yawDegrees(Direction.SOUTH));
        assertEquals(270.0F, ReforgerHammerGeometry.yawDegrees(Direction.WEST));
    }
}
