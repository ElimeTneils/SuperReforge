package com.mutuo.superreforge.registry;

import com.mutuo.superreforge.SuperReforge;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** 注册锻台物品和三种默认重铸媒介；媒介行为仍完全由数据定义。 */
public final class ModItems {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SuperReforge.MOD_ID);

    public static final DeferredItem<BlockItem> REFORGER =
            ITEMS.registerSimpleBlockItem("reforger", ModBlocks.REFORGER);
    public static final DeferredItem<Item> COMMON_REFORGE_STONE =
            ITEMS.registerSimpleItem("common_reforge_stone", new Item.Properties());
    public static final DeferredItem<Item> REFINED_REFORGE_STONE =
            ITEMS.registerSimpleItem("refined_reforge_stone", new Item.Properties());
    public static final DeferredItem<Item> SUPREME_REFORGE_STONE =
            ITEMS.registerSimpleItem("supreme_reforge_stone", new Item.Properties().fireResistant());

    private ModItems() {}

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
