package com.mutuo.superreforge.network;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;

/** 服务端记录每位玩家是否确认了当前显示快照；缺失/失败 ACK 时禁止开始重铸。 */
public final class DefinitionSyncTracker {
    private static final Map<UUID, Long> EXPECTED = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> READY = new ConcurrentHashMap<>();

    private DefinitionSyncTracker() {}

    public static void expect(ServerPlayer player, long generation) {
        expect(player.getUUID(), generation);
    }

    static void expect(UUID player, long generation) {
        EXPECTED.put(player, generation);
        READY.remove(player);
    }

    public static void acknowledge(ServerPlayer player, long generation, boolean success) {
        acknowledge(player.getUUID(), generation, success);
    }

    static void acknowledge(UUID player, long generation, boolean success) {
        Long expected = EXPECTED.get(player);
        if (success && expected != null && expected == generation) {
            READY.put(player, generation);
        } else {
            READY.remove(player);
        }
    }

    public static boolean canReforge(ServerPlayer player, long generation) {
        return canReforge(player.getUUID(), generation);
    }

    static boolean canReforge(UUID player, long generation) {
        Long expected = EXPECTED.get(player);
        Long ready = READY.get(player);
        return expected != null && expected == generation && ready != null && ready == generation;
    }

    public static void remove(ServerPlayer player) {
        EXPECTED.remove(player.getUUID());
        READY.remove(player.getUUID());
    }

    static void clearForTests() {
        EXPECTED.clear();
        READY.clear();
    }
}
