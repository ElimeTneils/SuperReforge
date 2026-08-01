package com.mutuo.superreforge.registry;

import com.mutuo.superreforge.SuperReforge;
import com.mutuo.superreforge.block.ReforgerBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** 注册保存两槽库存和 pendingResult 的熔核锻台方块实体。 */
public final class ModBlockEntities {
    private static final DeferredRegister<BlockEntityType<?>> TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, SuperReforge.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ReforgerBlockEntity>> REFORGER =
            TYPES.register("reforger", () -> BlockEntityType.Builder.of(
                            ReforgerBlockEntity::new, ModBlocks.REFORGER.get())
                    .build(null));

    private ModBlockEntities() {}

    public static void register(IEventBus bus) {
        TYPES.register(bus);
    }
}
