# Critical Strike KubeJS Compat Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 Minecraft 1.21.1 / NeoForge 构建一个独立附属模组，使所有进入 KubeJS 同一受伤事件管线且能追踪到玩家的非原生伤害自动使用 Critical Strike 暴击系统，并提供可修改和成功通知两类 KubeJS 事件。

**Architecture:** `LivingDamageEvent.Pre` 的最低优先级监听器把 NeoForge/KubeJS 可见伤害转成独立的判定请求；来源解析、数值状态、随机决策和伤害乘算分成可单元测试组件。两个范围 Mixin 在 Critical Strike 已原生处理的玩家攻击与持久弹射物调用期间设置线程局部守卫，兼容桥据此跳过，避免二次抽取；KubeJS 插件通过 `kubejs.plugins.txt` 注册专用事件组。

**Tech Stack:** Java 21、Minecraft 1.21.1、NeoForge 21.1.244、ModDevGradle 2.0.142、Critical Strike 1.0.4+1.21.1、KubeJS 2101.7.2-build.368、Rhino 2101.2.7-build.81、Mixin/MixinExtras、JUnit 5、Mockito、NeoForge GameTest。

## Global Constraints

- 工程根目录固定为 `D:\Codex\critical-strike-kubejs-compat`。
- Mod ID 固定为 `critical_strike_kubejs_compat`，显示名为 `Critical Strike KubeJS Compat`。
- Java 版本固定为 21；Minecraft 版本固定为 1.21.1。
- NeoForge 编译版本固定为 21.1.244，元数据兼容范围为 `[21.1,21.2)`。
- KubeJS 与 Critical Strike 都是必需依赖，均声明 `side = "BOTH"`；业务判定只在逻辑服务端运行。
- 不修改、复制或覆盖 Critical Strike 与 KubeJS 上游源码及配置文件。
- 只有能从 `DamageSource` 解析出玩家攻击者的伤害才进入兼容判定。
- 原生玩家攻击和 Critical Strike 已支持的持久弹射物必须完全保留原行为，并且每次伤害最多判定一次暴击。
- 普通 KubeJS `EntityEvents.beforeHurt` 修改必须先于兼容桥的暴击乘算。
- 自伤默认拒绝，但必须允许专用 KubeJS `modify` 事件显式开启。
- 所有脚本输入在写回伤害前必须验证有限性、范围和非负性。
- 不增加针对特定枪械或魔法模组的硬编码分支。

---

## File Structure

### Build and metadata

- `settings.gradle`：仓库和项目名。
- `gradle.properties`：锁定版本、Mod 元数据和 JVM 设置。
- `build.gradle`：ModDevGradle、上游依赖、JUnit/Mockito、运行配置和资源展开。
- `gradlew`, `gradlew.bat`, `gradle/wrapper/**`：Gradle 8.13 wrapper。
- `src/main/resources/META-INF/neoforge.mods.toml`：模组及必需依赖声明。
- `src/main/resources/pack.mcmeta`：资源包元数据。
- `src/main/resources/critical_strike_kubejs_compat.mixins.json`：范围 Mixin 注册。
- `src/main/resources/kubejs.plugins.txt`：KubeJS 插件类入口。

### Main code

- `src/main/java/com/codex/criticalstrikekubejs/CriticalStrikeKubeJSCompat.java`：NeoForge 入口与常量。
- `src/main/java/com/codex/criticalstrikekubejs/crit/ForcedCriticalResult.java`：强制判定三态。
- `src/main/java/com/codex/criticalstrikekubejs/crit/CriticalParameters.java`：脚本可修改的受控概率、倍率、允许状态和强制结果。
- `src/main/java/com/codex/criticalstrikekubejs/crit/CriticalDecisionEngine.java`：决策优先级和随机入口。
- `src/main/java/com/codex/criticalstrikekubejs/crit/CriticalDamageMath.java`：安全乘算。
- `src/main/java/com/codex/criticalstrikekubejs/crit/CriticalAttemptContext.java`：一次可修改判定的实体与伤害上下文。
- `src/main/java/com/codex/criticalstrikekubejs/crit/CriticalResultContext.java`：成功暴击的只读快照。
- `src/main/java/com/codex/criticalstrikekubejs/source/PlayerDamageSourceResolver.java`：负责玩家与伤害类型 ID 解析。
- `src/main/java/com/codex/criticalstrikekubejs/scope/NativeCriticalScope.java`：线程局部可嵌套守卫。
- `src/main/java/com/codex/criticalstrikekubejs/mixin/PlayerAttackScopeMixin.java`：包裹 `Player.attack`。
- `src/main/java/com/codex/criticalstrikekubejs/mixin/AbstractArrowHitScopeMixin.java`：包裹 `AbstractArrow.onHitEntity`。
- `src/main/java/com/codex/criticalstrikekubejs/kube/CriticalStrikeCompatEvents.java`：KubeJS 事件组。
- `src/main/java/com/codex/criticalstrikekubejs/kube/ModifyCriticalKubeEvent.java`：可修改脚本事件。
- `src/main/java/com/codex/criticalstrikekubejs/kube/CriticalSuccessKubeEvent.java`：只读成功事件。
- `src/main/java/com/codex/criticalstrikekubejs/kube/CriticalStrikeCompatKubePlugin.java`：插件注册。
- `src/main/java/com/codex/criticalstrikekubejs/kube/CriticalScriptHooks.java`：桥接服务依赖的脚本钩子接口。
- `src/main/java/com/codex/criticalstrikekubejs/kube/KubeCriticalScriptHooks.java`：生产 KubeJS 事件发布器。
- `src/main/java/com/codex/criticalstrikekubejs/bridge/CriticalStrikeFacade.java`：上游 Critical Strike 边界接口。
- `src/main/java/com/codex/criticalstrikekubejs/bridge/UpstreamCriticalStrikeFacade.java`：读取上游 Attribute、判定、标记和特效。
- `src/main/java/com/codex/criticalstrikekubejs/bridge/CriticalBridgeRequest.java`：事件适配器交给服务的不可修改输入。
- `src/main/java/com/codex/criticalstrikekubejs/bridge/CriticalBridgeService.java`：纯流程编排。
- `src/main/java/com/codex/criticalstrikekubejs/bridge/CriticalDamageBridge.java`：NeoForge 事件适配器。

### Tests and documentation

- `src/test/java/com/codex/criticalstrikekubejs/ProjectMetadataTest.java`
- `src/test/java/com/codex/criticalstrikekubejs/crit/CriticalParametersTest.java`
- `src/test/java/com/codex/criticalstrikekubejs/crit/CriticalDecisionEngineTest.java`
- `src/test/java/com/codex/criticalstrikekubejs/crit/CriticalDamageMathTest.java`
- `src/test/java/com/codex/criticalstrikekubejs/scope/NativeCriticalScopeTest.java`
- `src/test/java/com/codex/criticalstrikekubejs/source/PlayerDamageSourceResolverTest.java`
- `src/test/java/com/codex/criticalstrikekubejs/kube/CriticalKubeEventsTest.java`
- `src/test/java/com/codex/criticalstrikekubejs/bridge/CriticalBridgeServiceTest.java`
- `src/test/java/com/codex/criticalstrikekubejs/bridge/BridgeEventOrderingTest.java`
- `src/test/java/com/codex/criticalstrikekubejs/TestFixtures.java`
- `src/gameTest/java/com/codex/criticalstrikekubejs/gametest/CriticalStrikeCompatGameTests.java`
- `src/gameTest/resources/kubejs/server_scripts/critical_strike_compat_test.js`
- `examples/kubejs/server_scripts/critical_strike_compat.js`
- `README.md`：中文安装、边界和 API 文档。

---

### Task 1: Reproducible NeoForge project and dependency smoke test

**Files:**
- Create: `critical-strike-kubejs-compat/settings.gradle`
- Create: `critical-strike-kubejs-compat/gradle.properties`
- Create: `critical-strike-kubejs-compat/build.gradle`
- Create: `critical-strike-kubejs-compat/gradlew`
- Create: `critical-strike-kubejs-compat/gradlew.bat`
- Create: `critical-strike-kubejs-compat/gradle/wrapper/gradle-wrapper.jar`
- Create: `critical-strike-kubejs-compat/gradle/wrapper/gradle-wrapper.properties`
- Create: `critical-strike-kubejs-compat/src/main/resources/META-INF/neoforge.mods.toml`
- Create: `critical-strike-kubejs-compat/src/main/resources/pack.mcmeta`
- Create: `critical-strike-kubejs-compat/src/main/java/com/codex/criticalstrikekubejs/CriticalStrikeKubeJSCompat.java`
- Test: `critical-strike-kubejs-compat/src/test/java/com/codex/criticalstrikekubejs/ProjectMetadataTest.java`

**Interfaces:**
- Consumes: none.
- Produces: `CriticalStrikeKubeJSCompat.MOD_ID`, Java 21 project, resolved Critical Strike/KubeJS compile and runtime classpaths, JUnit Platform test task.

- [ ] **Step 1: Create the Gradle skeleton and write the failing metadata test**

Use ModDevGradle `2.0.142` and Gradle `8.13`. The first test must read the source TOML and assert all required IDs and version ranges:

```java
@Test
void declaresExactRequiredDependencies() throws IOException {
    String toml = Files.readString(Path.of("src/main/resources/META-INF/neoforge.mods.toml"));
    assertTrue(toml.contains("modId = \"critical_strike_kubejs_compat\""));
    assertTrue(toml.contains("modId = \"critical_strike\""));
    assertTrue(toml.contains("modId = \"kubejs\""));
    assertTrue(toml.contains("versionRange = \"[1.0.4,2)\""));
    assertTrue(toml.contains("versionRange = \"[2101.7.2,)\""));
    assertEquals(21, Runtime.version().feature());
}
```

- [ ] **Step 2: Run the test and confirm the intended failure**

Run: `./gradlew test --tests com.codex.criticalstrikekubejs.ProjectMetadataTest --console=plain`

Expected: FAIL because `neoforge.mods.toml` and the main entry point do not yet exist.

- [ ] **Step 3: Add exact repositories and dependency coordinates**

The relevant `build.gradle` portion must be:

```groovy
plugins {
    id 'java-library'
    id 'net.neoforged.moddev' version '2.0.142'
}

repositories {
    mavenCentral()
    maven { url = 'https://maven.neoforged.net/releases' }
    maven { url = 'https://maven.latvian.dev/releases' }
    maven {
        url = 'https://api.modrinth.com/maven'
        content { includeGroup 'maven.modrinth' }
    }
}

dependencies {
    implementation 'maven.modrinth:critical-strike:1.0.4+1.21.1-neoforge'
    implementation 'dev.latvian.mods:kubejs-neoforge:2101.7.2-build.368'
    implementation 'dev.latvian.mods:rhino:2101.2.7-build.81'
    testImplementation platform('org.junit:junit-bom:5.11.4')
    testImplementation 'org.junit.jupiter:junit-jupiter'
    testImplementation 'org.mockito:mockito-core:5.15.2'
}
```

Configure `neoForge.version = '21.1.244'`, `neoForge.unitTest.enable()`, Java release 21, UTF-8, JUnit Platform, resource expansion, and client/server/game-test runs. Do not use dynamic dependency versions.

- [ ] **Step 4: Add the minimal entry point and complete metadata**

```java
@Mod(CriticalStrikeKubeJSCompat.MOD_ID)
public final class CriticalStrikeKubeJSCompat {
    public static final String MOD_ID = "critical_strike_kubejs_compat";

    public CriticalStrikeKubeJSCompat() {
    }
}
```

The TOML must declare required `minecraft`, `neoforge`, `critical_strike`, and `kubejs` dependencies, with `ordering = "AFTER"` for the two mod dependencies and `side = "BOTH"` for all entries.

- [ ] **Step 5: Run dependency and metadata verification**

Run:

```bash
./gradlew dependencies --configuration compileClasspath --console=plain
./gradlew test --tests com.codex.criticalstrikekubejs.ProjectMetadataTest --console=plain
./gradlew classes --console=plain
```

Expected: Critical Strike 1.0.4, KubeJS 2101.7.2-build.368 and Rhino 2101.2.7-build.81 resolve; metadata test and compilation PASS.

- [ ] **Step 6: Commit**

```bash
git add critical-strike-kubejs-compat
git commit -m "build: bootstrap Critical Strike KubeJS compat"
```

---

### Task 2: Validated critical parameters, decision engine and safe damage math

**Files:**
- Create: `critical-strike-kubejs-compat/src/main/java/com/codex/criticalstrikekubejs/crit/ForcedCriticalResult.java`
- Create: `critical-strike-kubejs-compat/src/main/java/com/codex/criticalstrikekubejs/crit/CriticalParameters.java`
- Create: `critical-strike-kubejs-compat/src/main/java/com/codex/criticalstrikekubejs/crit/CriticalDecisionEngine.java`
- Create: `critical-strike-kubejs-compat/src/main/java/com/codex/criticalstrikekubejs/crit/CriticalDamageMath.java`
- Test: `critical-strike-kubejs-compat/src/test/java/com/codex/criticalstrikekubejs/crit/CriticalParametersTest.java`
- Test: `critical-strike-kubejs-compat/src/test/java/com/codex/criticalstrikekubejs/crit/CriticalDecisionEngineTest.java`
- Test: `critical-strike-kubejs-compat/src/test/java/com/codex/criticalstrikekubejs/crit/CriticalDamageMathTest.java`

**Interfaces:**
- Consumes: none beyond Java standard library.
- Produces: `ForcedCriticalResult { DEFAULT, CRITICAL, NORMAL }`; `CriticalParameters(double chance, double damageMultiplier, boolean allowed, Consumer<String> errorSink)`; `CriticalDecisionEngine.isCritical(CriticalParameters, BooleanSupplier, DoubleSupplier)`; `CriticalDamageMath.multiply(float, double): Optional<Float>`.

- [ ] **Step 1: Write failing parameter validation tests**

```java
@Test
void invalidAssignmentsKeepLastValidValueAndReportField() {
    var errors = new ArrayList<String>();
    var values = new CriticalParameters(0.25D, 1.5D, true, errors::add);
    values.setChance(0.4D);
    values.setChance(Double.NaN);
    values.setDamageMultiplier(2.0D);
    values.setDamageMultiplier(Double.POSITIVE_INFINITY);

    assertEquals(0.4D, values.chance());
    assertEquals(2.0D, values.damageMultiplier());
    assertEquals(List.of("chance", "damageMultiplier"), errors);
}

@Test
void clampsFiniteValuesToPublicRanges() {
    var values = new CriticalParameters(0.05D, 1.5D, true, ignored -> {});
    values.setChance(4.0D);
    values.setDamageMultiplier(-3.0D);
    assertEquals(1.0D, values.chance());
    assertEquals(1.0D, values.damageMultiplier());
}
```

- [ ] **Step 2: Run validation tests and confirm they fail**

Run: `./gradlew test --tests '*CriticalParametersTest' --console=plain`

Expected: FAIL because `CriticalParameters` is missing.

- [ ] **Step 3: Implement the minimal parameter state**

Implement validated setters, `allow()`, `deny()`, `forceCritical()`, `forceNormal()`, `clearForcedResult()`, getters, and a `chanceOverridden()` flag that changes only after a valid explicit `setChance` call. Invalid non-finite assignments call `errorSink.accept("chance")` or `errorSink.accept("damageMultiplier")` and keep the last valid value.

- [ ] **Step 4: Write failing decision-priority and multiplication tests**

```java
@Test
void denyAndForcedNormalWinBeforeAnyRandomCall() {
    var values = new CriticalParameters(1.0D, 2.0D, false, ignored -> {});
    assertFalse(CriticalDecisionEngine.isCritical(values,
            () -> { throw new AssertionError("default roll called"); },
            () -> { throw new AssertionError("random called"); }));
    values.allow();
    values.forceNormal();
    assertFalse(CriticalDecisionEngine.isCritical(values,
            () -> { throw new AssertionError("default roll called"); },
            () -> { throw new AssertionError("random called"); }));
}

@Test
void explicitChanceUsesInjectedRandomButDefaultChanceUsesUpstreamRoll() {
    var values = new CriticalParameters(0.25D, 2.0D, true, ignored -> {});
    assertTrue(CriticalDecisionEngine.isCritical(values, () -> true, () -> 0.99D));
    values.setChance(0.25D);
    assertFalse(CriticalDecisionEngine.isCritical(values, () -> true, () -> 0.30D));
}

@Test
void rejectsNonFiniteDamageProduct() {
    assertTrue(CriticalDamageMath.multiply(4.0F, 2.5D).isPresent());
    assertTrue(CriticalDamageMath.multiply(Float.MAX_VALUE, Double.MAX_VALUE).isEmpty());
}
```

- [ ] **Step 5: Implement decision and safe multiplication**

Decision order must be: denied → forced normal → forced critical → upstream default roll when chance is untouched → injected random comparison when chance was overridden. `CriticalDamageMath.multiply` returns empty for negative/non-finite input or result and otherwise returns the finite float product.

- [ ] **Step 6: Run focused and complete unit tests**

Run:

```bash
./gradlew test --tests 'com.codex.criticalstrikekubejs.crit.*' --console=plain
./gradlew test --console=plain
```

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add critical-strike-kubejs-compat/src/main/java/com/codex/criticalstrikekubejs/crit critical-strike-kubejs-compat/src/test/java/com/codex/criticalstrikekubejs/crit
git commit -m "feat: add validated critical decision model"
```

---

### Task 3: Native Critical Strike scope guard and Mixin wiring

**Files:**
- Create: `critical-strike-kubejs-compat/src/main/java/com/codex/criticalstrikekubejs/scope/NativeCriticalScope.java`
- Create: `critical-strike-kubejs-compat/src/main/java/com/codex/criticalstrikekubejs/mixin/PlayerAttackScopeMixin.java`
- Create: `critical-strike-kubejs-compat/src/main/java/com/codex/criticalstrikekubejs/mixin/AbstractArrowHitScopeMixin.java`
- Create: `critical-strike-kubejs-compat/src/main/resources/critical_strike_kubejs_compat.mixins.json`
- Modify: `critical-strike-kubejs-compat/src/main/resources/META-INF/neoforge.mods.toml`
- Test: `critical-strike-kubejs-compat/src/test/java/com/codex/criticalstrikekubejs/scope/NativeCriticalScopeTest.java`

**Interfaces:**
- Consumes: MixinExtras `@WrapMethod` and `Operation` available on the NeoForge classpath.
- Produces: `NativeCriticalScope.enter(): NativeCriticalScope.Token`; `NativeCriticalScope.isActive(): boolean`; idempotent `Token.close()`.

- [ ] **Step 1: Write the failing nesting and cleanup tests**

```java
@Test
void nestedTokensKeepScopeActiveUntilLastClose() {
    assertFalse(NativeCriticalScope.isActive());
    try (var outer = NativeCriticalScope.enter()) {
        assertTrue(NativeCriticalScope.isActive());
        try (var inner = NativeCriticalScope.enter()) {
            assertTrue(NativeCriticalScope.isActive());
        }
        assertTrue(NativeCriticalScope.isActive());
    }
    assertFalse(NativeCriticalScope.isActive());
}

@Test
void tokenCloseIsIdempotentAndRemovesThreadLocal() {
    var token = NativeCriticalScope.enter();
    token.close();
    token.close();
    assertFalse(NativeCriticalScope.isActive());
}
```

- [ ] **Step 2: Run the test and confirm failure**

Run: `./gradlew test --tests '*NativeCriticalScopeTest' --console=plain`

Expected: FAIL because the scope class is missing.

- [ ] **Step 3: Implement a depth-counted ThreadLocal scope**

Use `ThreadLocal<Integer>` with `enter()` increment, `close()` decrement, and `ThreadLocal.remove()` at zero. Reject underflow with `IllegalStateException`; make each token close only once.

- [ ] **Step 4: Add two try/finally range Mixins**

Player wrapper shape:

```java
@Mixin(Player.class)
abstract class PlayerAttackScopeMixin {
    @WrapMethod(method = "attack")
    private void criticalStrikeCompat$guardAttack(Entity target, Operation<Void> original) {
        try (var ignored = NativeCriticalScope.enter()) {
            original.call(target);
        }
    }
}
```

Arrow wrapper targets `AbstractArrow.onHitEntity(EntityHitResult)` with the same scope pattern. Register both Mixins in `critical_strike_kubejs_compat.mixins.json` with `required: true`, Java compatibility 21 and default injector requirement 1. Add the Mixin config to TOML.

- [ ] **Step 5: Compile Mixins and run scope tests**

Run:

```bash
./gradlew test --tests '*NativeCriticalScopeTest' --console=plain
./gradlew classes --console=plain
```

Expected: PASS with both Mixin target signatures resolved.

- [ ] **Step 6: Commit**

```bash
git add critical-strike-kubejs-compat/src/main/java/com/codex/criticalstrikekubejs/scope critical-strike-kubejs-compat/src/main/java/com/codex/criticalstrikekubejs/mixin critical-strike-kubejs-compat/src/main/resources
git commit -m "feat: guard Critical Strike native damage paths"
```

---

### Task 4: Player damage-source and registry-ID resolution

**Files:**
- Create: `critical-strike-kubejs-compat/src/main/java/com/codex/criticalstrikekubejs/source/PlayerDamageSourceResolver.java`
- Test: `critical-strike-kubejs-compat/src/test/java/com/codex/criticalstrikekubejs/source/PlayerDamageSourceResolverTest.java`

**Interfaces:**
- Consumes: Minecraft `DamageSource`, `Player`, `Projectile`, `ResourceLocation`.
- Produces: `PlayerDamageSourceResolver.resolvePlayer(DamageSource): Optional<Player>` and `PlayerDamageSourceResolver.sourceType(DamageSource): String`.

- [ ] **Step 1: Write failing Mockito resolver tests**

```java
@Test
void prefersResponsiblePlayerAndFallsBackToProjectileOwner() {
    var source = mock(DamageSource.class);
    var player = mock(Player.class);
    when(source.getEntity()).thenReturn(player);
    assertSame(player, PlayerDamageSourceResolver.resolvePlayer(source).orElseThrow());

    var projectileSource = mock(DamageSource.class);
    var projectile = mock(Projectile.class);
    when(projectileSource.getDirectEntity()).thenReturn(projectile);
    when(projectile.getOwner()).thenReturn(player);
    assertSame(player, PlayerDamageSourceResolver.resolvePlayer(projectileSource).orElseThrow());
}

@Test
void refusesNonPlayerAndMissingAttribution() {
    var source = mock(DamageSource.class);
    when(source.getEntity()).thenReturn(mock(LivingEntity.class));
    assertTrue(PlayerDamageSourceResolver.resolvePlayer(source).isEmpty());
}
```

- [ ] **Step 2: Run resolver tests and confirm failure**

Run: `./gradlew test --tests '*PlayerDamageSourceResolverTest' --console=plain`

Expected: FAIL because the resolver is missing.

- [ ] **Step 3: Implement exact resolution order and source type**

Use `source.getEntity()` first. Only if that is not a player, inspect `source.getDirectEntity() instanceof Projectile` and its owner. `sourceType` must read `source.typeHolder().unwrapKey().map(key -> key.location().toString())`; use `"<unregistered:" + source.getMsgId() + ">"` only when the holder has no registry key.

- [ ] **Step 4: Add source-type tests and run the suite**

Mock a holder key for `mymod:arcane_bolt` and assert the exact string. Also assert the explicit `<unregistered:magic>` fallback. Run:

```bash
./gradlew test --tests '*PlayerDamageSourceResolverTest' --console=plain
./gradlew test --console=plain
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add critical-strike-kubejs-compat/src/main/java/com/codex/criticalstrikekubejs/source critical-strike-kubejs-compat/src/test/java/com/codex/criticalstrikekubejs/source
git commit -m "feat: resolve player-owned damage sources"
```

---

### Task 5: KubeJS event group, mutable event and success event

**Files:**
- Create: `critical-strike-kubejs-compat/src/main/java/com/codex/criticalstrikekubejs/crit/CriticalAttemptContext.java`
- Create: `critical-strike-kubejs-compat/src/main/java/com/codex/criticalstrikekubejs/crit/CriticalResultContext.java`
- Create: `critical-strike-kubejs-compat/src/main/java/com/codex/criticalstrikekubejs/kube/CriticalStrikeCompatEvents.java`
- Create: `critical-strike-kubejs-compat/src/main/java/com/codex/criticalstrikekubejs/kube/ModifyCriticalKubeEvent.java`
- Create: `critical-strike-kubejs-compat/src/main/java/com/codex/criticalstrikekubejs/kube/CriticalSuccessKubeEvent.java`
- Create: `critical-strike-kubejs-compat/src/main/java/com/codex/criticalstrikekubejs/kube/CriticalStrikeCompatKubePlugin.java`
- Create: `critical-strike-kubejs-compat/src/main/java/com/codex/criticalstrikekubejs/kube/CriticalScriptHooks.java`
- Create: `critical-strike-kubejs-compat/src/main/java/com/codex/criticalstrikekubejs/kube/KubeCriticalScriptHooks.java`
- Create: `critical-strike-kubejs-compat/src/main/resources/kubejs.plugins.txt`
- Create: `critical-strike-kubejs-compat/src/test/java/com/codex/criticalstrikekubejs/TestFixtures.java`
- Test: `critical-strike-kubejs-compat/src/test/java/com/codex/criticalstrikekubejs/kube/CriticalKubeEventsTest.java`

**Interfaces:**
- Consumes: `CriticalParameters`, player/target/source/direct entity and `sourceType` from Tasks 2 and 4.
- Produces: `CriticalScriptHooks.modify(CriticalAttemptContext)` and `CriticalScriptHooks.critical(CriticalResultContext)`; event group `CriticalStrikeCompatEvents.modify` and `.critical`.

- [ ] **Step 1: Write failing event delegation and read-only result tests**

```java
@Test
void modifyEventDelegatesScriptMutationsToParameters() {
    var context = TestFixtures.attemptContext(8.0F);
    var event = new ModifyCriticalKubeEvent(context);
    event.setChance(0.75D);
    event.setDamageMultiplier(3.0D);
    event.forceCritical();
    assertEquals(0.75D, context.parameters().chance());
    assertEquals(3.0D, context.parameters().damageMultiplier());
    assertEquals(ForcedCriticalResult.CRITICAL, context.parameters().forcedResult());
}

@Test
void pluginFileNamesTheExactPluginClass() throws IOException {
    String plugin = Files.readString(Path.of("src/main/resources/kubejs.plugins.txt")).trim();
    assertEquals("com.codex.criticalstrikekubejs.kube.CriticalStrikeCompatKubePlugin", plugin);
}
```

- [ ] **Step 2: Run event tests and confirm failure**

Run: `./gradlew test --tests '*CriticalKubeEventsTest' --console=plain`

Expected: FAIL because the contexts, events and plugin file are absent.

- [ ] **Step 3: Implement context types and script-visible wrappers**

`CriticalAttemptContext` is a record with `Player attacker`, `LivingEntity target`, `DamageSource source`, nullable `Entity directEntity`, `String sourceType`, `float damage`, and `CriticalParameters parameters`. `CriticalResultContext` is a record with the same identity fields plus `float baseDamage`, `float criticalDamage`, `double chance`, `double damageMultiplier`, and `boolean forced`.

`TestFixtures.attemptContext(float damage)` uses Mockito to create one player, target and source, assigns `mymod:test` as `sourceType`, and constructs parameters with chance `0.25`, multiplier `1.5`, allowed `true`, and a no-op error sink. Later tests must reuse this helper instead of inventing a conflicting constructor shape.

`ModifyCriticalKubeEvent implements KubeEvent` exposes JavaBean getters plus `setChance`, `setDamageMultiplier`, `allow`, `deny`, `forceCritical`, `forceNormal`, and `clearForcedResult`. `CriticalSuccessKubeEvent implements KubeEvent` exposes getters only; do not expose setters or its mutable parameter object.

- [ ] **Step 4: Register the event group and KubeJS plugin**

```java
public interface CriticalStrikeCompatEvents {
    EventGroup GROUP = EventGroup.of("CriticalStrikeCompatEvents");
    EventHandler MODIFY = GROUP.common("modify", () -> ModifyCriticalKubeEvent.class);
    EventHandler CRITICAL = GROUP.common("critical", () -> CriticalSuccessKubeEvent.class);
}

public final class CriticalStrikeCompatKubePlugin implements KubeJSPlugin {
    @Override
    public void registerEvents(EventGroupRegistry registry) {
        registry.register(CriticalStrikeCompatEvents.GROUP);
    }

    @Override
    public void registerClasses(ClassFilter filter) {
        filter.allow("com.codex.criticalstrikekubejs.kube");
    }
}
```

`KubeCriticalScriptHooks` must check `hasListeners()` before allocating wrappers and post with `ScriptType.SERVER`. The plugin text file contains only the fully qualified plugin class plus a trailing newline.

- [ ] **Step 5: Run tests and compile against real KubeJS**

Run:

```bash
./gradlew test --tests '*CriticalKubeEventsTest' --console=plain
./gradlew classes --console=plain
```

Expected: PASS; KubeJS API signatures compile without reflection.

- [ ] **Step 6: Commit**

```bash
git add critical-strike-kubejs-compat/src/main/java/com/codex/criticalstrikekubejs/crit critical-strike-kubejs-compat/src/main/java/com/codex/criticalstrikekubejs/kube critical-strike-kubejs-compat/src/main/resources/kubejs.plugins.txt critical-strike-kubejs-compat/src/test/java/com/codex/criticalstrikekubejs/kube
git commit -m "feat: expose KubeJS critical customization events"
```

---

### Task 6: Critical Strike facade and universal damage bridge

**Files:**
- Create: `critical-strike-kubejs-compat/src/main/java/com/codex/criticalstrikekubejs/bridge/CriticalStrikeFacade.java`
- Create: `critical-strike-kubejs-compat/src/main/java/com/codex/criticalstrikekubejs/bridge/UpstreamCriticalStrikeFacade.java`
- Create: `critical-strike-kubejs-compat/src/main/java/com/codex/criticalstrikekubejs/bridge/CriticalBridgeRequest.java`
- Create: `critical-strike-kubejs-compat/src/main/java/com/codex/criticalstrikekubejs/bridge/CriticalBridgeService.java`
- Create: `critical-strike-kubejs-compat/src/main/java/com/codex/criticalstrikekubejs/bridge/CriticalDamageBridge.java`
- Test: `critical-strike-kubejs-compat/src/test/java/com/codex/criticalstrikekubejs/bridge/CriticalBridgeServiceTest.java`
- Test: `critical-strike-kubejs-compat/src/test/java/com/codex/criticalstrikekubejs/bridge/BridgeEventOrderingTest.java`

**Interfaces:**
- Consumes: Tasks 2–5, `CriticalStriker`, `CriticalDamageSource`, `CritLogic`, `CriticalStrikeMod.config`, and NeoForge `LivingDamageEvent.Pre`.
- Produces: `CriticalBridgeRequest(Player, LivingEntity, DamageSource, @Nullable Entity, String, float)`; `CriticalBridgeService.process(CriticalBridgeRequest): Optional<CriticalResultContext>`; `CriticalBridgeService.commit(CriticalResultContext, Consumer<Float>)`; lowest-priority server event listener that writes the finite critical damage back into `DamageContainer` exactly once.

- [ ] **Step 1: Write failing bridge-service behavior tests with fakes**

Cover all branches explicitly:

```java
@Test
void appliesScriptModifiedMultiplierAndPostsOneSuccess() {
    var facade = mock(CriticalStrikeFacade.class);
    when(facade.chance(any())).thenReturn(0.20D);
    when(facade.damageMultiplier(any())).thenReturn(1.5D);
    var successes = new ArrayList<CriticalResultContext>();
    var hooks = new CriticalScriptHooks() {
        public void modify(CriticalAttemptContext ctx) {
            ctx.parameters().setChance(1.0D);
            ctx.parameters().setDamageMultiplier(2.25D);
        }
        public void critical(CriticalResultContext result) {
            successes.add(result);
        }
    };
    var service = new CriticalBridgeService(facade, hooks, () -> 0.5D, ignored -> {});

    var result = service.process(TestFixtures.bridgeRequest(8.0F)).orElseThrow();
    service.commit(result, ignored -> {});

    assertEquals(18.0F, result.criticalDamage());
    verify(facade).markCritical(any(), eq(2.25D));
    assertEquals(1, successes.size());
}

@Test
void deniedForcedNormalAndFailedRollDoNotMarkOrPostSuccess() {
    var facade = mock(CriticalStrikeFacade.class);
    when(facade.chance(any())).thenReturn(0.20D);
    when(facade.damageMultiplier(any())).thenReturn(1.5D);
    var hooks = mock(CriticalScriptHooks.class);
    doAnswer(invocation -> {
        CriticalAttemptContext context = invocation.getArgument(0);
        context.parameters().deny();
        return null;
    }).when(hooks).modify(any());
    var service = new CriticalBridgeService(facade, hooks, () -> 0.0D, ignored -> {});

    assertTrue(service.process(TestFixtures.bridgeRequest(8.0F)).isEmpty());
    verify(facade, never()).markCritical(any(), anyDouble());
    verify(hooks, never()).critical(any());
}
```

Add separate concrete tests with the same Mockito arrangement for `forceNormal()`, force-critical, untouched probability using the facade default roll, overridden probability using the injected random source, default self-denial followed by script `allow()`, invalid damage product, and one success notification per committed result.

- [ ] **Step 2: Run service tests and confirm failure**

Run: `./gradlew test --tests '*CriticalBridgeServiceTest' --console=plain`

Expected: FAIL because the facade and service are missing.

- [ ] **Step 3: Implement the facade boundary**

```java
public interface CriticalStrikeFacade {
    double chance(Player player);
    double damageMultiplier(Player player);
    boolean defaultRoll(Player player);
    void markCritical(DamageSource source, double multiplier);
    void playEffects(LivingEntity target);
}
```

`UpstreamCriticalStrikeFacade` casts the player to `CriticalStriker`, calls `rng_criticalChance`, `rng_criticalDamageMultiplier`, and `rng_shouldDealCriticalHit`, casts the source to `CriticalDamageSource` to set the multiplier, and calls `CritLogic.playFxAt(target, CriticalStrikeMod.config.value.sound_ranged_crit_volume)`. Fail startup with a clear `IllegalStateException` if an expected upstream interface is absent; do not silently skip a partially loaded dependency.

`CriticalBridgeRequest` is the six-field immutable record declared in the Interfaces block. Extend `TestFixtures` with `bridgeRequest(float damage)`, reusing the same mocked entities and source shape as `attemptContext` but omitting `CriticalParameters`, because the service owns their creation.

- [ ] **Step 4: Implement the service in the approved order**

1. Create `CriticalParameters` from facade values and `allowed = attacker != target`.
2. Publish `hooks.modify(context)`.
3. Call `CriticalDecisionEngine` with `facade.defaultRoll(player)` and injected random.
4. Call `CriticalDamageMath.multiply` on the KubeJS-adjusted current damage.
5. Create and return the immutable result without mutating the NeoForge damage container or publishing success.
6. `commit(result, damageWriter)` first invokes `damageWriter.accept(criticalDamage)`, then marks the `DamageSource`, plays Critical Strike effects, and finally calls `hooks.critical(result)` exactly once.

This split makes the event-order guarantee testable: the success script cannot observe the result before the adapter has written the critical damage.

- [ ] **Step 5: Write failing adapter/order tests**

Mock `LivingDamageEvent.Pre` and its damage container, and invoke a package-private `handle(event, service)` overload so the production singleton does not leak into tests. Assert that the adapter:

- returns immediately when `NativeCriticalScope.isActive()`;
- returns on client side;
- returns when no player can be resolved;
- passes `container.getNewDamage()` rather than original damage to the service;
- calls `container.setNewDamage(result.criticalDamage())` once before source marking, effects and the success event;
- is annotated with `@SubscribeEvent(priority = EventPriority.LOWEST)`.

The reflection assertion for priority must inspect the listener method annotation and compare the exact enum constant.

- [ ] **Step 6: Implement the NeoForge adapter**

```java
@EventBusSubscriber(modid = CriticalStrikeKubeJSCompat.MOD_ID)
public final class CriticalDamageBridge {
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void beforeLivingDamage(LivingDamageEvent.Pre event) {
        handle(event, SERVICE);
    }

    static void handle(LivingDamageEvent.Pre event, CriticalBridgeService service) {
        var target = event.getEntity();
        if (target.level().isClientSide() || NativeCriticalScope.isActive()) {
            return;
        }
        var container = event.getContainer();
        var source = container.getSource();
        var attacker = PlayerDamageSourceResolver.resolvePlayer(source);
        if (attacker.isEmpty()) {
            return;
        }
        var request = new CriticalBridgeRequest(attacker.get(), target, source,
                source.getDirectEntity(), PlayerDamageSourceResolver.sourceType(source),
                container.getNewDamage());
        service.process(request).ifPresent(result ->
                service.commit(result, container::setNewDamage));
    }
}
```

The event method must not cancel the event and must not replace its `DamageSource`. Log invalid script field names with attacker UUID and `sourceType`, without logging every normal skipped environmental hit.

- [ ] **Step 7: Run bridge tests and compile all production integrations**

Run:

```bash
./gradlew test --tests 'com.codex.criticalstrikekubejs.bridge.*' --console=plain
./gradlew test --console=plain
./gradlew classes --console=plain
```

Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add critical-strike-kubejs-compat/src/main/java/com/codex/criticalstrikekubejs/bridge critical-strike-kubejs-compat/src/test/java/com/codex/criticalstrikekubejs/bridge
git commit -m "feat: bridge player-owned KubeJS damage into critical hits"
```

---

### Task 7: Real-mod GameTests and server startup verification

**Files:**
- Modify: `critical-strike-kubejs-compat/build.gradle`
- Create: `critical-strike-kubejs-compat/src/gameTest/java/com/codex/criticalstrikekubejs/gametest/CriticalStrikeCompatGameTests.java`
- Create: `critical-strike-kubejs-compat/src/gameTest/resources/kubejs/server_scripts/critical_strike_compat_test.js`
- Create: `critical-strike-kubejs-compat/src/gameTest/resources/META-INF/neoforge.mods.toml`

**Interfaces:**
- Consumes: the complete bridge, real Critical Strike Attribute registrations, real KubeJS plugin loading and NeoForge GameTest.
- Produces: automated proof that real player-owned custom damage crits, original paths do not double-roll, script customization works, and the required-mod server starts.

- [ ] **Step 1: Add a dedicated GameTest source set and failing smoke test**

Configure a `gameTest` source set, add it to the test mod run beside `main`, and copy `src/gameTest/resources/kubejs` into its isolated game directory before launch. First test:

```java
@GameTest(template = "empty")
public static void attributesAndKubePluginAreLoaded(GameTestHelper helper) {
    var chance = BuiltInRegistries.ATTRIBUTE.get(ResourceLocation.parse("critical_strike:chance"));
    var damage = BuiltInRegistries.ATTRIBUTE.get(ResourceLocation.parse("critical_strike:damage"));
    helper.assertTrue(chance != null, "critical chance attribute missing");
    helper.assertTrue(damage != null, "critical damage attribute missing");
    helper.assertTrue(CriticalStrikeCompatEvents.MODIFY != null, "KubeJS event group missing");
    helper.succeed();
}
```

- [ ] **Step 2: Run the GameTest server and confirm the initial integration failure**

Run: `./gradlew runGameTestServer --console=plain`

Expected: FAIL until the source set, test namespace and runtime mods/scripts are wired correctly; record the exact first failure in the task notes before fixing it.

- [ ] **Step 3: Add deterministic real-damage tests**

Create server players and unarmored targets with high health. Set the actual Critical Strike attributes so chance is guaranteed and compute the expected multiplier through the upstream interface. Test:

- direct `target.hurt(level.damageSources().playerAttack(player), baseDamage)` outside `Player.attack` is treated as Hitscan-like custom damage and multiplied once;
- `player.attack(target)` is multiplied exactly once by Critical Strike, not again by the bridge;
- an `AbstractArrow` owned by the player is multiplied exactly once;
- generic, fall, fire and poison damage without a player are unchanged;
- player-attributed self-damage is unchanged by default.

Use target health delta and the source multiplier marker for assertions. Reset entities between cases so armor, invulnerability frames and previous damage do not contaminate results.

- [ ] **Step 4: Add and exercise the real KubeJS test script**

```js
EntityEvents.beforeHurt(event => {
  if (event.entity.persistentData.compatOrderTest) {
    event.damage = 8
  }
})

CriticalStrikeCompatEvents.modify(event => {
  if (event.sourceType == 'minecraft:magic') {
    event.allow()
    event.chance = 1.0
    event.damageMultiplier = 2.25
  }
  if (event.sourceType == 'minecraft:on_fire') {
    event.forceNormal()
  }
})

CriticalStrikeCompatEvents.critical(event => {
  event.attacker.persistentData.compatCriticalCount =
    (event.attacker.persistentData.compatCriticalCount || 0) + 1
})
```

The Java GameTest creates a player-attributed magic `DamageSource`, asserts the 2.25x health delta, and asserts `compatCriticalCount == 1`. A separate `EntityEvents.beforeHurt` listener changes base damage to `8`; the expected result must be `18`, proving normal KubeJS modification precedes the bridge.

- [ ] **Step 5: Run the full GameTest and startup matrix**

Run:

```bash
./gradlew runGameTestServer --console=plain
./gradlew runServer --args "--nogui" --console=plain
```

Expected: all GameTests PASS; the server log reaches normal KubeJS server-script load and NeoForge startup completion with no missing plugin, Mixin target, Critical Strike interface or dependency errors. Stop the smoke server after startup completion rather than waiting indefinitely.

- [ ] **Step 6: Run all unit tests again**

Run: `./gradlew test --console=plain`

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add critical-strike-kubejs-compat/build.gradle critical-strike-kubejs-compat/src/gameTest
git commit -m "test: verify real KubeJS and Critical Strike integration"
```

---

### Task 8: Chinese documentation, examples and release artifact

**Files:**
- Create: `critical-strike-kubejs-compat/README.md`
- Create: `critical-strike-kubejs-compat/examples/kubejs/server_scripts/critical_strike_compat.js`
- Modify: `critical-strike-kubejs-compat/build.gradle`
- Test: `critical-strike-kubejs-compat/src/test/java/com/codex/criticalstrikekubejs/ProjectMetadataTest.java`

**Interfaces:**
- Consumes: final public event fields and methods from Task 5.
- Produces: installable JAR, exact API documentation, copyable server script, and reproducible verification commands.

- [ ] **Step 1: Extend the documentation test before writing docs**

```java
@Test
void readmeDocumentsEveryPublicScriptControl() throws IOException {
    String readme = Files.readString(Path.of("README.md"));
    for (String required : List.of(
            "CriticalStrikeCompatEvents.modify",
            "CriticalStrikeCompatEvents.critical",
            "sourceType", "chance", "damageMultiplier",
            "allow()", "deny()", "forceCritical()", "forceNormal()")) {
        assertTrue(readme.contains(required), () -> "missing documentation: " + required);
    }
}
```

- [ ] **Step 2: Run the documentation test and confirm failure**

Run: `./gradlew test --tests '*ProjectMetadataTest' --console=plain`

Expected: FAIL because `README.md` is absent.

- [ ] **Step 3: Write the Chinese README and complete example**

Document installation, exact required versions, automatic behavior, player-attribution rule, native duplicate prevention, event ordering, all fields/methods, self-damage default, finite-number validation, unsupported damage paths, build commands and troubleshooting. The example script must include:

- source allow/deny filtering;
- chance and multiplier override;
- forced critical and forced normal examples;
- success logging;
- a warning that `critical` means the pre-damage roll succeeded, not guaranteed final health loss.

- [ ] **Step 4: Configure the release JAR name and contents**

Set the artifact name to `critical-strike-kubejs-compat-1.0.0-mc1.21.1.jar`. Ensure the JAR contains `META-INF/neoforge.mods.toml`, Mixin config, `kubejs.plugins.txt` and compiled classes, but does not bundle Critical Strike, KubeJS, Rhino, test scripts, logs or downloaded dependency JARs.

- [ ] **Step 5: Run final verification**

Run:

```bash
./gradlew clean test classes runGameTestServer build --console=plain
git diff --check
```

Inspect:

```bash
jar tf build/libs/critical-strike-kubejs-compat-1.0.0-mc1.21.1.jar
```

Expected: every command succeeds; the JAR contains required metadata/plugin/Mixin entries exactly once and excludes dependency classes.

- [ ] **Step 6: Commit the release-ready project**

```bash
git add critical-strike-kubejs-compat
git commit -m "docs: add KubeJS compat guide and release artifact"
```

---

## Final Review Checklist

- [ ] Every design-spec acceptance criterion maps to Tasks 2–8.
- [ ] `CriticalStrikeCompatEvents.modify` runs after ordinary KubeJS `beforeHurt` and before damage write-back.
- [ ] `CriticalStrikeCompatEvents.critical` fires once only after a finite multiplied value is accepted.
- [ ] Player attack and `AbstractArrow` native ranges always bypass the bridge, including native failure/disabled paths.
- [ ] Custom player-owned damage outside those ranges uses Critical Strike Attribute values and effects.
- [ ] Missing player attribution, client execution and environmental damage are silent no-ops.
- [ ] Self-damage begins denied but scripts can call `allow()`.
- [ ] Invalid script values keep the last valid value and cannot create non-finite/negative damage.
- [ ] Dependencies are pinned; the final JAR does not shade or redistribute them.
- [ ] Unit tests, real-mod GameTests, server startup, compilation and release build all pass.
