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

        assertEquals(8, model.rows().size());
        assertEquals(ReforgerLogModel.Kind.LEVEL, model.rows().get(0).kind());
        assertEquals(ReforgerLogModel.Kind.MODIFIER, model.rows().get(1).kind());
        assertEquals(ReforgerLogModel.Kind.LEVEL, model.rows().get(4).kind());
        assertEquals(74, model.contentHeight());
    }

    @Test
    void clampsWheelAndThumbScrollingToContentBounds() {
        ReforgerLogModel model = ReforgerLogModel.from(preview());

        assertEquals(0, model.clampScroll(-5, 40));
        assertEquals(34, model.clampScroll(999, 40));
        assertEquals(18, model.scrollBy(0, -1.0, 40));
        assertEquals(0, model.scrollBy(18, 1.0, 40));
        assertEquals(21, model.thumbHeight(40, 40));
        assertEquals(34, model.scrollFromThumb(19.0, 40, 21, 40));
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
}
