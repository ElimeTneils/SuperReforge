package com.mutuo.superreforge.block;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** 验证屏幕和菜单共享的布局能保持槽位居中，并正确隔离日志滚动区域。 */
final class ReforgerLayoutTest {
    @Test
    void centersTheStandardPlayerInventoryInsideTheWiderScreen() {
        int inventoryPanelWidth = 176;
        int leftMargin = ReforgerLayout.playerInventoryLeft();

        assertEquals((ReforgerLayout.GUI_WIDTH - inventoryPanelWidth) / 2, leftMargin);
        assertEquals(leftMargin, ReforgerLayout.playerSlotX(0));
        assertEquals(leftMargin + 8 * 18, ReforgerLayout.playerSlotX(8));
        assertTrue(ReforgerLayout.playerSlotX(8) + 16 < ReforgerLayout.GUI_WIDTH);
    }

    @Test
    void placesBothMachineSlotsInsideTheOperationPanel() {
        assertTrue(ReforgerLayout.TARGET_X >= ReforgerLayout.OPERATION_LEFT);
        assertTrue(ReforgerLayout.TARGET_X + 18 <= ReforgerLayout.OPERATION_RIGHT);
        assertTrue(ReforgerLayout.CATALYST_X >= ReforgerLayout.OPERATION_LEFT);
        assertTrue(ReforgerLayout.CATALYST_X + 18 <= ReforgerLayout.OPERATION_RIGHT);
        assertTrue(ReforgerLayout.TARGET_Y < ReforgerLayout.CATALYST_Y);
    }

    @Test
    void logHitTestingConsumesOnlyTheClippedLogAndScrollbar() {
        assertTrue(ReforgerLayout.insideLog(154, 36));
        assertTrue(ReforgerLayout.insideLog(269.9, 111.9));
        assertFalse(ReforgerLayout.insideLog(270, 112));
        assertTrue(ReforgerLayout.insideScrollbar(273, 36));
        assertFalse(ReforgerLayout.insideScrollbar(272.9, 36));
        assertFalse(ReforgerLayout.insideLog(154, ReforgerLayout.PLAYER_INVENTORY_Y));
    }

    @Test
    void previewLimitWarningHasItsOwnHoverTarget() {
        assertTrue(ReforgerLayout.insidePreviewWarning(261, 22));
        assertTrue(ReforgerLayout.insidePreviewWarning(269.9, 31.9));
        assertFalse(ReforgerLayout.insidePreviewWarning(260.9, 22));
        assertFalse(ReforgerLayout.insidePreviewWarning(270, 32));
    }
}
