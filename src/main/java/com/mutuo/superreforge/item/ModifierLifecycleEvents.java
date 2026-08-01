package com.mutuo.superreforge.item;

import com.mutuo.superreforge.config.SuperReforgeConfig;
import com.mutuo.superreforge.definition.DefinitionManager;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** 定期检查玩家库存，使铁砧、锻造升级、附魔和脚本动态失配都采用同一生命周期规则。 */
public final class ModifierLifecycleEvents {
    private static final int CHECK_INTERVAL_TICKS = 20;

    private ModifierLifecycleEvents() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(ModifierLifecycleEvents::onPlayerTick);
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || player.tickCount % CHECK_INTERVAL_TICKS != 0) {
            return;
        }
        var snapshot = DefinitionManager.snapshot();
        var settings = SuperReforgeConfig.snapshot();
        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ModifierLifecycle.reconcile(
                    inventory.getItem(slot), snapshot, settings, player.getRandom().nextLong());
        }
    }
}
