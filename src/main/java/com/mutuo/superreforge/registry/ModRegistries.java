package com.mutuo.superreforge.registry;

import net.neoforged.bus.api.IEventBus;

/** 保持入口简洁，并保证方块、物品、方块实体和菜单注册顺序一致。 */
public final class ModRegistries {
    private ModRegistries() {}

    public static void register(IEventBus bus) {
        ModBlocks.register(bus);
        ModItems.register(bus);
        ModCreativeTabs.register(bus);
        ModBlockEntities.register(bus);
        ModMenus.register(bus);
    }
}
