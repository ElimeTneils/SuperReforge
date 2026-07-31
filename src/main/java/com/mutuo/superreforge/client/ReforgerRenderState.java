package com.mutuo.superreforge.client;

import net.minecraft.util.Mth;

/** 把同步 tick 转成锤头高度、熔核亮度和总体进度，供 GUI 与 3D 渲染共用。 */
public record ReforgerRenderState(float progress, float hammerHeight, float coreIntensity) {
    public static ReforgerRenderState fromTicks(int totalTicks, int remainingTicks, float partialTick) {
        if (totalTicks <= 0) {
            return new ReforgerRenderState(1.0F, 0.35F, 0.35F);
        }
        float elapsed = totalTicks - remainingTicks + partialTick;
        float progress = Mth.clamp(elapsed / totalTicks, 0.0F, 1.0F);

        // 前 30% 保持抬起，30%-50% 快速落锤，之后小幅回弹并停在低位。
        float hammerHeight;
        if (progress < 0.30F) {
            hammerHeight = 1.15F;
        } else if (progress < 0.50F) {
            float fall = (progress - 0.30F) / 0.20F;
            hammerHeight = Mth.lerp(fall * fall, 1.15F, 0.38F);
        } else {
            float bounce = (float) Math.sin((progress - 0.50F) * Math.PI * 4.0F)
                    * (1.0F - progress) * 0.10F;
            hammerHeight = 0.38F + Math.max(0.0F, bounce);
        }

        float distanceFromImpact = Math.abs(progress - 0.50F);
        float core = Mth.clamp(1.0F - distanceFromImpact * 3.2F, 0.35F, 1.0F);
        return new ReforgerRenderState(progress, hammerHeight, core);
    }
}
