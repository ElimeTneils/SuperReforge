package com.mutuo.superreforge.client;

import net.minecraft.core.Direction;

/** Shared model bounds and fixed-pivot geometry for the animated hammer. */
public final class ReforgerHammerGeometry {
    public static final float SCALE = 0.62F;
    public static final float ANVIL_TOP = 11.0F / 16.0F;
    public static final float PIVOT_X = 0.5F;
    public static final float PIVOT_Y = 1.355F;
    public static final float PIVOT_Z = 0.5F;
    public static final float PIVOT_MODEL_Y = 1.0F;
    public static final float REST_ANGLE_DEGREES = -45.0F;
    public static final float RAISED_ANGLE_DEGREES = 0.0F;
    public static final float CONTACT_ANGLE_DEGREES = -45.0F;
    public static final float REBOUND_ANGLE_DEGREES = -30.0F;

    public static final Box HANDLE_BOUNDS = new Box(7, 7, 7, 9, 16, 9);
    public static final Box SOCKET_BOUNDS = new Box(6, 5, 6, 10, 7, 10);
    public static final Box HEAD_BOUNDS = new Box(3, 1, 5, 14, 5, 11);
    public static final Box CAP_BOUNDS = new Box(2, 1, 5, 3, 5, 11);

    private ReforgerHammerGeometry() {}

    /** Lowest point of the hammer face after local Z rotation around the handle pivot. */
    public static float hammerFaceY(float angleDegrees) {
        float radians = (float) Math.toRadians(angleDegrees);
        float sin = (float) Math.sin(radians);
        float cos = (float) Math.cos(radians);
        float lowest = Float.POSITIVE_INFINITY;
        for (float x : new float[] {HEAD_BOUNDS.minX(), HEAD_BOUNDS.maxX()}) {
            for (float y : new float[] {HEAD_BOUNDS.minY(), HEAD_BOUNDS.maxY()}) {
                float centeredX = x / 16.0F - 0.5F;
                float centeredY = y / 16.0F - 0.5F;
                lowest = Math.min(lowest, centeredX * sin + centeredY * cos);
            }
        }
        return PIVOT_Y + (lowest - (PIVOT_MODEL_Y - 0.5F)) * SCALE;
    }

    public static boolean boxesDoNotOverlap(Box first, Box second) {
        return first.maxX() <= second.minX() || second.maxX() <= first.minX()
                || first.maxY() <= second.minY() || second.maxY() <= first.minY()
                || first.maxZ() <= second.minZ() || second.maxZ() <= first.minZ();
    }

    public static float headMinX() {
        return 0.5F + fromModelCenter(HEAD_BOUNDS.minX() / 16.0F);
    }

    public static float headMaxX() {
        return 0.5F + fromModelCenter(HEAD_BOUNDS.maxX() / 16.0F);
    }

    public static float headMinZ() {
        return 0.5F + fromModelCenter(HEAD_BOUNDS.minZ() / 16.0F);
    }

    public static float headMaxZ() {
        return 0.5F + fromModelCenter(HEAD_BOUNDS.maxZ() / 16.0F);
    }

    public static float yawDegrees(Direction facing) {
        return switch (facing) {
            case EAST -> 90.0F;
            case SOUTH -> 180.0F;
            case WEST -> 270.0F;
            default -> 0.0F;
        };
    }

    public record Box(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {}

    private static float fromModelCenter(float coordinate) {
        return (coordinate - 0.5F) * SCALE;
    }
}
