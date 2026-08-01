package com.mutuo.superreforge.registry;

import com.mutuo.superreforge.SuperReforge;
import com.mutuo.superreforge.item.ReforgeData;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** 注册 Super Reforge 写入物品的持久数据组件。 */
public final class ModDataComponents {
    private static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, SuperReforge.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ReforgeData>> REFORGE_DATA =
            COMPONENTS.register("reforge_data", () -> DataComponentType.<ReforgeData>builder()
                    .persistent(ReforgeData.CODEC)
                    .networkSynchronized(ReforgeData.STREAM_CODEC)
                    .build());

    private ModDataComponents() {}

    /** 将 deferred register 接入模组事件总线。 */
    public static void register(IEventBus modBus) {
        COMPONENTS.register(modBus);
    }
}
