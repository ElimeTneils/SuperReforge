package com.mutuo.superreforge.network;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** 客户端按 containerId 缓存最后一份预览；类本身不引用客户端专属 Minecraft 类型。 */
public final class ClientPreviewState {
    private static final Map<Integer, ReforgePreviewPayload> PREVIEWS = new ConcurrentHashMap<>();

    private ClientPreviewState() {}

    public static void accept(ReforgePreviewPayload payload) {
        PREVIEWS.put(payload.containerId(), payload);
    }

    public static Optional<ReforgePreviewPayload> get(int containerId) {
        return Optional.ofNullable(PREVIEWS.get(containerId));
    }

    public static void clear(int containerId) {
        PREVIEWS.remove(containerId);
    }

    public static void clearAll() {
        PREVIEWS.clear();
    }
}
