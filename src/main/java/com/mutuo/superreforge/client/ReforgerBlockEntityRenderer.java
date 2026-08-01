package com.mutuo.superreforge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mutuo.superreforge.block.ReforgerBlock;
import com.mutuo.superreforge.block.ReforgerBlockEntity;
import com.mutuo.superreforge.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** 按共享变换计划把动力锻锤绕固定连接点渲染为斜置摆动。 */
public final class ReforgerBlockEntityRenderer implements BlockEntityRenderer<ReforgerBlockEntity> {
    public ReforgerBlockEntityRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(ReforgerBlockEntity blockEntity, float partialTick, PoseStack poseStack,
            MultiBufferSource buffers, int packedLight, int packedOverlay) {
        float hammerAngle = blockEntity.pending()
                .map(value -> ReforgerRenderState.fromTicks(
                        value.totalTicks(), value.remainingTicks(), partialTick).hammerAngleDegrees())
                .orElse(ReforgerHammerGeometry.REST_ANGLE_DEGREES);
        ReforgerHammerGeometry.TransformPlan plan = ReforgerHammerGeometry.transformPlan(
                blockEntity.getBlockState().getValue(ReforgerBlock.FACING), hammerAngle);
        poseStack.pushPose();
        // renderer 逐步消费几何层的同一份计划，测试与实际矩阵顺序不会各自漂移。
        for (ReforgerHammerGeometry.TransformStep step : plan.steps()) {
            switch (step.operation()) {
                case TRANSLATE -> poseStack.translate(step.x(), step.y(), step.z());
                case ROTATE_Y -> poseStack.mulPose(Axis.YP.rotationDegrees(step.x()));
                case ROTATE_Z -> poseStack.mulPose(Axis.ZP.rotationDegrees(step.x()));
                case SCALE -> poseStack.scale(step.x(), step.y(), step.z());
            }
        }
        Minecraft.getInstance().getItemRenderer().renderStatic(new ItemStack(ModItems.FORGE_HAMMER.get()),
                ItemDisplayContext.FIXED, packedLight, packedOverlay, poseStack, buffers,
                blockEntity.getLevel(), (int) blockEntity.getBlockPos().asLong());
        poseStack.popPose();
    }
}
