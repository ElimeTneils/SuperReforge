package com.mutuo.superreforge;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
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
                "docs/DATAPACK_TUTORIAL.md",
                "docs/KUBEJS_API.md",
                "docs/KUBEJS_TUTORIAL.md",
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

    @Test
    void shipsSeparateDatapackAndKubeJsTutorialsWithSupportedWorkflows() throws IOException {
        Path datapackPath = ROOT.resolve("docs/DATAPACK_TUTORIAL.md");
        Path kubeJsPath = ROOT.resolve("docs/KUBEJS_TUTORIAL.md");
        assertTrue(Files.isRegularFile(datapackPath), "缺少独立 Datapack 教程");
        assertTrue(Files.isRegularFile(kubeJsPath), "缺少独立 KubeJS 教程");

        String datapack = Files.readString(datapackPath, StandardCharsets.UTF_8);
        for (String required : List.of(
                "pack.mcmeta",
                "superreforge/levels",
                "superreforge/item_types",
                "superreforge/modifiers",
                "superreforge/catalysts",
                "/reload",
                "严格 JSON",
                "JSONC",
                "curios:any",
                "## 常见问题与排错")) {
            assertTrue(datapack.contains(required), "Datapack 教程缺少工作流内容：" + required);
        }

        String kubeJs = Files.readString(kubeJsPath, StandardCharsets.UTF_8);
        for (String required : List.of(
                "server_scripts",
                "SuperReforge.addLevel",
                "SuperReforge.addItemType",
                "SuperReforge.addModifier",
                "SuperReforge.addCatalyst",
                "SuperReforge.addPredicate",
                "SuperReforge.addStage",
                "kubejs_predicate",
                "SuperReforge.setStageActive",
                "event.server",
                "持久化",
                "同 ID",
                "KubeJS",
                "datapack",
                "superreforge:worn",
                "superreforge:divine",
                "critical_strike:chance",
                "critical_strike:damage",
                "ranged_weapon:damage",
                "ranged_weapon:haste",
                "ranged_weapon:velocity",
                "ranged_weapon:pull_time",
                "## 常见问题与排错")) {
            assertTrue(kubeJs.contains(required), "KubeJS 教程缺少已支持内容：" + required);
        }

        String choice = markdownSection(kubeJs, "## 2. 先二选一：最小教学脚本或完整战斗脚本");
        for (String required : List.of(
                "方案 A",
                "方案 B",
                "二选一",
                "不能同时",
                "同一 KubeJS 层",
                "datapack",
                "superreforge_combat_attributes.js")) {
            assertTrue(choice.contains(required), "KubeJS 教程的二选一流程缺少：" + required);
        }
        assertTrue(choice.contains("覆盖 datapack") && choice.contains("重复 ID"),
                "教程必须区分 KubeJS 覆盖 datapack 与同层重复 ID");

        String weights = markdownSection(kubeJs, "## 4. 词条、媒介与相对权重");
        assertTrue(weights.contains("相对权重") && weights.contains("自动归一化"),
                "KubeJS 教程必须说明 relative weight 的归一化");

        String combat = markdownSection(kubeJs, "## 5. Critical Strike 与 Ranged Weapon Attribute");
        assertTrue(combat.contains("add_multiplied_base") && combat.contains("add_multiplied_total"),
                "战斗 Attribute 章节必须保留真实 operation");
        assertLocalMarkdownLink(kubeJsPath, "../examples/kubejs/superreforge_combat_attributes.js");

        String stages = markdownSection(kubeJs, "## 6. 持久化全服阶段与最高优先级成本");
        assertTrue(stages.contains("SuperReforge.getActiveStage") && stages.contains("event.server"),
                "阶段章节必须展示真实服务器查询流程");
        assertTrue(stages.contains("floor(") && stages.contains("multiplier") && stages.contains("addition"),
                "阶段章节必须说明完整成本公式");

        assertLocalMarkdownLink(ROOT.resolve("README.md"), "docs/DATAPACK_TUTORIAL.md");
        assertLocalMarkdownLink(ROOT.resolve("README.md"), "docs/KUBEJS_TUTORIAL.md");
        assertLocalMarkdownLink(ROOT.resolve("docs/DATAPACK_API.md"), "DATAPACK_TUTORIAL.md");
        assertLocalMarkdownLink(ROOT.resolve("docs/DATAPACK_API.md"), "KUBEJS_TUTORIAL.md");
        assertLocalMarkdownLink(ROOT.resolve("docs/KUBEJS_API.md"), "DATAPACK_TUTORIAL.md");
        assertLocalMarkdownLink(ROOT.resolve("docs/KUBEJS_API.md"), "KUBEJS_TUTORIAL.md");
    }

    @Test
    void recordsVanillaReforgerAndCompatibilityReleaseChanges() throws IOException {
        String changelog = Files.readString(ROOT.resolve("CHANGELOG.md"), StandardCharsets.UTF_8);
        for (String required : List.of(
                "superreforge_combat_attributes.js",
                "UTF-8",
                "等级 1",
                "Curios",
                "合成客户端上下文",
                "原版 GUI",
                "36",
                "单行概率",
                "-45°",
                "SAT",
                "DATAPACK_TUTORIAL.md",
                "KUBEJS_TUTORIAL.md")) {
            assertTrue(changelog.contains(required), "0.1.0 更新日志缺少发布可见变更：" + required);
        }
    }

    @Test
    void documentsEveryVanillaReforgerAndCompatibilityFileResponsibility() throws IOException {
        String reference = Files.readString(ROOT.resolve("docs/FILE_REFERENCE.md"), StandardCharsets.UTF_8);
        for (String required : List.of(
                "examples/kubejs/superreforge_combat_attributes.js",
                "docs/DATAPACK_TUTORIAL.md",
                "docs/KUBEJS_TUTORIAL.md",
                "CuriosContextPolicy.java",
                "SYNTHETIC_CLIENT",
                "PlayerSlotPosition",
                "36",
                "ReforgerLogModel.java",
                "单行",
                "TransformPlan",
                "SAT",
                "CombatKubeJsExampleTest.java",
                "CuriosContextPolicyTest.java")) {
            assertTrue(reference.contains(required), "文件参考缺少 Task 7 责任：" + required);
        }
    }

    private static String markdownSection(String markdown, String heading) {
        int start = markdown.indexOf(heading);
        assertTrue(start >= 0, "缺少章节：" + heading);
        int end = markdown.indexOf("\n## ", start + heading.length());
        return end < 0 ? markdown.substring(start) : markdown.substring(start, end);
    }

    private static void assertLocalMarkdownLink(Path source, String target) throws IOException {
        String markdown = Files.readString(source, StandardCharsets.UTF_8);
        Pattern link = Pattern.compile("\\[[^\\]]+\\]\\(" + Pattern.quote(target) + "\\)");
        assertTrue(link.matcher(markdown).find(), source + " 缺少真实 Markdown 链接：" + target);
        Path resolved = source.getParent().resolve(target).normalize();
        assertTrue(Files.isRegularFile(resolved), source + " 的 Markdown 链接目标不存在：" + resolved);
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
