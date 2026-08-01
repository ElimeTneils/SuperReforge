# Super Reforge Vanilla GUI, Curios Hotfix, and Combat KubeJS Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver a copy-ready eight-rank KubeJS combat modifier script first, then fix the Curios null-context crash, rebuild the reforger as a vanilla-style container, restore a non-intersecting 45° hammer, publish two tutorials, and ship a verified JAR.

**Architecture:** Keep the existing datapack/KubeJS definition pipeline and optional dependency boundaries. Add content through the public `SuperReforge` script API, isolate nullable Curios context classification in a pure helper, flatten GUI preview rows to final modifiers, and centralize vanilla inventory and hammer geometry in tested layout classes.

**Tech Stack:** Java 21, Minecraft 1.21.1, NeoForge 21.1.244, JUnit 5, Gradle/ModDevGradle, KubeJS 2101.7.2, Curios 9.5.1, Critical Strike 1.0.4, Ranged Weapon API 2.3.3.

## Global Constraints

- Super Reforge must not register any Attribute.
- Curios, KubeJS, Critical Strike, and Ranged Weapon API remain optional dependencies and must not be bundled in the output JAR.
- KubeJS definitions override datapack definitions by identical resource ID.
- Existing rank IDs and saved modifier ID + seed data remain compatible.
- The default rank count remains exactly eight.
- Every touched Java, JSON, JavaScript, and documentation file receives useful Chinese comments explaining non-obvious behavior.
- Weight values remain relative and are normalized automatically; scripts never require a fixed sum.
- The server remains authoritative for costs, candidates, probabilities, and reforge results.

---

### Task 1: Copy-ready combat Attribute KubeJS script

**Files:**
- Create: `examples/kubejs/superreforge_combat_attributes.js`
- Create: `src/test/java/com/mutuo/superreforge/CombatKubeJsExampleTest.java`

**Interfaces:**
- Consumes: global KubeJS methods `SuperReforge.addLevel`, `addItemType`, and `addModifier`.
- Produces: a standalone `server_scripts` file containing eight display ranks and complete melee, ranged, armor, tool, and Curios pools.

- [ ] **Step 1: Write the failing script contract test**

```java
package com.mutuo.superreforge;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** 保证可复制的战斗词条脚本覆盖用户指定的八级、类型和第三方 Attribute。 */
final class CombatKubeJsExampleTest {
    private static final Path SCRIPT = Path.of("examples/kubejs/superreforge_combat_attributes.js");

    @Test
    void coversEveryRankPoolAndRequestedAttribute() throws Exception {
        String source = Files.readString(SCRIPT, StandardCharsets.UTF_8);
        assertAll(
                () -> assertTrue(source.contains("superreforge:worn")),
                () -> assertTrue(source.contains("superreforge:divine")),
                () -> assertTrue(source.contains("example:armor")),
                () -> assertTrue(source.contains("example:tool")),
                () -> assertTrue(source.contains("critical_strike:chance")),
                () -> assertTrue(source.contains("critical_strike:damage")),
                () -> assertTrue(source.contains("ranged_weapon:damage")),
                () -> assertTrue(source.contains("ranged_weapon:haste")),
                () -> assertTrue(source.contains("ranged_weapon:velocity")),
                () -> assertTrue(source.contains("ranged_weapon:pull_time")),
                () -> assertTrue(source.contains("add_multiplied_base")),
                () -> assertTrue(source.contains("add_multiplied_total")),
                () -> assertTrue(source.contains("curios:any")));
    }
}
```

- [ ] **Step 2: Run the contract test and verify the missing file fails**

Run:

```powershell
.\gradlew.bat test --tests com.mutuo.superreforge.CombatKubeJsExampleTest --rerun-tasks --console=plain
```

Expected: FAIL because `examples/kubejs/superreforge_combat_attributes.js` does not exist.

- [ ] **Step 3: Implement the table-driven KubeJS script**

Use these exact eight level rows so current catalysts and saved items keep their IDs:

```js
const SR_LEVELS = [
  { id: 'superreforge:worn', rank: 1, name: '等级 1', color: 'gray' },
  { id: 'superreforge:common', rank: 2, name: '等级 2', color: 'white' },
  { id: 'superreforge:fine', rank: 3, name: '等级 3', color: 'green' },
  { id: 'superreforge:rare', rank: 4, name: '等级 4', color: 'aqua' },
  { id: 'superreforge:epic', rank: 5, name: '等级 5', color: 'light_purple' },
  { id: 'superreforge:legendary', rank: 6, name: '等级 6', color: 'gold' },
  { id: 'superreforge:mythic', rank: 7, name: '等级 7', color: 'red' },
  { id: 'superreforge:divine', rank: 8, name: '等级 8', color: 'yellow' }
]

SR_LEVELS.forEach(level => SuperReforge.addLevel(level.id, {
  rank: level.rank,
  name: { text: level.name, color: level.color, bold: level.rank >= 7 }
}))
```

Add `example:armor` with `minecraft:head_armor`, `minecraft:chest_armor`, `minecraft:leg_armor`, and `minecraft:foot_armor`; add `example:tool` with `minecraft:pickaxes`, `minecraft:shovels`, and `minecraft:hoes`.

Create eight entries for each pool using the following exact value progression. Every random object is ordered as `min <= max`.

| rank | critical chance | critical damage | ranged damage | haste | velocity | pull time |
| ---: | --- | --- | --- | --- | --- | --- |
| 1 | 0.01–0.02 | 0.03–0.05 | 0.02–0.03 | 0.02–0.03 | 0.01–0.02 | -0.03–-0.02 |
| 2 | 0.02–0.035 | 0.05–0.08 | 0.03–0.05 | 0.03–0.05 | 0.02–0.035 | -0.05–-0.03 |
| 3 | 0.035–0.05 | 0.08–0.12 | 0.05–0.08 | 0.05–0.08 | 0.035–0.05 | -0.07–-0.05 |
| 4 | 0.05–0.07 | 0.12–0.17 | 0.08–0.11 | 0.08–0.11 | 0.05–0.075 | -0.10–-0.07 |
| 5 | 0.07–0.095 | 0.17–0.23 | 0.11–0.15 | 0.11–0.15 | 0.075–0.10 | -0.13–-0.10 |
| 6 | 0.095–0.125 | 0.23–0.30 | 0.15–0.20 | 0.15–0.19 | 0.10–0.13 | -0.16–-0.13 |
| 7 | 0.125–0.16 | 0.30–0.38 | 0.20–0.25 | 0.19–0.23 | 0.13–0.16 | -0.19–-0.16 |
| 8 | 0.16–0.20 | 0.38–0.48 | 0.25–0.32 | 0.23–0.28 | 0.16–0.20 | -0.22–-0.19 |

Critical Strike effects use `add_multiplied_base`. Ranged damage and velocity use `add_multiplied_total`; haste uses `add_multiplied_base`; pull time uses the negative `add_multiplied_total` range. All weapon effects target `mainhand`, armor effects target their vanilla armor slots, and Curios effects target `curios:any`.

Use these prefix series in rank order:

```js
const MELEE_NAMES = ['锐意', '强袭', '猎杀', '致命', '狂战', '破军', '弑神', '终焉']
const RANGED_NAMES = ['稳弦', '劲射', '疾羽', '鹰眼', '风行', '穿云', '逐星', '天穹']
const ARMOR_NAMES = ['坚韧', '守势', '铁壁', '不屈', '磐石', '圣佑', '不灭', '永恒']
const TOOL_NAMES = ['熟练', '利落', '精工', '迅捷', '大师', '奇迹', '神匠', '创世']
const CURIO_NAMES = ['微光', '灵辉', '祝福', '守护', '星辉', '命运', '神谕', '超越']
```

Use unique IDs `example:combat_melee_1` through `_8`, and the same pattern for `combat_ranged`, `combat_armor`, `combat_tool`, and `combat_curio`. Assign weight `100 - rank * 8`, yielding positive relative weights 92 through 36.

- [ ] **Step 4: Run the focused test and full tests**

Run:

```powershell
.\gradlew.bat test --tests com.mutuo.superreforge.CombatKubeJsExampleTest --rerun-tasks --console=plain
.\gradlew.bat test --console=plain
```

Expected: both commands PASS.

- [ ] **Step 5: Run the KubeJS server smoke test**

Copy the script into the KubeJS smoke instance `server_scripts`, launch the existing KubeJS test configuration, and verify the log contains the Super Reforge script layer publication plus `0 errors` and `0 warnings`.

- [ ] **Step 6: Commit and hand the script to the user immediately**

```powershell
git add examples/kubejs/superreforge_combat_attributes.js src/test/java/com/mutuo/superreforge/CombatKubeJsExampleTest.java
git commit -m "feat: add eight-rank combat KubeJS example"
```

Report the absolute clickable path to the `.js` file before starting Task 2.

---

### Task 2: Curios synthetic tooltip context crash

**Files:**
- Create: `src/main/java/com/mutuo/superreforge/compat/curios/CuriosContextPolicy.java`
- Create: `src/test/java/com/mutuo/superreforge/compat/curios/CuriosContextPolicyTest.java`
- Modify: `src/main/java/com/mutuo/superreforge/compat/curios/CuriosCompat.java`

**Interfaces:**
- Produces: `CuriosContextPolicy.classify(@Nullable LivingEntity): Kind` with `SYNTHETIC_CLIENT`, `CLIENT_ENTITY`, and `SERVER_ENTITY`.
- Consumes: Curios `CurioAttributeModifierEvent` and its nullable `SlotContext.entity()` contract.

- [ ] **Step 1: Write the failing null-context test**

```java
@Test
void nullEntityIsASyntheticClientTooltipContext() {
    assertEquals(
            CuriosContextPolicy.Kind.SYNTHETIC_CLIENT,
            CuriosContextPolicy.classify(null));
}
```

- [ ] **Step 2: Run the test and verify it fails because the policy is absent**

```powershell
.\gradlew.bat test --tests com.mutuo.superreforge.compat.curios.CuriosContextPolicyTest --rerun-tasks --console=plain
```

- [ ] **Step 3: Implement the minimal policy**

```java
public final class CuriosContextPolicy {
    public enum Kind { SYNTHETIC_CLIENT, CLIENT_ENTITY, SERVER_ENTITY }

    public static Kind classify(@Nullable LivingEntity entity) {
        if (entity == null) {
            return Kind.SYNTHETIC_CLIENT;
        }
        return entity.level().isClientSide ? Kind.CLIENT_ENTITY : Kind.SERVER_ENTITY;
    }
}
```

In `CuriosCompat`, read `SlotContext` and entity once. Reconcile only for `SERVER_ENTITY`; treat `SYNTHETIC_CLIENT` as client-side when applying tooltip visibility checks. Continue resolving saved item data and adding stable modifiers for synthetic contexts.

- [ ] **Step 4: Run focused and full tests**

```powershell
.\gradlew.bat test --tests com.mutuo.superreforge.compat.curios.CuriosContextPolicyTest --rerun-tasks --console=plain
.\gradlew.bat test --console=plain
```

- [ ] **Step 5: Reproduce the original client path**

Launch with Curios 9.5.1, BountifulBaubles 1.2.5, and JEI. Open the creative inventory, type into search, wait for search-tree tooltip indexing, and confirm no `CuriosCompat.onCurioAttributes` exception appears.

- [ ] **Step 6: Commit**

```powershell
git add src/main/java/com/mutuo/superreforge/compat/curios/CuriosContextPolicy.java src/main/java/com/mutuo/superreforge/compat/curios/CuriosCompat.java src/test/java/com/mutuo/superreforge/compat/curios/CuriosContextPolicyTest.java
git commit -m "fix: handle synthetic Curios tooltip contexts"
```

---

### Task 3: Numeric ranks and one-row final modifier log

**Files:**
- Modify: `src/main/resources/data/superreforge/superreforge/levels/*.json`
- Modify: `src/main/resources/assets/superreforge/lang/zh_cn.json`
- Modify: `src/main/resources/assets/superreforge/lang/en_us.json`
- Modify: `src/main/java/com/mutuo/superreforge/client/ReforgerLogModel.java`
- Modify: `src/main/java/com/mutuo/superreforge/client/ReforgerScreen.java`
- Modify: `src/test/java/com/mutuo/superreforge/client/ReforgerLogModelTest.java`
- Modify: `src/test/java/com/mutuo/superreforge/definition/DefaultResourcesTest.java`

**Interfaces:**
- Produces: one `ReforgerLogModel.Row` per `PreviewModifier`, containing level name, modifier ID/name, final probability, top, and height.
- Consumes: unchanged nested server payload; no protocol format change is needed.

- [ ] **Step 1: Change tests to require eight numeric names and one row per final modifier**

For a preview with two levels and three modifiers per level, require `rows().size() == 6`, not 8. Assert the first row preserves both `Component.literal("one")` as its level and `Component.literal("one_a")` as its modifier.

- [ ] **Step 2: Run focused tests and verify current hierarchical rows fail**

```powershell
.\gradlew.bat test --tests com.mutuo.superreforge.client.ReforgerLogModelTest --tests com.mutuo.superreforge.definition.DefaultResourcesTest --rerun-tasks --console=plain
```

- [ ] **Step 3: Flatten rows and update default rank Components**

Replace `Kind.LEVEL`/`Kind.MODIFIER` rows with:

```java
public record Row(
        Component levelName,
        ResourceLocation modifierId,
        Component modifierName,
        double probability,
        int top,
        int height) {}
```

Use a constant row height of 10. Render level name, modifier name, and percentage as three columns; hover the entire row and resolve tooltip data by `modifierId`.

Update fallback and translations to `等级 1` through `等级 8` and `Level 1` through `Level 8`; retain resource IDs, ranks, and colors.

- [ ] **Step 4: Run focused and full tests**

```powershell
.\gradlew.bat test --tests com.mutuo.superreforge.client.ReforgerLogModelTest --tests com.mutuo.superreforge.definition.DefaultResourcesTest --rerun-tasks --console=plain
.\gradlew.bat test --console=plain
```

- [ ] **Step 5: Commit**

```powershell
git add src/main/java/com/mutuo/superreforge/client/ReforgerLogModel.java src/main/java/com/mutuo/superreforge/client/ReforgerScreen.java src/main/resources/assets/superreforge/lang src/main/resources/data/superreforge/superreforge/levels src/test/java/com/mutuo/superreforge/client/ReforgerLogModelTest.java src/test/java/com/mutuo/superreforge/definition/DefaultResourcesTest.java
git commit -m "feat: show numeric ranks with final modifier rows"
```

---

### Task 4: Strict vanilla container and exact player inventory geometry

**Files:**
- Modify: `src/main/java/com/mutuo/superreforge/block/ReforgerLayout.java`
- Modify: `src/main/java/com/mutuo/superreforge/client/ReforgerScreen.java`
- Modify: `src/test/java/com/mutuo/superreforge/block/ReforgerLayoutTest.java`

**Interfaces:**
- Produces: `PLAYER_PANEL_WIDTH = 176`, `playerPanelLeft()`, and `playerSlotX(column) = playerPanelLeft() + 8 + column * 18`.
- Consumes: the unchanged `ReforgerMenu` slot creation loop.

- [ ] **Step 1: Write failing symmetry and 36-slot geometry assertions**

```java
@Test
void vanillaInventoryHasSymmetricMarginsAndExactlyNineColumns() {
    int panelLeft = ReforgerLayout.playerPanelLeft();
    assertEquals(176, ReforgerLayout.PLAYER_PANEL_WIDTH);
    assertEquals(panelLeft + 8, ReforgerLayout.playerSlotX(0));
    assertEquals(panelLeft + 8 + 8 * 18, ReforgerLayout.playerSlotX(8));
    assertEquals(8, panelLeft + 176 - (ReforgerLayout.playerSlotX(8) + 16));
}
```

- [ ] **Step 2: Run the layout test and verify the current extra-width panel fails**

```powershell
.\gradlew.bat test --tests com.mutuo.superreforge.block.ReforgerLayoutTest --rerun-tasks --console=plain
```

- [ ] **Step 3: Implement scheme A**

Use a vanilla light-gray background and bevel colors. Draw the player panel from `playerPanelLeft()` for exactly 176 pixels. Draw 27 inventory and 9 hotbar slot backgrounds at each actual `Slot` coordinate using the vanilla container slot sprite or the same 18×18 light/dark bevel used by vanilla. Keep the existing real `Button` widget; remove custom dark/copper/purple panels and use the standard disabled state when no valid quote exists.

Keep the 286×218 screen envelope so the full log remains visible, but ensure every player slot and its background share the same `ReforgerLayout` coordinate.

- [ ] **Step 4: Run tests and capture a client screenshot**

```powershell
.\gradlew.bat test --tests com.mutuo.superreforge.block.ReforgerLayoutTest --rerun-tasks --console=plain
.\gradlew.bat test --console=plain
```

In the client, verify the right margin is eight pixels, no tenth column appears, empty slots remain visible, and the start button is visible in enabled and disabled states.

- [ ] **Step 5: Commit**

```powershell
git add src/main/java/com/mutuo/superreforge/block/ReforgerLayout.java src/main/java/com/mutuo/superreforge/client/ReforgerScreen.java src/test/java/com/mutuo/superreforge/block/ReforgerLayoutTest.java
git commit -m "feat: use a vanilla-style reforger container"
```

---

### Task 5: Restore a pivoted 45-degree hammer without intersecting cubes

**Files:**
- Modify: `src/main/resources/assets/superreforge/models/item/forge_hammer.json`
- Modify: `src/main/java/com/mutuo/superreforge/client/ReforgerBlockEntityRenderer.java`
- Modify: `src/main/java/com/mutuo/superreforge/client/ReforgerHammerGeometry.java`
- Modify: `src/main/java/com/mutuo/superreforge/client/ReforgerRenderState.java`
- Modify: `src/test/java/com/mutuo/superreforge/client/ReforgerHammerGeometryTest.java`
- Modify: `src/test/java/com/mutuo/superreforge/client/ReforgerRenderStateTest.java`

**Interfaces:**
- Produces: local pivot translation and angle curve for rest, raised, contact, rebound, and settled frames.
- Consumes: existing synchronized total/remaining ticks and block horizontal facing.

- [ ] **Step 1: Add failing model-bound and angle assertions**

Require the model copper band and iron head boxes to have zero volumetric overlap. Require rest angle `-45°`, a raised angle less negative than rest, a contact angle that places the hammer face at or above `ANVIL_TOP`, and four horizontal facings that only change yaw.

- [ ] **Step 2: Run focused geometry tests and verify the vertical model fails**

```powershell
.\gradlew.bat test --tests com.mutuo.superreforge.client.ReforgerHammerGeometryTest --tests com.mutuo.superreforge.client.ReforgerRenderStateTest --rerun-tasks --console=plain
```

- [ ] **Step 3: Remodel and rotate around a fixed local pivot**

Restore the slanted work pose but separate model cubes at their boundaries: wood handle ends where the copper socket begins; copper socket ends where the iron head begins; the reinforcement cap touches but does not overlap the head. Remove duplicated item-display tilt.

In the renderer, translate to the tested local pivot, apply block yaw, apply the animated local strike rotation, translate back from the pivot, then render. `ReforgerRenderState` returns hammer angle instead of treating world Y translation as the primary motion. Preserve the 20-tick raise, strike, rebound, and settle timing plus core intensity.

- [ ] **Step 4: Run tests and inspect north/east/south/west in game**

```powershell
.\gradlew.bat test --tests com.mutuo.superreforge.client.ReforgerHammerGeometryTest --tests com.mutuo.superreforge.client.ReforgerRenderStateTest --rerun-tasks --console=plain
.\gradlew.bat test --console=plain
```

Capture front, side, and top views at rest and contact. Reject any striped z-fighting or entry into the back beam, posts, work surface, or anvil.

- [ ] **Step 5: Commit**

```powershell
git add src/main/resources/assets/superreforge/models/item/forge_hammer.json src/main/java/com/mutuo/superreforge/client/ReforgerBlockEntityRenderer.java src/main/java/com/mutuo/superreforge/client/ReforgerHammerGeometry.java src/main/java/com/mutuo/superreforge/client/ReforgerRenderState.java src/test/java/com/mutuo/superreforge/client/ReforgerHammerGeometryTest.java src/test/java/com/mutuo/superreforge/client/ReforgerRenderStateTest.java
git commit -m "fix: pivot the diagonal reforger hammer"
```

---

### Task 6: Separate Datapack and KubeJS tutorials

**Files:**
- Create: `docs/DATAPACK_TUTORIAL.md`
- Create: `docs/KUBEJS_TUTORIAL.md`
- Modify: `README.md`
- Modify: `docs/DATAPACK_API.md`
- Modify: `docs/KUBEJS_API.md`
- Modify: `src/test/java/com/mutuo/superreforge/DocumentationContractTest.java`

**Interfaces:**
- Produces: two beginner workflows linked from README and API references.
- Consumes: validated examples, schemas, and the combat Attribute script from Task 1.

- [ ] **Step 1: Add failing documentation link and required-section assertions**

Assert both tutorial files exist; README links both; the Datapack tutorial contains `pack.mcmeta`, all four definition directories, `/reload`, Curios, strict JSON, and troubleshooting; the KubeJS tutorial contains `server_scripts`, all `add*` methods, predicates, stage persistence, override priority, all six third-party Attribute IDs, and troubleshooting.

- [ ] **Step 2: Run the documentation test and verify missing tutorials fail**

```powershell
.\gradlew.bat test --tests com.mutuo.superreforge.DocumentationContractTest --rerun-tasks --console=plain
```

- [ ] **Step 3: Write the two tutorials**

Datapack chapters: installation, directory tree, `pack.mcmeta`, numeric level, item type, multi-Attribute modifier, Curios modifier, catalyst, weights, strict JSON versus annotated JSONC, reload, and error diagnosis.

KubeJS chapters: file location, level/type/modifier/catalyst registration, KubeJS predicates and groups, same-ID override, persistent server stage, cost multiplication/addition, Critical Strike/Ranged Weapon operations, reload, and error diagnosis. Link the complete combat script instead of duplicating its entire source.

- [ ] **Step 4: Run documentation and full tests**

```powershell
.\gradlew.bat test --tests com.mutuo.superreforge.DocumentationContractTest --rerun-tasks --console=plain
.\gradlew.bat test --console=plain
```

- [ ] **Step 5: Commit**

```powershell
git add docs/DATAPACK_TUTORIAL.md docs/KUBEJS_TUTORIAL.md docs/DATAPACK_API.md docs/KUBEJS_API.md README.md src/test/java/com/mutuo/superreforge/DocumentationContractTest.java
git commit -m "docs: add Datapack and KubeJS tutorials"
```

---

### Task 7: Release verification, JAR, and GitHub update

**Files:**
- Modify: `CHANGELOG.md`
- Modify: `docs/FILE_REFERENCE.md`
- Output: `build/libs/superreforge-0.1.0.jar`

**Interfaces:**
- Produces: verified source commits, a new installable JAR, checksum, and updated PR #1.

- [ ] **Step 1: Document every changed file and release-visible fix**

Add changelog entries for the combat KubeJS example, synthetic Curios context fix, numeric levels, vanilla GUI, corrected inventory width, one-row probabilities, diagonal hammer, and tutorials. Update file reference responsibilities and effects.

- [ ] **Step 2: Run the clean full build**

```powershell
.\gradlew.bat clean test build --rerun-tasks --console=plain
```

Expected: BUILD SUCCESSFUL and all tests pass.

- [ ] **Step 3: Run compatibility matrix**

Start dedicated server configurations for core-only, Curios-only, KubeJS-only, and Curios+KubeJS. Require each to reach `Done`, require KubeJS scripts to report zero errors/warnings, and require Curios to initialize its bridge.

- [ ] **Step 4: Run client compatibility smoke test**

Launch with Curios 9.5.1, BountifulBaubles 1.2.5, JEI, KubeJS, Critical Strike 1.0.4, and Ranged Weapon API 2.3.3. Verify creative search, Curios tooltips, the A-scheme GUI, scrolling, button, 36 player slots, numeric ranks, modifier hover details, and all hammer views.

- [ ] **Step 5: Inspect JAR contents and checksum**

Require the expected Super Reforge classes and runtime assets in the artifact. The repository-level `examples/` directory remains outside the runtime JAR. Reject embedded Curios, KubeJS, Critical Strike, Ranged Weapon API classes and any `data/superreforge/attribute` resources. Record file size and SHA-256.

- [ ] **Step 6: Commit release docs and push**

```powershell
git add CHANGELOG.md docs/FILE_REFERENCE.md
git commit -m "docs: record vanilla reforger and compatibility fixes"
git push origin feature/super-reforge-build
```

Confirm GitHub PR #1 contains every implementation commit and give the user the final JAR link, checksum, test totals, compatibility matrix, script link, tutorial links, and PR link.
