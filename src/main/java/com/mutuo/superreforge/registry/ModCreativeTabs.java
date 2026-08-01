package com.mutuo.superreforge.registry;

import com.mutuo.superreforge.SuperReforge;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** 注册独立 Super Reforge 创造页签，并集中维护玩家可见物品的固定顺序。 */
public final class ModCreativeTabs {
    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SuperReforge.MOD_ID);

    // 动画锤子是方块实体渲染实现细节，故意不加入这份玩家可见清单。
    private static final List<Entry> VISIBLE_ITEMS = List.of(
            entry(ModItems.REFORGER),
            entry(ModItems.COMMON_REFORGE_STONE),
            entry(ModItems.REFINED_REFORGE_STONE),
            entry(ModItems.SUPREME_REFORGE_STONE),
            entry(ModItems.ENHANCEMENT_STONE_4),
            entry(ModItems.ENHANCEMENT_STONE_5),
            entry(ModItems.ENHANCEMENT_STONE_6));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> SUPER_REFORGE =
            TABS.register("super_reforge", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.superreforge"))
                    .icon(() -> ModItems.REFORGER.get().getDefaultInstance())
                    .displayItems((parameters, output) ->
                            VISIBLE_ITEMS.forEach(value -> output.accept(value.item().get())))
                    .build());

    private ModCreativeTabs() {}

    /** 测试与外部诊断读取的稳定展示顺序；返回副本防止运行时篡改页签。 */
    public static List<ResourceLocation> visibleItemIds() {
        return VISIBLE_ITEMS.stream().map(Entry::id).toList();
    }

    public static void register(IEventBus bus) {
        TABS.register(bus);
    }

    private static Entry entry(DeferredItem<? extends Item> item) {
        return new Entry(item.getId(), item);
    }

    private record Entry(ResourceLocation id, Supplier<? extends Item> item) {}
}
