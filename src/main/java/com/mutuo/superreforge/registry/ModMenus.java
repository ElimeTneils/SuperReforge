package com.mutuo.superreforge.registry;

import com.mutuo.superreforge.SuperReforge;
import com.mutuo.superreforge.block.ReforgerMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** 注册带方块位置附加数据的熔核锻台菜单。 */
public final class ModMenus {
    private static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, SuperReforge.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<ReforgerMenu>> REFORGER =
            MENUS.register("reforger", () -> IMenuTypeExtension.create(ReforgerMenu::new));

    private ModMenus() {}

    public static void register(IEventBus bus) {
        MENUS.register(bus);
    }
}
