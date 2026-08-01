package com.mutuo.superreforge.client;

import net.minecraft.util.Mth;

/** 把同步 tick 转成锻锤模型中心高度、熔核亮度和总体进度，供 GUI 与 3D 渲染共用。 */
public record ReforgerRenderState(float progress, float hammerAngleDegrees, float coreIntensity) {
    public static ReforgerRenderState fromTicks(int totalTicks, int remainingTicks, float partialTick) {
        if (totalTicks <= 0) {
            return new ReforgerRenderState(
                    1.0F, ReforgerHammerGeometry.REST_ANGLE_DEGREES, 0.35F);
        }
        float elapsed = totalTicks - remainingTicks + partialTick;
        float progress = Mth.clamp(elapsed / totalTicks, 0.0F, 1.0F);

        // 一秒内完成抬锤、加速落下、短回弹和复位；结束高度与无任务静止高度完全一致。
        float hammerAngle;
        if (progress <= 0.25F) {
            float raise = smooth(progress / 0.25F);
            hammerAngle = Mth.lerp(
                    raise,
                    ReforgerHammerGeometry.REST_ANGLE_DEGREES,
                    ReforgerHammerGeometry.RAISED_ANGLE_DEGREES);
        } else if (progress <= 0.50F) {
            float fall = (progress - 0.25F) / 0.25F;
            hammerAngle = Mth.lerp(
                    fall * fall,
                    ReforgerHammerGeometry.RAISED_ANGLE_DEGREES,
                    ReforgerHammerGeometry.CONTACT_ANGLE_DEGREES);
        } else if (progress <= 0.65F) {
            float rebound = smooth((progress - 0.50F) / 0.15F);
            hammerAngle = Mth.lerp(
                    rebound,
                    ReforgerHammerGeometry.CONTACT_ANGLE_DEGREES,
                    ReforgerHammerGeometry.REBOUND_ANGLE_DEGREES);
        } else {
            float settle = smooth((progress - 0.65F) / 0.35F);
            hammerAngle = Mth.lerp(
                    settle,
                    ReforgerHammerGeometry.REBOUND_ANGLE_DEGREES,
                    ReforgerHammerGeometry.REST_ANGLE_DEGREES);
        }

        float distanceFromImpact = Math.abs(progress - 0.50F);
        float core = Mth.clamp(1.0F - distanceFromImpact * 3.2F, 0.35F, 1.0F);
        return new ReforgerRenderState(progress, hammerAngle, core);
    }

    private static float smooth(float value) {
        float clamped = Mth.clamp(value, 0.0F, 1.0F);
        return clamped * clamped * (3.0F - 2.0F * clamped);
    }
}
