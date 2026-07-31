package com.mutuo.superreforge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mutuo.superreforge.block.ReforgerBlockEntity;
import com.mutuo.superreforge.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;

/** 在静态熔核底座上方渲染独立锻锤，并按服务端同步 tick 下落。 */
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
                .orElse(0.92F);
        poseStack.pushPose();
        poseStack.translate(0.5, height, 0.5);
        poseStack.scale(0.72F, 0.72F, 0.72F);
        poseStack.mulPose(new Quaternionf().rotateXYZ((float) Math.toRadians(90), 0.0F, 0.0F));
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
