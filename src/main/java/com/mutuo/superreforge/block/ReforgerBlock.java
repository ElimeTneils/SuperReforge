package com.mutuo.superreforge.block;

import com.mojang.serialization.MapCodec;
import com.mutuo.superreforge.registry.ModBlockEntities;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/** 独立的 3D 熔核锻台；服务端打开菜单，破坏时安全掉落库存或 pending 结果。 */
public final class ReforgerBlock extends BaseEntityBlock {
    public static final MapCodec<ReforgerBlock> CODEC = simpleCodec(ReforgerBlock::new);

    public ReforgerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity instanceof ReforgerBlockEntity reforger) {
                serverPlayer.openMenu(reforger, buffer -> buffer.writeBlockPos(pos));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity instanceof ReforgerBlockEntity reforger) {
                List<net.minecraft.world.item.ItemStack> drops = reforger.drainForDrop();
                drops.forEach(stack -> Containers.dropItemStack(
                        level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack));
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ReforgerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide
                ? null
                : createTickerHelper(type, ModBlockEntities.REFORGER.get(), ReforgerBlockEntity::serverTick);
    }
}
