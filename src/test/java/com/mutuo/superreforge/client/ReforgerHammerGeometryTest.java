package com.mutuo.superreforge.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

/** 验证动力锻锤的真实资源边界、固定枢轴变换和四向全动画安全净空。 */
final class ReforgerHammerGeometryTest {
    @Test
    void contactPointTouchesTheAnvilWithoutEnteringIt() {
        float headBottom = ReforgerHammerGeometry.hammerFaceY(
                ReforgerHammerGeometry.CONTACT_ANGLE_DEGREES);

        assertEquals(ReforgerHammerGeometry.ANVIL_TOP, headBottom, 0.0001F);
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

    @Test
    void modelResourcePartsMeetAtBoundariesWithoutPositiveVolumeOverlap() throws IOException {
        Map<String, ReforgerHammerGeometry.Box> elements = readNamedElements(
                "/assets/superreforge/models/item/forge_hammer.json");
        ReforgerHammerGeometry.Box handle = elements.get("handle");
        ReforgerHammerGeometry.Box socket = elements.get("socket");
        ReforgerHammerGeometry.Box head = elements.get("head");
        ReforgerHammerGeometry.Box cap = elements.get("cap");

        // 资源模型是最终渲染输入；所有部件两两检查，避免只覆盖三个已知相邻关系。
        List<ReforgerHammerGeometry.Box> boxes = List.copyOf(elements.values());
        for (int first = 0; first < boxes.size(); first++) {
            for (int second = first + 1; second < boxes.size(); second++) {
                assertTrue(ReforgerHammerGeometry.boxesDoNotOverlap(boxes.get(first), boxes.get(second)));
            }
        }
        assertEquals(handle.minY(), socket.maxY());
        assertEquals(socket.minY(), head.maxY());
        assertEquals(cap.maxX(), head.minX());
        assertEquals(handle, ReforgerHammerGeometry.HANDLE_BOUNDS);
        assertEquals(socket, ReforgerHammerGeometry.SOCKET_BOUNDS);
        assertEquals(head, ReforgerHammerGeometry.HEAD_BOUNDS);
        assertEquals(cap, ReforgerHammerGeometry.CAP_BOUNDS);
    }

    @Test
    void transformPlanKeepsTheLocalPivotAndStepOrderForEveryFacing() {
        ReforgerHammerGeometry.TransformPlan north = ReforgerHammerGeometry.transformPlan(
                Direction.NORTH, ReforgerHammerGeometry.REST_ANGLE_DEGREES);
        for (Direction facing : new Direction[] {Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            ReforgerHammerGeometry.TransformPlan plan = ReforgerHammerGeometry.transformPlan(
                    facing, ReforgerHammerGeometry.REST_ANGLE_DEGREES);
            // 只有方块 yaw 随朝向变化，局部枢轴、角度、缩放和模型偏移保持一致。
            assertEquals(north.pivotX(), plan.pivotX());
            assertEquals(north.pivotY(), plan.pivotY());
            assertEquals(north.pivotZ(), plan.pivotZ());
            assertEquals(north.localZAngleDegrees(), plan.localZAngleDegrees());
            assertEquals(north.scale(), plan.scale());
            assertEquals(north.modelOffsetY(), plan.modelOffsetY());
        }
        assertEquals(List.of(
                        ReforgerHammerGeometry.TransformOperation.TRANSLATE,
                        ReforgerHammerGeometry.TransformOperation.ROTATE_Y,
                        ReforgerHammerGeometry.TransformOperation.ROTATE_Z,
                        ReforgerHammerGeometry.TransformOperation.SCALE,
                        ReforgerHammerGeometry.TransformOperation.TRANSLATE),
                north.steps().stream().map(ReforgerHammerGeometry.TransformStep::operation).toList());
    }

    @Test
    void transformedHammerAvoidsEveryRealForgePartAtEveryCriticalFrameAndFacing() throws IOException {
        Map<String, ReforgerHammerGeometry.Box> hammerParts = readNamedElements(
                "/assets/superreforge/models/item/forge_hammer.json");
        List<ModelElement> forgeParts = readElements(
                "/assets/superreforge/models/block/reforger.json");
        Map<String, Float> frames = Map.of(
                "rest", ReforgerHammerGeometry.REST_ANGLE_DEGREES,
                "raised", ReforgerHammerGeometry.RAISED_ANGLE_DEGREES,
                "contact", ReforgerHammerGeometry.CONTACT_ANGLE_DEGREES,
                "rebound", ReforgerHammerGeometry.REBOUND_ANGLE_DEGREES,
                "settled", ReforgerHammerGeometry.REST_ANGLE_DEGREES);
        for (Direction facing : new Direction[] {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            for (var frame : frames.entrySet()) {
                ReforgerHammerGeometry.TransformPlan plan = ReforgerHammerGeometry.transformPlan(
                        facing, frame.getValue());
                for (var hammerPart : hammerParts.entrySet()) {
                    for (ModelElement forgePart : forgeParts) {
                        String message = "frame=" + frame.getKey() + ", facing=" + facing
                                + ", hammer=" + hammerPart.getKey() + ", forge=" + forgePart.name();
                        // 锤子和方块模型必须共享 facing yaw；边界接触允许，正体积相交禁止。
                        assertFalse(ReforgerHammerGeometry.hasPositiveVolumeIntersection(
                                hammerPart.getValue(), forgePart.box(), plan), message);
                    }
                }
                float clearance = ReforgerHammerGeometry.hammerFaceY(frame.getValue())
                        - ReforgerHammerGeometry.ANVIL_TOP;
                if (frame.getKey().equals("contact")) {
                    assertEquals(0.0F, clearance, 0.0001F);
                } else if (frame.getKey().equals("raised") || frame.getKey().equals("rebound")) {
                    assertTrue(clearance > 0.0F, "frame=" + frame.getKey() + ", clearance=" + clearance);
                } else {
                    assertTrue(clearance >= 0.0F, "frame=" + frame.getKey() + ", clearance=" + clearance);
                }
            }
        }
    }

    /** 从真实模型资源读取具名方块，避免测试只验证复制到 Java 的常量。 */
    private static Map<String, ReforgerHammerGeometry.Box> readNamedElements(String resource) throws IOException {
        Map<String, ReforgerHammerGeometry.Box> boxes = new HashMap<>();
        for (ModelElement element : readElements(resource)) {
            boxes.put(element.name(), element.box());
        }
        return boxes;
    }

    /** 直接解析资源中的全部元素，锻台禁区不会被人工合并或缩小。 */
    private static List<ModelElement> readElements(String resource) throws IOException {
        try (var reader = new InputStreamReader(Objects.requireNonNull(
                ReforgerHammerGeometryTest.class.getResourceAsStream(resource)), StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            JsonArray elements = root.getAsJsonArray("elements");
            List<ModelElement> boxes = new ArrayList<>();
            for (var value : elements) {
                JsonObject element = value.getAsJsonObject();
                boxes.add(new ModelElement(element.get("name").getAsString(), boxFrom(element)));
            }
            return boxes;
        }
    }

    /** Minecraft JSON 的 from/to 是模型像素坐标，直接转换为几何盒。 */
    private static ReforgerHammerGeometry.Box boxFrom(JsonObject element) {
        JsonArray from = element.getAsJsonArray("from");
        JsonArray to = element.getAsJsonArray("to");
        return new ReforgerHammerGeometry.Box(
                from.get(0).getAsFloat(), from.get(1).getAsFloat(), from.get(2).getAsFloat(),
                to.get(0).getAsFloat(), to.get(1).getAsFloat(), to.get(2).getAsFloat());
    }

    private record ModelElement(String name, ReforgerHammerGeometry.Box box) {}
}
