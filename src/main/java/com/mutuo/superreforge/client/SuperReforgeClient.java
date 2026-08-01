package com.mutuo.superreforge.client;

import com.mutuo.superreforge.SuperReforge;
import com.mutuo.superreforge.registry.ModBlockEntities;
import com.mutuo.superreforge.registry.ModMenus;
import com.mutuo.superreforge.definition.DefinitionManager;
import com.mutuo.superreforge.network.ClientDefinitionSync;
import com.mutuo.superreforge.network.ClientPreviewState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;

/** 只在物理客户端加载的界面和方块实体渲染注册入口。 */
@Mod(value = SuperReforge.MOD_ID, dist = Dist.CLIENT)
public final class SuperReforgeClient {
    public SuperReforgeClient(IEventBus modBus) {
        modBus.addListener(this::registerScreens);
        modBus.addListener(this::registerRenderers);
        NeoForge.EVENT_BUS.addListener(this::onClientLogout);
    }

    private void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.REFORGER.get(), ReforgerScreen::new);
    }

    private void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                ModBlockEntities.REFORGER.get(), ReforgerBlockEntityRenderer::new);
    }

    /** 连接切换时清除上一世界的显示代次，防止专服沿用单人世界静态快照。 */
    private void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientDefinitionSync.clear();
        ClientPreviewState.clearAll();
        DefinitionManager.clearClientSession();
    }
}
