package com.mutuo.superreforge;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Guards the published KubeJS combat example against losing its optional Attribute integrations. */
final class CombatKubeJsExampleTest {
    private static final Path ROOT = findProjectRoot();

    @Test
    void shipsCombatExampleWithEveryOptionalAttributePool() throws IOException {
        String source = Files.readString(
                ROOT.resolve("examples/kubejs/superreforge_combat_attributes.js"), StandardCharsets.UTF_8);
        for (String required : List.of(
                "superreforge:worn",
                "superreforge:divine",
                "example:armor",
                "example:tool",
                "critical_strike:chance",
                "critical_strike:damage",
                "ranged_weapon:damage",
                "ranged_weapon:haste",
                "ranged_weapon:velocity",
                "ranged_weapon:pull_time",
                "add_multiplied_base",
                "add_multiplied_total",
                "curios:any")) {
            assertTrue(source.contains(required), "combat KubeJS example is missing: " + required);
        }
    }

    private static Path findProjectRoot() {
        Path cursor = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (cursor != null && !Files.isRegularFile(cursor.resolve("gradlew.bat"))) {
            cursor = cursor.getParent();
        }
        if (cursor == null) {
            throw new IllegalStateException("Unable to locate the Super Reforge project root");
        }
        return cursor;
    }
}
