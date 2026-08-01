package com.mutuo.superreforge.definition;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.List;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import org.junit.jupiter.api.Test;

/** 使用原版真实资源管理器锁定 reload 目录参数，防止末尾斜杠再次让整个服务器数据包加载失败。 */
final class DefinitionReloadListenerTest {
    @Test
    void emptyResourceManagerCanPrepareDefinitionsWithoutInvalidDirectoryPaths() {
        try (var manager = new MultiPackResourceManager(PackType.SERVER_DATA, List.of())) {
            assertDoesNotThrow(() -> new DefinitionReloadListener().prepare(manager, null));
        }
    }
}
