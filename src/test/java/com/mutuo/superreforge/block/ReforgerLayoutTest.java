package com.mutuo.superreforge.block;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** 验证屏幕和菜单共享的布局能保持槽位居中，并正确隔离日志滚动区域。 */
final class ReforgerLayoutTest {
    @Test
    void centersTheStandardPlayerInventoryInsideTheWiderScreen() {
        int leftMargin = ReforgerLayout.playerPanelLeft();

        assertEquals((ReforgerLayout.GUI_WIDTH - ReforgerLayout.PLAYER_PANEL_WIDTH) / 2, leftMargin);
        assertEquals(leftMargin + 8, ReforgerLayout.playerSlotX(0));
        assertEquals(leftMargin + 8 + 8 * 18, ReforgerLayout.playerSlotX(8));
        assertTrue(ReforgerLayout.playerSlotX(8) + 16 < ReforgerLayout.GUI_WIDTH);
    }

    @Test
    void vanillaInventoryHasSymmetricMarginsAndExactlyNineColumns() {
        int panelLeft = ReforgerLayout.playerPanelLeft();

        assertEquals(176, ReforgerLayout.PLAYER_PANEL_WIDTH);
        assertEquals(panelLeft + 8, ReforgerLayout.playerSlotX(0));
        assertEquals(panelLeft + 8 + 8 * 18, ReforgerLayout.playerSlotX(8));
        assertEquals(8, panelLeft + ReforgerLayout.PLAYER_PANEL_WIDTH - (ReforgerLayout.playerSlotX(8) + 16));
    }

    @Test
    void exposesExactlyThirtySixUniquePlayerSlotPositionsAcrossNineColumns() {
        Set<String> positions = new HashSet<>();
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                positions.add(ReforgerLayout.playerSlotX(column) + ","
                        + (ReforgerLayout.PLAYER_INVENTORY_Y + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            positions.add(ReforgerLayout.playerSlotX(column) + "," + ReforgerLayout.PLAYER_HOTBAR_Y);
        }

        assertEquals(36, positions.size());
        assertEquals(9, positions.stream()
                .map(position -> position.substring(0, position.indexOf(',')))
                .distinct()
                .count());
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
