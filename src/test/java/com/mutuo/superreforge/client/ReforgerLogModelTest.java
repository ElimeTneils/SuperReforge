package com.mutuo.superreforge.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mutuo.superreforge.network.PreviewLevel;
import com.mutuo.superreforge.network.PreviewModifier;
import com.mutuo.superreforge.network.ReforgePreviewPayload;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

/** 验证锻造日志保存全部行，并在任意视口高度下稳定计算滚动边界。 */
final class ReforgerLogModelTest {
    @Test
    void flattensEveryLevelAndModifierWithoutViewportTruncation() {
        ReforgerLogModel model = ReforgerLogModel.from(preview());

        // 每个最终词条独占一行，等级仅作为该行的第一列而非额外标题行。
        assertEquals(6, model.rows().size());
        List<ExpectedRow> expectedRows = List.of(
                expected("one", "one_a", 0.13333333333333333, 0),
                expected("one", "one_b", 0.13333333333333333, 10),
                expected("one", "one_c", 0.13333333333333333, 20),
                expected("two", "two_a", 0.19999999999999998, 30),
                expected("two", "two_b", 0.19999999999999998, 40),
                expected("two", "two_c", 0.19999999999999998, 50));
        for (int index = 0; index < expectedRows.size(); index++) {
            assertRow(expectedRows.get(index), model.rows().get(index));
        }
        assertEquals(60, model.contentHeight());
    }

    @Test
    void clampsWheelAndThumbScrollingToContentBounds() {
        ReforgerLogModel model = ReforgerLogModel.from(preview());

        assertEquals(0, model.clampScroll(-5, 40));
        assertEquals(20, model.clampScroll(999, 40));
        assertEquals(18, model.scrollBy(0, -1.0, 40));
        assertEquals(0, model.scrollBy(18, 1.0, 40));
        assertEquals(26, model.thumbHeight(40, 40));
        assertEquals(20, model.scrollFromThumb(14.0, 40, 26, 40));
    }

    @Test
    void disablesScrollingWhenEverythingFits() {
        ReforgerLogModel model = ReforgerLogModel.from(new ReforgePreviewPayload(
                1, 0L, -1, 1, 0, false, List.of()));

        assertEquals(0, model.clampScroll(20, 40));
        assertEquals(40, model.thumbHeight(40, 40));
        assertEquals(0, model.scrollFromThumb(10.0, 40, 40, 40));
    }

    private static ReforgePreviewPayload preview() {
        return new ReforgePreviewPayload(
                1,
                0L,
                -1,
                2,
                3,
                false,
                List.of(level("one", 0.4), level("two", 0.6)));
    }

    private static PreviewLevel level(String path, double probability) {
        return new PreviewLevel(
                id(path),
                Component.literal(path),
                probability,
                List.of(
                        modifier(path + "_a", probability / 3.0),
                        modifier(path + "_b", probability / 3.0),
                        modifier(path + "_c", probability / 3.0)));
    }

    private static PreviewModifier modifier(String path, double probability) {
        return new PreviewModifier(id(path), Component.literal(path), probability);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("test", path);
    }

    /** 全字段断言防止未来重构只保留第一行或打乱连续几何位置。 */
    private static void assertRow(ExpectedRow expected, ReforgerLogModel.Row actual) {
        assertEquals(Component.literal(expected.level()), actual.levelName());
        assertEquals(id(expected.modifier()), actual.modifierId());
        assertEquals(Component.literal(expected.modifier()), actual.modifierName());
        assertEquals(expected.probability(), actual.probability());
        assertEquals(expected.top(), actual.top());
        assertEquals(ReforgerLogModel.ROW_HEIGHT, actual.height());
    }

    /** 人工列出的预期行，不复用日志模型的扁平化或坐标计算。 */
    private record ExpectedRow(String level, String modifier, double probability, int top) {}

    private static ExpectedRow expected(String level, String modifier, double probability, int top) {
        return new ExpectedRow(level, modifier, probability, top);
    }
}
