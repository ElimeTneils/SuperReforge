package com.mutuo.superreforge.client;

import java.util.List;
import net.minecraft.core.Direction;

/** 集中描述动力锻锤的模型边界、固定枢轴和可验证的局部变换。 */
public final class ReforgerHammerGeometry {
    private static final float CONTACT_EPSILON = 0.000001F;
    public static final float SCALE = 0.62F;
    public static final float ANVIL_TOP = 11.0F / 16.0F;
    public static final float PIVOT_X = 0.5F;
    public static final float PIVOT_Y = 1.262909F;
    public static final float PIVOT_Z = 0.5F;
    public static final float PIVOT_MODEL_Y = 1.0F;
    public static final float REST_ANGLE_DEGREES = -45.0F;
    public static final float RAISED_ANGLE_DEGREES = -75.0F;
    public static final float CONTACT_ANGLE_DEGREES = -45.0F;
    public static final float REBOUND_ANGLE_DEGREES = -47.0F;

    /** 下列边界与 forge_hammer.json 的四个元素一一对应。 */
    public static final Box HANDLE_BOUNDS = new Box(7, 7, 7, 9, 16, 9);
    public static final Box SOCKET_BOUNDS = new Box(6, 5, 6, 10, 7, 10);
    public static final Box HEAD_BOUNDS = new Box(3, 1, 5, 14, 5, 11);
    public static final Box CAP_BOUNDS = new Box(2, 1, 5, 3, 5, 11);

    private ReforgerHammerGeometry() {}

    /** 生成 renderer 直接消费的固定步骤，避免矩阵调用顺序和测试语义分离。 */
    public static TransformPlan transformPlan(Direction facing, float localZAngleDegrees) {
        return new TransformPlan(
                PIVOT_X, PIVOT_Y, PIVOT_Z, yawDegrees(facing), localZAngleDegrees,
                SCALE, -PIVOT_MODEL_Y + 0.5F);
    }

    /** 将模型像素边界按枢轴、yaw、局部 Z 角度和缩放变为世界轴对齐边界。 */
    public static Box transformedBounds(Box source, TransformPlan plan) {
        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        float maxZ = Float.NEGATIVE_INFINITY;
        for (float x : new float[] {source.minX(), source.maxX()}) {
            for (float y : new float[] {source.minY(), source.maxY()}) {
                for (float z : new float[] {source.minZ(), source.maxZ()}) {
                    Point point = transformPoint(x, y, z, plan);
                    minX = Math.min(minX, point.x());
                    minY = Math.min(minY, point.y());
                    minZ = Math.min(minZ, point.z());
                    maxX = Math.max(maxX, point.x());
                    maxY = Math.max(maxY, point.y());
                    maxZ = Math.max(maxZ, point.z());
                }
            }
        }
        return new Box(minX, minY, minZ, maxX, maxY, maxZ);
    }

    /** 计算旋转后锤面最低点，接触帧必须只到达砧面上方。 */
    public static float hammerFaceY(float angleDegrees) {
        return transformedBounds(HEAD_BOUNDS, transformPlan(Direction.NORTH, angleDegrees)).minY();
    }

    /** 两个盒子仅边界相接时视为不相交，正体积相交才是穿模。 */
    public static boolean boxesDoNotOverlap(Box first, Box second) {
        return first.maxX() <= second.minX() || second.maxX() <= first.minX()
                || first.maxY() <= second.minY() || second.maxY() <= first.minY()
                || first.maxZ() <= second.minZ() || second.maxZ() <= first.minZ();
    }

    /**
     * 精确检查旋转锤盒与同朝向锻台盒是否共享正体积。
     * 边界接触不算碰撞；锻台盒与动态锤子使用同一个 facing yaw，避免四向测试出现假禁区。
     */
    public static boolean hasPositiveVolumeIntersection(Box hammer, Box forge, TransformPlan plan) {
        Point[] hammerCorners = corners(hammer, point -> transformPoint(point.x(), point.y(), point.z(), plan));
        Point[] forgeCorners = corners(forge, point -> transformForgePoint(point, plan.yawDegrees()));
        Point[] hammerAxes = hammerAxes(plan);
        Point[] forgeAxes = forgeAxes(plan.yawDegrees());

        for (Point axis : hammerAxes) {
            if (separates(hammerCorners, forgeCorners, axis)) {
                return false;
            }
        }
        for (Point axis : forgeAxes) {
            if (separates(hammerCorners, forgeCorners, axis)) {
                return false;
            }
        }
        for (Point hammerAxis : hammerAxes) {
            for (Point forgeAxis : forgeAxes) {
                Point cross = cross(hammerAxis, forgeAxis);
                if (lengthSquared(cross) > CONTACT_EPSILON && separates(hammerCorners, forgeCorners, cross)) {
                    return false;
                }
            }
        }
        return true;
    }

    /** 保留中央砧面平面投影检查所需的锤头局部边界。 */
    public static float headMinX() { return 0.5F + fromModelCenter(HEAD_BOUNDS.minX() / 16.0F); }
    public static float headMaxX() { return 0.5F + fromModelCenter(HEAD_BOUNDS.maxX() / 16.0F); }
    public static float headMinZ() { return 0.5F + fromModelCenter(HEAD_BOUNDS.minZ() / 16.0F); }
    public static float headMaxZ() { return 0.5F + fromModelCenter(HEAD_BOUNDS.maxZ() / 16.0F); }

    /** 方块朝向只决定水平 yaw，不改变任何局部运动参数。 */
    public static float yawDegrees(Direction facing) {
        return switch (facing) {
            case EAST -> 90.0F;
            case SOUTH -> 180.0F;
            case WEST -> 270.0F;
            default -> 0.0F;
        };
    }

    private static Point transformPoint(float modelX, float modelY, float modelZ, TransformPlan plan) {
        float x = (modelX / 16.0F - 0.5F) * plan.scale();
        float y = (modelY / 16.0F - 0.5F + plan.modelOffsetY()) * plan.scale();
        float z = (modelZ / 16.0F - 0.5F) * plan.scale();
        float localRadians = (float) Math.toRadians(plan.localZAngleDegrees());
        float localX = x * (float) Math.cos(localRadians) - y * (float) Math.sin(localRadians);
        float localY = x * (float) Math.sin(localRadians) + y * (float) Math.cos(localRadians);
        float yawRadians = (float) Math.toRadians(plan.yawDegrees());
        float worldX = localX * (float) Math.cos(yawRadians) + z * (float) Math.sin(yawRadians);
        float worldZ = -localX * (float) Math.sin(yawRadians) + z * (float) Math.cos(yawRadians);
        return new Point(plan.pivotX() + worldX, plan.pivotY() + localY, plan.pivotZ() + worldZ);
    }

    private static Point transformForgePoint(Point point, float yawDegrees) {
        float x = point.x() / 16.0F - 0.5F;
        float y = point.y() / 16.0F;
        float z = point.z() / 16.0F - 0.5F;
        float radians = (float) Math.toRadians(yawDegrees);
        float cos = (float) Math.cos(radians);
        float sin = (float) Math.sin(radians);
        return new Point(0.5F + x * cos + z * sin, y, 0.5F - x * sin + z * cos);
    }

    private static Point[] hammerAxes(TransformPlan plan) {
        float localRadians = (float) Math.toRadians(plan.localZAngleDegrees());
        float localCos = (float) Math.cos(localRadians);
        float localSin = (float) Math.sin(localRadians);
        float yawRadians = (float) Math.toRadians(plan.yawDegrees());
        float yawCos = (float) Math.cos(yawRadians);
        float yawSin = (float) Math.sin(yawRadians);
        return new Point[] {
                new Point(localCos * yawCos, localSin, -localCos * yawSin),
                new Point(-localSin * yawCos, localCos, localSin * yawSin),
                new Point(yawSin, 0.0F, yawCos)
        };
    }

    private static Point[] forgeAxes(float yawDegrees) {
        float radians = (float) Math.toRadians(yawDegrees);
        float cos = (float) Math.cos(radians);
        float sin = (float) Math.sin(radians);
        return new Point[] {
                new Point(cos, 0.0F, -sin),
                new Point(0.0F, 1.0F, 0.0F),
                new Point(sin, 0.0F, cos)
        };
    }

    private static Point[] corners(Box box, PointTransform transform) {
        Point[] points = new Point[8];
        int index = 0;
        for (float x : new float[] {box.minX(), box.maxX()}) {
            for (float y : new float[] {box.minY(), box.maxY()}) {
                for (float z : new float[] {box.minZ(), box.maxZ()}) {
                    points[index++] = transform.apply(new Point(x, y, z));
                }
            }
        }
        return points;
    }

    private static boolean separates(Point[] first, Point[] second, Point axis) {
        float firstMin = Float.POSITIVE_INFINITY;
        float firstMax = Float.NEGATIVE_INFINITY;
        float secondMin = Float.POSITIVE_INFINITY;
        float secondMax = Float.NEGATIVE_INFINITY;
        for (Point point : first) {
            float projection = dot(point, axis);
            firstMin = Math.min(firstMin, projection);
            firstMax = Math.max(firstMax, projection);
        }
        for (Point point : second) {
            float projection = dot(point, axis);
            secondMin = Math.min(secondMin, projection);
            secondMax = Math.max(secondMax, projection);
        }
        float tolerance = CONTACT_EPSILON * (float) Math.sqrt(lengthSquared(axis));
        return firstMax <= secondMin + tolerance || secondMax <= firstMin + tolerance;
    }

    private static float dot(Point first, Point second) {
        return first.x() * second.x() + first.y() * second.y() + first.z() * second.z();
    }

    private static Point cross(Point first, Point second) {
        return new Point(
                first.y() * second.z() - first.z() * second.y(),
                first.z() * second.x() - first.x() * second.z(),
                first.x() * second.y() - first.y() * second.x());
    }

    private static float lengthSquared(Point point) {
        return dot(point, point);
    }

    private static float fromModelCenter(float coordinate) { return (coordinate - 0.5F) * SCALE; }

    /** 模型或世界中的轴对齐盒；单位由调用方决定。 */
    public record Box(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {}

    /** 不可变渲染计划的顺序与 PoseStack 调用顺序严格一致。 */
    public record TransformPlan(
            float pivotX, float pivotY, float pivotZ, float yawDegrees, float localZAngleDegrees,
            float scale, float modelOffsetY) {
        public List<TransformStep> steps() {
            return List.of(
                    new TransformStep(TransformOperation.TRANSLATE, pivotX, pivotY, pivotZ),
                    new TransformStep(TransformOperation.ROTATE_Y, yawDegrees, 0.0F, 0.0F),
                    new TransformStep(TransformOperation.ROTATE_Z, localZAngleDegrees, 0.0F, 0.0F),
                    new TransformStep(TransformOperation.SCALE, scale, scale, scale),
                    new TransformStep(TransformOperation.TRANSLATE, 0.0F, modelOffsetY, 0.0F));
        }
    }

    /** renderer 可直接执行、测试可直接检查的单步变换。 */
    public record TransformStep(TransformOperation operation, float x, float y, float z) {}

    public enum TransformOperation {
        TRANSLATE,
        ROTATE_Y,
        ROTATE_Z,
        SCALE
    }

    /** 私有点值对象避免在纯几何计算中依赖客户端渲染类。 */
    private record Point(float x, float y, float z) {}

    @FunctionalInterface
    private interface PointTransform {
        Point apply(Point point);
    }
}
