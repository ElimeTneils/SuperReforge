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

/** 在静态熔核底座上方竖直渲染独立动力锻锤，并按服务端同步 tick 下落。 */
public final class ReforgerBlockEntityRenderer implements BlockEntityRenderer<ReforgerBlockEntity> {
    public ReforgerBlockEntityRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(
            ReforgerBlockEntity blockEntity,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay) {
        float height = blockEntity.pending()
                .map(value -> ReforgerRenderState.fromTicks(
                        value.totalTicks(), value.remainingTicks(), partialTick)
                        .hammerHeight())
                .orElse(ReforgerHammerGeometry.REST_ORIGIN_Y);
        poseStack.pushPose();
        poseStack.translate(0.5, height, 0.5);
        // 方块朝向只影响水平 yaw；模型本身已经是锤头朝下的工作姿态。
        float yRotation = ReforgerHammerGeometry.yawDegrees(
                blockEntity.getBlockState().getValue(ReforgerBlock.FACING));
        poseStack.mulPose(Axis.YP.rotationDegrees(yRotation));
        poseStack.scale(
                ReforgerHammerGeometry.SCALE,
                ReforgerHammerGeometry.SCALE,
                ReforgerHammerGeometry.SCALE);
        Minecraft.getInstance().getItemRenderer().renderStatic(
                new ItemStack(ModItems.FORGE_HAMMER.get()),
                ItemDisplayContext.FIXED,
                packedLight,
                packedOverlay,
                poseStack,
                buffers,
                blockEntity.getLevel(),
                (int) blockEntity.getBlockPos().asLong());
        poseStack.popPose();
    }
}
