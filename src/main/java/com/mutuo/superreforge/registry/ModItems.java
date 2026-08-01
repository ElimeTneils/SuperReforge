package com.mutuo.superreforge.registry;

import com.mutuo.superreforge.SuperReforge;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** 注册锻台物品和六级强化媒介；媒介的等级权重、成本与适用类型仍完全由数据定义。 */
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
    /** 第四级强化石；默认不内置配方，由整合包的数据包或 KubeJS 决定获取方式。 */
    public static final DeferredItem<Item> ENHANCEMENT_STONE_4 =
            ITEMS.registerSimpleItem("enhancement_stone_4", new Item.Properties().fireResistant());
    /** 第五级强化石；使用独立注册 ID，避免与其他强化石的媒介匹配发生歧义。 */
    public static final DeferredItem<Item> ENHANCEMENT_STONE_5 =
            ITEMS.registerSimpleItem("enhancement_stone_5", new Item.Properties().fireResistant());
    /** 第六级强化石；作为只抽取等级 8 的最高级媒介。 */
    public static final DeferredItem<Item> ENHANCEMENT_STONE_6 =
            ITEMS.registerSimpleItem("enhancement_stone_6", new Item.Properties().fireResistant());
    /** 方块实体渲染使用的独立 3D 锻锤，不加入默认配方或创造栏。 */
    public static final DeferredItem<Item> FORGE_HAMMER =
            ITEMS.registerSimpleItem("forge_hammer", new Item.Properties().stacksTo(1));

    private ModItems() {}

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
