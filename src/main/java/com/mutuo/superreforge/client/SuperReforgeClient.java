package com.mutuo.superreforge.client;

import com.mutuo.superreforge.SuperReforge;
import com.mutuo.superreforge.registry.ModBlockEntities;
import com.mutuo.superreforge.registry.ModMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** 只在物理客户端加载的界面和方块实体渲染注册入口。 */
@Mod(value = SuperReforge.MOD_ID, dist = Dist.CLIENT)
public final class SuperReforgeClient {
    public SuperReforgeClient(IEventBus modBus) {
        modBus.addListener(this::registerScreens);
        modBus.addListener(this::registerRenderers);
    }

    private void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.REFORGER.get(), ReforgerScreen::new);
    }

    private void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                ModBlockEntities.REFORGER.get(), ReforgerBlockEntityRenderer::new);
    }
}
