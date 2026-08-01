package com.mutuo.superreforge.client;

import com.mutuo.superreforge.network.PreviewLevel;
import com.mutuo.superreforge.network.PreviewModifier;
import com.mutuo.superreforge.network.ReforgePreviewPayload;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * 将服务端嵌套预览扁平化为完整锻造日志，并集中计算滚动边界。
 *
 * <p>该模型不接触屏幕尺寸以外的客户端状态，因此服务端给出的所有行都会保留，界面只负责裁剪可见部分。
 */
public final class ReforgerLogModel {
    public static final int LEVEL_HEIGHT = 10;
    public static final int MODIFIER_HEIGHT = 9;
    private static final int WHEEL_STEP = 18;
    private static final int MIN_THUMB_HEIGHT = 10;

    private final List<Row> rows;
    private final int contentHeight;

    private ReforgerLogModel(List<Row> rows, int contentHeight) {
        this.rows = List.copyOf(rows);
        this.contentHeight = contentHeight;
    }

    /** 按服务端顺序保留全部等级与词条，不根据当前 GUI 高度截断。 */
    public static ReforgerLogModel from(ReforgePreviewPayload preview) {
        List<Row> rows = new ArrayList<>();
        int top = 0;
        for (PreviewLevel level : preview.levels()) {
            rows.add(new Row(Kind.LEVEL, level.id(), level.name(), level.probability(), top, LEVEL_HEIGHT));
            top += LEVEL_HEIGHT;
            for (PreviewModifier modifier : level.modifiers()) {
                rows.add(new Row(
                        Kind.MODIFIER,
                        modifier.id(),
                        modifier.name(),
                        modifier.probability(),
                        top,
                        MODIFIER_HEIGHT));
                top += MODIFIER_HEIGHT;
            }
        }
        return new ReforgerLogModel(rows, top);
    }

    public List<Row> rows() {
        return rows;
    }

    public int contentHeight() {
        return contentHeight;
    }

    /** 将任意偏移收敛到有内容的区间，数据刷新后也不会留下空白页。 */
    public int clampScroll(int requested, int viewportHeight) {
        int maximum = Math.max(0, contentHeight - Math.max(0, viewportHeight));
        return Math.max(0, Math.min(requested, maximum));
    }

    /** Minecraft 的正滚轮值表示向上，因此从当前偏移中减去滚轮步长。 */
    public int scrollBy(int current, double wheelDelta, int viewportHeight) {
        int requested = current - (int) Math.round(wheelDelta * WHEEL_STEP);
        return clampScroll(requested, viewportHeight);
    }

    /** 根据可见比例计算滑块高度，并保证长列表仍至少有 10 像素可拖动。 */
    public int thumbHeight(int trackHeight, int viewportHeight) {
        if (trackHeight <= 0) {
            return 0;
        }
        if (contentHeight <= Math.max(0, viewportHeight)) {
            return trackHeight;
        }
        int proportional = (int) Math.floor((double) trackHeight * viewportHeight / contentHeight);
        return Math.max(Math.min(MIN_THUMB_HEIGHT, trackHeight), Math.min(trackHeight, proportional));
    }

    /** 把相对轨道顶端的滑块位置换算成内容偏移。 */
    public int scrollFromThumb(double thumbTop, int trackHeight, int thumbHeight, int viewportHeight) {
        int maximum = Math.max(0, contentHeight - Math.max(0, viewportHeight));
        int travel = Math.max(0, trackHeight - thumbHeight);
        if (maximum == 0 || travel == 0) {
            return 0;
        }
        double clampedTop = Math.max(0.0, Math.min(thumbTop, travel));
        return clampScroll((int) Math.round(clampedTop / travel * maximum), viewportHeight);
    }

    /** 一行日志的语义与几何；词条行的 ID 同时用于查找悬停 Attribute 定义。 */
    public record Row(
            Kind kind,
            ResourceLocation id,
            Component name,
            double probability,
            int top,
            int height) {}

    public enum Kind {
        LEVEL,
        MODIFIER
    }
}
