package com.mutuo.superreforge;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 保证发布包旁的中文指南、可复制示例和 JSON Schema 不会在重构时遗漏。 */
final class DocumentationContractTest {
    /*
     * ModDevGradle 会把 NeoForge 单元测试的工作目录切到 build 下，不能假定 user.dir 就是项目根目录。
     * 从实际工作目录向上寻找 Gradle Wrapper，可让 IDE、命令行和 CI 使用同一份契约测试。
     */
    private static final Path ROOT = findProjectRoot();

    @Test
    void shipsAllPublicApiGuidesAndExamples() throws IOException {
        List<String> required = List.of(
                "docs/CONFIGURATION.md",
                "docs/DATAPACK_API.md",
                "docs/KUBEJS_API.md",
                "docs/FILE_REFERENCE.md",
                "schemas/level.schema.json",
                "schemas/item_type.schema.json",
                "schemas/modifier.schema.json",
                "schemas/catalyst.schema.json",
                "examples/datapack/pack.mcmeta",
                "examples/datapack/data/example/superreforge/modifiers/legendary_blade.json",
                "examples/datapack/readable/modifiers/legendary_blade.jsonc",
                "examples/kubejs/superreforge_definitions.js",
                "examples/kubejs/superreforge_progression.js",
                "CHANGELOG.md",
                "CREDITS.md");
        for (String path : required) {
            assertTrue(Files.isRegularFile(ROOT.resolve(path)), "缺少发布文档或示例：" + path);
        }

        String kube = Files.readString(
                ROOT.resolve("examples/kubejs/superreforge_definitions.js"), StandardCharsets.UTF_8);
        assertTrue(kube.contains("SuperReforge.addItemType"));
        assertTrue(kube.contains("SuperReforge.addModifier"));
        assertTrue(kube.contains("kubejs_predicate"));

        String readme = Files.readString(ROOT.resolve("README.md"), StandardCharsets.UTF_8);
        assertTrue(readme.contains("安装"));
        assertTrue(readme.contains("DATAPACK_API.md"));
        assertFalse(readme.contains("当前仓库正在"), "发布 README 不应继续声称项目仍在占位开发中");
    }

    private static Path findProjectRoot() {
        Path cursor = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (cursor != null && !Files.isRegularFile(cursor.resolve("gradlew.bat"))) {
            cursor = cursor.getParent();
        }
        if (cursor == null) {
            throw new IllegalStateException("无法从测试工作目录定位 Super Reforge 项目根目录");
        }
        return cursor;
    }
}
