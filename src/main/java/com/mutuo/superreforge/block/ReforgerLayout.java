package com.mutuo.superreforge.block;

import java.util.ArrayList;
import java.util.List;

/**
 * 重铸菜单与客户端屏幕共享的相对坐标。
 *
 * <p>把槽位和日志命中区域放在公共类中，可避免服务器菜单坐标与客户端绘制坐标各自修改后发生错位。
 */
public final class ReforgerLayout {
    public static final int GUI_WIDTH = 286;
    public static final int GUI_HEIGHT = 218;

    public static final int OPERATION_LEFT = 6;
    public static final int OPERATION_RIGHT = 148;
    public static final int TARGET_X = 35;
    public static final int TARGET_Y = 45;
    public static final int CATALYST_X = 35;
    public static final int CATALYST_Y = 81;

    public static final int LOG_LEFT = 154;
    public static final int LOG_TOP = 36;
    public static final int LOG_WIDTH = 116;
    public static final int LOG_HEIGHT = 76;
    public static final int SCROLLBAR_X = 273;
    public static final int SCROLLBAR_WIDTH = 5;
    public static final int PREVIEW_WARNING_X = 261;
    public static final int PREVIEW_WARNING_Y = 22;
    public static final int PREVIEW_WARNING_SIZE = 9;

    public static final int PLAYER_INVENTORY_Y = 136;
    public static final int PLAYER_HOTBAR_Y = 194;
    /** 原版背包面板固定为 176px：9 个槽位配合两侧对称 8px 内边距。 */
    public static final int PLAYER_PANEL_WIDTH = 176;
    private static final int PLAYER_PANEL_INSET = 8;
    private static final List<PlayerSlotPosition> PLAYER_SLOTS = createPlayerSlots();

    private ReforgerLayout() {}

    public static int playerPanelLeft() {
        return (GUI_WIDTH - PLAYER_PANEL_WIDTH) / 2;
    }

    /** 菜单真实 Slot 与客户端空槽底图共同使用这条横坐标公式，避免出现第十列。 */
    public static int playerSlotX(int column) {
        return playerPanelLeft() + PLAYER_PANEL_INSET + column * 18;
    }

    /**
     * 菜单 Slot 与屏幕空槽底图唯一共享的玩家背包几何；顺序与原版 Inventory 索引一致。
     */
    public static List<PlayerSlotPosition> playerSlots() {
        return PLAYER_SLOTS;
    }

    private static List<PlayerSlotPosition> createPlayerSlots() {
        List<PlayerSlotPosition> positions = new ArrayList<>(36);
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                positions.add(new PlayerSlotPosition(
                        9 + row * 9 + column,
                        playerSlotX(column),
                        PLAYER_INVENTORY_Y + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            positions.add(new PlayerSlotPosition(column, playerSlotX(column), PLAYER_HOTBAR_Y));
        }
        return List.copyOf(positions);
    }

    /** 16px 物品内容区坐标与环绕它的 18px 原版槽框坐标。 */
    public record PlayerSlotPosition(int inventoryIndex, int x, int y) {
        public int frameX() {
            return x - 1;
        }

        public int frameY() {
            return y - 1;
        }
    }

    public static boolean insideLog(double x, double y) {
        return x >= LOG_LEFT && x < LOG_LEFT + LOG_WIDTH && y >= LOG_TOP && y < LOG_TOP + LOG_HEIGHT;
    }

    public static boolean insideScrollbar(double x, double y) {
        return x >= SCROLLBAR_X
                && x < SCROLLBAR_X + SCROLLBAR_WIDTH
                && y >= LOG_TOP
                && y < LOG_TOP + LOG_HEIGHT;
    }

    public static boolean insidePreviewWarning(double x, double y) {
        return x >= PREVIEW_WARNING_X
                && x < PREVIEW_WARNING_X + PREVIEW_WARNING_SIZE
                && y >= PREVIEW_WARNING_Y
                && y < PREVIEW_WARNING_Y + PREVIEW_WARNING_SIZE + 1;
    }
}
