package com.mutuo.superreforge.block;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
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
    void sharesTheExactOrderedThirtySixSlotPositionsWithTheMenu() {
        List<ReforgerLayout.PlayerSlotPosition> positions = ReforgerLayout.playerSlots();

        assertEquals(36, positions.size());
        assertSame(positions, ReforgerMenu.playerSlots());
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                int offset = row * 9 + column;
                ReforgerLayout.PlayerSlotPosition position = positions.get(offset);
                assertEquals(9 + offset, position.inventoryIndex());
                assertEquals(ReforgerLayout.playerSlotX(column), position.x());
                assertEquals(ReforgerLayout.PLAYER_INVENTORY_Y + row * 18, position.y());
            }
        }
        for (int column = 0; column < 9; column++) {
            ReforgerLayout.PlayerSlotPosition position = positions.get(27 + column);
            assertEquals(column, position.inventoryIndex());
            assertEquals(ReforgerLayout.playerSlotX(column), position.x());
            assertEquals(ReforgerLayout.PLAYER_HOTBAR_Y, position.y());
        }
        assertEquals(36, positions.stream().map(position -> position.x() + "," + position.y()).distinct().count());
    }

    @Test
    void framesTheSixteenPixelContentWithSymmetricSevenPixelVisualMargins() {
        ReforgerLayout.PlayerSlotPosition first = ReforgerLayout.playerSlots().getFirst();
        ReforgerLayout.PlayerSlotPosition last = ReforgerLayout.playerSlots().get(8);
        int panelLeft = ReforgerLayout.playerPanelLeft();

        assertEquals(panelLeft + 8, first.x());
        assertEquals(panelLeft + 7, first.frameX());
        assertEquals(8, panelLeft + ReforgerLayout.PLAYER_PANEL_WIDTH - (last.x() + 16));
        assertEquals(7, panelLeft + ReforgerLayout.PLAYER_PANEL_WIDTH - (last.frameX() + 18));
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
