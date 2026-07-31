package com.mutuo.superreforge.progress;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;

/** 注册阶段定义、保存激活状态，并只选择最高优先级阶段。 */
public final class ProgressService {
    private static final Map<String, ProgressStage> STAGES = new ConcurrentHashMap<>();

    private ProgressService() {}

    public static void replaceStages(Collection<ProgressStage> stages) {
        Map<String, ProgressStage> replacement = new java.util.LinkedHashMap<>();
        for (ProgressStage stage : stages) {
            if (replacement.putIfAbsent(stage.id(), stage) != null) {
                throw new IllegalArgumentException("重复阶段 ID: " + stage.id());
            }
        }
        STAGES.clear();
        STAGES.putAll(replacement);
    }

    public static void setActive(MinecraftServer server, String id, boolean active) {
        data(server).setActive(id, active);
    }

    public static Optional<ProgressStage> highestActive(MinecraftServer server) {
        return highestActive(STAGES.values(), data(server).activeStages());
    }

    public static Optional<ProgressStage> highestActive(
            Collection<ProgressStage> stages, Set<String> activeIds) {
        return stages.stream()
                .filter(stage -> activeIds.contains(stage.id()))
                .sorted(Comparator.comparingInt(ProgressStage::priority)
                        .reversed()
                        .thenComparing(ProgressStage::id))
                .findFirst();
    }

    private static ServerProgressData data(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(ServerProgressData.FACTORY, ServerProgressData.FILE_ID);
    }
}
