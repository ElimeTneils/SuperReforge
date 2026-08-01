package com.mutuo.superreforge.registry;

import com.mutuo.superreforge.SuperReforge;
import com.mutuo.superreforge.block.ReforgerBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/** 注册独立的熔核锻台方块。 */
public final class ModBlocks {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(SuperReforge.MOD_ID);

    public static final DeferredBlock<ReforgerBlock> REFORGER = BLOCKS.register(
            "reforger",
            () -> new ReforgerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(5.0F, 8.0F)
                    .requiresCorrectToolForDrops()
                    // 立体模型含熔核凹槽和锤架空隙，不能按不透明整方块遮挡相邻面。
                    .noOcclusion()
                    .lightLevel(state -> 6)));

    private ModBlocks() {}

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }
}
