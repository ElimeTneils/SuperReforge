package com.mutuo.superreforge.client;

import net.minecraft.core.Direction;

/**
 * 动力锻锤的模型边界、缩放和世界坐标锚点。
 *
 * <p>模型以方块中心为原点渲染；集中计算这些数值能保证最低锤面只接触 11/16 高的中央砧面。
 */
public final class ReforgerHammerGeometry {
    public static final float SCALE = 0.62F;
    public static final float ANVIL_TOP = 11.0F / 16.0F;
    public static final float CONTACT_ORIGIN_Y = 0.96F;
    public static final float REST_ORIGIN_Y = 1.03F;
    public static final float RAISED_ORIGIN_Y = 1.24F;
    public static final float REBOUND_ORIGIN_Y = 1.06F;

    private static final float MODEL_HEAD_BOTTOM = 1.0F / 16.0F;
    private static final float MODEL_HEAD_MIN_X = 2.0F / 16.0F;
    private static final float MODEL_HEAD_MAX_X = 14.0F / 16.0F;
    private static final float MODEL_HEAD_MIN_Z = 5.0F / 16.0F;
    private static final float MODEL_HEAD_MAX_Z = 11.0F / 16.0F;

    private ReforgerHammerGeometry() {}

    public static float headBottom(float originY) {
        return originY + fromModelCenter(MODEL_HEAD_BOTTOM);
    }

    public static float headMinX() {
        return 0.5F + fromModelCenter(MODEL_HEAD_MIN_X);
    }

    public static float headMaxX() {
        return 0.5F + fromModelCenter(MODEL_HEAD_MAX_X);
    }

    public static float headMinZ() {
        return 0.5F + fromModelCenter(MODEL_HEAD_MIN_Z);
    }

    public static float headMaxZ() {
        return 0.5F + fromModelCenter(MODEL_HEAD_MAX_Z);
    }

    public static float yawDegrees(Direction facing) {
        return switch (facing) {
            case EAST -> 90.0F;
            case SOUTH -> 180.0F;
            case WEST -> 270.0F;
            default -> 0.0F;
        };
    }

    private static float fromModelCenter(float modelCoordinate) {
        return (modelCoordinate - 0.5F) * SCALE;
    }
}
