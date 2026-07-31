# Super Reforge Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 Minecraft 1.21.1 / NeoForge 21.1.244 构建并打包一个由 datapack 与可选 KubeJS 驱动、可兼容可选 Curios 的 3D 重铸台模组。

**Architecture:** 服务端用 reload listener 建立不可变 `DefinitionSnapshot`，物品只保存词条资源 ID、随机种子和 schema 版本；每次读取时动态解析最新定义。重铸采用服务端原子事务和持久化 `pendingResult`，客户端只显示菜单、概率和约 20 tick 动画。Curios 与 KubeJS 均放在隔离兼容层中，缺少依赖时不会加载对应类。

**Tech Stack:** Java 21、Minecraft 1.21.1、NeoForge 21.1.244、ModDevGradle、Mojang Codec、JUnit 5、GameTest、可选 Curios 9.x、可选 KubeJS 2101。

## Global Constraints

- 模组名为 `Super Reforge`，mod ID 为 `superreforge`，作者为 `MUTUO`，Java 包为 `com.mutuo.superreforge`。
- 代码许可证为 `LGPL-3.0-or-later`；原创资源采用相同项目许可。若最终直接派生 BountifulBaubles 图像，单独标注 `CC BY-NC-SA 4.0`、署名与非商业限制。
- 不注册任何自定义 Attribute；所有效果按注册表 ID 解析原版或其他模组 Attribute。
- Curios 与 KubeJS 是可选依赖；缺少它们时核心武器重铸和 datapack 功能必须正常运行。
- KubeJS 内容定义覆盖 datapack 同 ID 定义；全局 TOML 设置不被内容脚本覆盖。
- 目标槽只能放入数量恰好为 1 的物品；一次只消耗一种媒介，数量和经验由媒介、配置和最高优先级阶段共同决定。
- 所有概率输入是非负有限权重，通过 `weight / sum(weight)` 自动归一化；空候选等级在归一化前移除。
- 所有生产 Java 文件包含文件级职责注释；公开 API、边界校验、事务与兼容逻辑包含有意义的中文注释。

## File Map

- `build.gradle`、`settings.gradle`、`gradle.properties`、`gradle/wrapper/*`：固定工具链、NeoForge 版本、可选依赖仓库和可复现构建。
- `src/main/java/com/mutuo/superreforge/SuperReforge.java`：模组入口，只负责注册和生命周期编排。
- `config/*`：全局 TOML 设置及经验模式。
- `registry/*`：方块、物品、菜单、方块实体、数据组件、粒子和音效注册。
- `definition/*`：等级、类型、词条、媒介的数据结构、Codec、校验、合并与 reload 快照。
- `reforge/*`：类型解析、候选合并、权重抽取、确定性数值、成本和原子事务。
- `item/*`：物品数据组件、动态名称、原版 Attribute 注入和无效词条策略。
- `block/*`：熔核锻台方块、方块实体、菜单、约 20 tick 的持久化待揭晓状态。
- `client/*`：锻造日志 GUI、方块实体渲染、锤击/熔核/火花动画和客户端同步。
- `compat/curios/*`、`compat/kubejs/*`：隔离的可选依赖集成。
- `progress/*`：全服持久阶段、最高优先级选择和成本修正。
- `src/main/resources/data/superreforge/*`：配方、标签、8 等级、7 类型、3 媒介和示例词条。
- `src/test/java/*`：纯逻辑 JUnit 测试；`gametest/*`：菜单、事务、持久化和无可选模组启动验证。
- `README.md`、`docs/CONFIGURATION.md`、`docs/FILE_REFERENCE.md`：安装、配置、每个文件的用途和影响。

---

### Task 1: 可复现 NeoForge 工程与启动冒烟测试

**Files:**
- Create: `settings.gradle`, `build.gradle`, `gradle.properties`, `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.properties`, `gradle/wrapper/gradle-wrapper.jar`
- Create: `src/main/java/com/mutuo/superreforge/SuperReforge.java`
- Create: `src/main/resources/META-INF/neoforge.mods.toml`, `src/main/resources/pack.mcmeta`
- Create: `LICENSE`, `README.md`
- Test: `src/test/java/com/mutuo/superreforge/BootstrapMetadataTest.java`

**Interfaces:**
- Produces: `SuperReforge.MOD_ID`, NeoForge event bus entry point and Java 21 test/build tasks.

- [ ] **Step 1: 写失败测试**，读取生成后的 `neoforge.mods.toml`，断言 modId、displayName、authors、Minecraft/NeoForge 版本范围准确，并断言 Java 运行版本至少为 21。
- [ ] **Step 2: 运行** `./gradlew test --tests '*BootstrapMetadataTest'`，预期因工程或元数据尚不存在而失败。
- [ ] **Step 3: 建立 ModDevGradle 工程**，使用 `net.neoforged.moddev`、`neoForge.version = 21.1.244`、JUnit Platform 和 UTF-8；主类仅注册后续模块入口，注释说明它不承载业务逻辑。
- [ ] **Step 4: 运行** `./gradlew test` 与 `./gradlew classes`，预期通过。
- [ ] **Step 5: 提交** `build: bootstrap NeoForge 1.21.1 project`。

### Task 2: 全局配置、数据组件与基础值对象

**Files:**
- Create: `config/SuperReforgeConfig.java`, `config/ExperienceMode.java`, `config/MissingDefinitionPolicy.java`
- Create: `item/ReforgeData.java`, `item/ReforgeDataComponent.java`
- Create: `registry/ModDataComponents.java`
- Test: `config/SuperReforgeConfigTest.java`, `item/ReforgeDataCodecTest.java`

**Interfaces:**
- Produces: `ReforgeData(ResourceLocation modifierId, long seed, int schemaVersion)`、持久化 `Codec`、网络 `StreamCodec`、`SuperReforgeConfig.SERVER`。

- [ ] **Step 1: 写失败测试**，覆盖 `ReforgeData` JSON/网络往返、非法 schema 拒绝，以及配置默认值：经验开启、`LEVELS`、自动初始词条关闭、动画 20 tick、动态失效策略 `REMOVE`。
- [ ] **Step 2: 运行目标测试**，预期类型不存在而失败。
- [ ] **Step 3: 实现枚举、配置和数据组件**；校验动画 tick 至少为 1，所有中文注释解释配置影响。
- [ ] **Step 4: 运行目标测试和 `classes`**，预期通过。
- [ ] **Step 5: 提交** `feat: add global config and reforge data component`。

### Task 3: Datapack 定义模型、Codec 与严格校验

**Files:**
- Create: `definition/LevelDefinition.java`, `ItemTypeDefinition.java`, `ItemSelector.java`
- Create: `definition/ModifierDefinition.java`, `AttributeEffectDefinition.java`, `ValueDefinition.java`, `SlotTarget.java`
- Create: `definition/CatalystDefinition.java`, `WeightedLevel.java`
- Create: `definition/DefinitionValidationException.java`, `DefinitionValidator.java`
- Test: `definition/DefinitionCodecTest.java`, `definition/DefinitionValidatorTest.java`

**Interfaces:**
- Produces: 四类定义的 `Codec`；`DefinitionValidator.validate(DefinitionSnapshot): ValidationReport`。

- [ ] **Step 1: 写失败参数化测试**，加载规格中的等级、类型、武器词条、Curio 词条和媒介 JSON；再覆盖负数/NaN/无穷权重、未知等级、未知 Attribute、非法范围、allow/deny 同时命中与重复 ID。
- [ ] **Step 2: 运行** `./gradlew test --tests '*Definition*'`，预期失败。
- [ ] **Step 3: 实现记录类和 Codec**；`ValueDefinition` 明确 `Fixed` 与 `Range(min,max)`，Attribute 操作只允许 `add_value`、`add_multiplied_base`、`add_multiplied_total`。
- [ ] **Step 4: 实现聚合校验**，一次 reload 输出所有资源路径和原因；坏定义不污染旧快照。
- [ ] **Step 5: 运行测试**，预期全部通过。
- [ ] **Step 6: 提交** `feat: define validated datapack schema`。

### Task 4: Reload、不可变快照与 KubeJS 覆盖层接口

**Files:**
- Create: `definition/DefinitionSnapshot.java`, `DefinitionManager.java`, `DefinitionReloadListener.java`, `DefinitionLayer.java`
- Create: `api/DefinitionContribution.java`, `api/SuperReforgeApi.java`
- Test: `definition/DefinitionManagerTest.java`

**Interfaces:**
- Produces: `DefinitionManager.snapshot()`；`reload(ResourceManager)`；`replaceScriptLayer(Collection<DefinitionContribution>)`。
- Consumes: Task 3 的 Codec 与校验器。

- [ ] **Step 1: 写失败测试**，验证 datapack pack 优先级、KubeJS 同 ID 覆盖、重复脚本 ID 报错、失败 reload 保留上次有效快照、成功 reload 原子替换。
- [ ] **Step 2: 运行目标测试**，预期失败。
- [ ] **Step 3: 实现四个 reload 目录**：`levels`、`item_types`、`modifiers`、`catalysts`，先解析到临时 builder，再验证并发布不可变快照。
- [ ] **Step 4: 实现脚本覆盖接口**，不让脚本改写全局 TOML。
- [ ] **Step 5: 运行测试和 `classes`**，预期通过。
- [ ] **Step 6: 提交** `feat: load and layer reforge definitions`。

### Task 5: 类型解析、词条池合并和自动归一化抽取

**Files:**
- Create: `reforge/ItemTypeResolver.java`, `SelectorMatcher.java`, `CandidatePool.java`
- Create: `reforge/WeightNormalizer.java`, `RollEngine.java`, `DeterministicValue.java`
- Test: `reforge/ItemTypeResolverTest.java`, `reforge/WeightNormalizerTest.java`, `reforge/RollEngineTest.java`

**Interfaces:**
- Produces: `resolve(ItemStack, RegistryAccess): Set<ResourceLocation>`；`candidates(ItemStack, CatalystDefinition, Optional<ResourceLocation>): CandidatePool`；`roll(CandidatePool,long): RollResult`。

- [ ] **Step 1: 写失败测试**，覆盖 ID、标签、Ingredient 和脚本谓词选择器；木棍硬绑定 sword；物品同时匹配 sword/axe 时按词条 ID 去重合并。
- [ ] **Step 2: 写失败概率测试**，断言 `20,30,30 -> 0.25,0.375,0.375`、缺省权重为 1、0 权重排除、空等级先移除、禁止相同词条后空等级继续移除。
- [ ] **Step 3: 写失败确定性测试**，相同 ID+seed 在范围变化后稳定重新映射；不同 effect 使用稳定派生盐，定义字段排序不改变已有 effect 的值。
- [ ] **Step 4: 实现解析、候选合并、归一化和两阶段抽取**，所有随机只来自服务器 seed。
- [ ] **Step 5: 运行三个测试类**，预期通过。
- [ ] **Step 6: 提交** `feat: resolve item pools and deterministic rolls`。

### Task 6: 动态名称、Attribute 解析与转换继承

**Files:**
- Create: `item/ResolvedModifier.java`, `ModifierResolver.java`, `ModifierNameService.java`
- Create: `item/VanillaAttributeApplicator.java`, `ModifierLifecycleEvents.java`
- Test: `item/ModifierResolverTest.java`, `item/ModifierNameServiceTest.java`, `item/VanillaAttributeApplicatorTest.java`

**Interfaces:**
- Produces: `resolve(ItemStack, DefinitionSnapshot): Optional<ResolvedModifier>`；`displayName(ItemStack, Component): Component`；按 effect 独立槽位生成稳定 UUID/ID 的 attribute modifiers。

- [ ] **Step 1: 写失败测试**，覆盖直接文本/翻译键、颜色/粗体样式、前缀动态置于当前自定义基础名之前且不写死到 `CUSTOM_NAME`。
- [ ] **Step 2: 写失败测试**，覆盖三个 operation、固定/范围值、主手/副手/四护甲槽；未注册 Attribute 仅报告并跳过，不崩溃。
- [ ] **Step 3: 写失败转换测试**，铁砧、附魔和锻造输出仍匹配任一原池时保留 `ReforgeData`；不再匹配则移除。脚本谓词动态失配时立即停用并按配置移除或保留 inactive。
- [ ] **Step 4: 实现解析、名称和事件接入**；默认不添加自定义 tooltip，只由配置决定原版 Attribute 行可见性。
- [ ] **Step 5: 运行测试和 `classes`**，预期通过。
- [ ] **Step 6: 提交** `feat: resolve modifier names and vanilla attributes`。

### Task 7: 全服进度、成本计算和经验扣除

**Files:**
- Create: `progress/ProgressStage.java`, `ServerProgressData.java`, `ProgressService.java`
- Create: `reforge/Cost.java`, `CostModifier.java`, `CostService.java`, `ExperienceService.java`
- Test: `progress/ProgressServiceTest.java`, `reforge/CostServiceTest.java`, `reforge/ExperienceServiceTest.java`

**Interfaces:**
- Produces: `setStage(MinecraftServer,String,boolean)`；`highestActiveStage()`；`quote(Player,ItemStack,CatalystDefinition): Cost`；`canPay/pay`。

- [ ] **Step 1: 写失败测试**，覆盖全服持久化、重启往返、优先级并列用 ID 稳定排序、只采用最高优先级激活阶段。
- [ ] **Step 2: 写失败成本测试**，材料和经验分别按 `floor(base * multiplier) + addition` 处理并至少为 0；allow/deny 类型过滤；等级模式和点数模式准确扣除；关闭经验时 XP 为 0。
- [ ] **Step 3: 实现 Overworld `SavedData`、成本服务和经验算法**，创造免除行为由配置控制。
- [ ] **Step 4: 运行目标测试**，预期通过。
- [ ] **Step 5: 提交** `feat: persist progression and calculate costs`。

### Task 8: 熔核锻台方块、菜单与原子重铸事务

**Files:**
- Create: `registry/ModBlocks.java`, `ModItems.java`, `ModBlockEntities.java`, `ModMenus.java`
- Create: `block/ReforgerBlock.java`, `ReforgerBlockEntity.java`, `ReforgerMenu.java`, `ReforgerSlots.java`
- Create: `reforge/ReforgeRequest.java`, `ReforgeQuote.java`, `ReforgeTransaction.java`, `ReforgeFailure.java`
- Test: `reforge/ReforgeTransactionTest.java`
- Test: `gametest/ReforgerGameTests.java`

**Interfaces:**
- Produces: 目标槽、媒介槽、输出展示；`quote` 与 `execute` 共享同一验证路径；方块实体保存 `pendingResult`、动画开始 tick 和锁定输入快照。

- [ ] **Step 1: 写失败事务测试**，覆盖目标数量不为 1、媒介 ID/数量不足、玩家经验不足、陈旧客户端报价、无候选、伪造请求和重复点击。
- [ ] **Step 2: 写失败原子性测试**，断言验证失败零扣除；成功时一次性扣除并生成 pending；20 tick 前不可取出，揭晓后可取；关闭 GUI/断线继续；重启恢复；破坏方块结算并掉落，绝不复制。
- [ ] **Step 3: 注册方块/物品/方块实体/菜单**，实现服务端菜单和锁槽。
- [ ] **Step 4: 实现事务**，客户端请求不携带结果、价格或 seed，只携带方块位置和操作意图；服务端重新解析全部状态。
- [ ] **Step 5: 运行单元测试和 GameTest server**，预期通过。
- [ ] **Step 6: 提交** `feat: add atomic molten-core reforger`。

### Task 9: 网络同步、锻造日志 GUI 和 20 tick 动画

**Files:**
- Create: `network/ModPayloads.java`, `StartReforgePayload.java`, `DefinitionSyncPayload.java`, `ReforgerStatePayload.java`
- Create: `client/ReforgerScreen.java`, `ClientDefinitionCache.java`
- Create: `client/render/ReforgerBlockEntityRenderer.java`, `ReforgerRenderState.java`
- Create: `registry/ModParticles.java`, `ModSounds.java`
- Test: `network/PayloadCodecTest.java`, `client/ReforgerRenderStateTest.java`

**Interfaces:**
- Produces: 登录/reload 时同步只读等级、词条名称、候选概率和 UI 所需媒介信息；服务端发送 pending 状态，客户端插值锤头、熔核亮度和火花时点。

- [ ] **Step 1: 写失败 payload 往返与上限测试**，拒绝超大集合、未知资源 ID 和错误方块距离。
- [ ] **Step 2: 写失败动画测试**，断言 tick 0 锤头抬起、6-10 落下、10 熔核峰值和火花、20 揭晓；暂停或丢包时由服务端 tick 校正。
- [ ] **Step 3: 实现“锻造日志”双栏 GUI**，左侧目标/媒介/成本/按钮，右侧显示实际归一化后的等级与词条概率，不显示伪概率。
- [ ] **Step 4: 实现方块实体渲染和粒子/声音触发**，服务端只广播一次冲击事件。
- [ ] **Step 5: 运行测试和 `clientClasses`**，预期通过。
- [ ] **Step 6: 提交** `feat: add forge-log screen and reforge animation`。

### Task 10: 原创 3D 模型、纹理、配方和默认数据

**Files:**
- Create: `assets/superreforge/blockstates/reforger.json`
- Create: `assets/superreforge/models/block/reforger_base.json`, `reforger_hammer.json`, `reforger_core.json`
- Create: `assets/superreforge/models/item/reforger.json`
- Create: `assets/superreforge/textures/block/reforger_*.png`, `textures/item/*_reforge_stone.png`
- Create: `assets/superreforge/lang/en_us.json`, `zh_cn.json`
- Create: `data/superreforge/recipe/reforger.json`, catalyst recipes and loot table
- Create: `data/superreforge/superreforge/levels/*.json`, `item_types/*.json`, `modifiers/*.json`, `catalysts/*.json`
- Test: `resources/DefaultDataValidationTest.java`

**Interfaces:**
- Produces: 8 个中性等级、sword/axe/bow/crossbow/trident/mace/curio、普通/精炼/终极媒介，以及每个池每个可达等级至少一个示例词条。

- [ ] **Step 1: 写失败资源测试**，遍历内置 JSON 并用生产 Codec 解码，验证翻译键存在、配方引用注册物品、每个默认池非空、Super Reforge 未声明自定义 Attribute。
- [ ] **Step 2: 创建原创“熔核锻台”资产**：低矮重型底座、中央熔核、独立锻锤，使用新绘制纹理；在 `CREDITS.md` 说明设计灵感边界和是否含衍生资产。
- [ ] **Step 3: 写完整默认数据和中文/英文翻译**，弓与弩示例只引用真实可用的已注册 Attribute，不把近战攻击伤害伪装成弹射物伤害。
- [ ] **Step 4: 运行资源测试、`processResources` 和客户端模型烘焙冒烟测试**，预期无 missing texture/model。
- [ ] **Step 5: 提交** `feat: add original molten-core assets and defaults`。

### Task 11: 可选 Curios 兼容

**Files:**
- Create: `compat/OptionalMods.java`
- Create: `compat/curios/CuriosCompat.java`, `CuriosAttributeBridge.java`, `CuriosItemMatcher.java`
- Test: `compat/OptionalModsTest.java`, `compat/curios/CuriosAttributeBridgeTest.java`

**Interfaces:**
- Produces: `curios:any` 通用槽目标；所有 Curios 饰品进入共同 `curio` 类型池；通过 Curios 事件附加已解析 effect。

- [ ] **Step 1: 写失败测试**，覆盖任意 Curios 槽、固定/百分比/乘算三种操作、同一词条多个 Attribute，以及非 Curios 环境下不触碰 Curios 类。
- [ ] **Step 2: 配置 `compileOnly`/本地测试 runtime**，`neoforge.mods.toml` 将 Curios 标为 optional。
- [ ] **Step 3: 实现隔离兼容层**，仅在 `ModList.isLoaded("curios")` 时由专用入口加载引用 Curios API 的类。
- [ ] **Step 4: 分别运行无 Curios 的 `runServer` 冒烟测试和带 Curios 的兼容测试**，预期两者通过。
- [ ] **Step 5: 提交** `feat: add optional universal Curios support`。

### Task 12: 可选 KubeJS 定义与全服阶段 API

**Files:**
- Create: `compat/kubejs/SuperReforgeKubePlugin.java`, `KubeDefinitionEvents.java`, `KubeDefinitionBuilder.java`
- Create: `compat/kubejs/KubeProgressApi.java`, `ScriptPredicateRegistry.java`
- Create: `examples/kubejs/superreforge_definitions.js`, `examples/kubejs/superreforge_progression.js`
- Test: `compat/kubejs/KubeDefinitionBuilderTest.java`, `KubeProgressApiTest.java`

**Interfaces:**
- Produces: `SuperReforgeEvents.definitions(event => ...)`；类型/等级/词条/媒介 builder；`SuperReforgeProgress.defineStage(...)` 与 `setActive(server,id,boolean)`。

- [ ] **Step 1: 写失败 builder 测试**，用规格示例注册木棍 sword、多个类型、多个 Attribute、固定/范围值和权重；断言脚本层覆盖同 ID datapack。
- [ ] **Step 2: 写失败阶段测试**，注册多个阶段并只应用最高优先级；全服状态重启后仍存在。
- [ ] **Step 3: 配置 KubeJS 2101 `compileOnly` 和 optional 元数据，使用其插件/事件 API 实现 builder 与脚本谓词注册。
- [ ] **Step 4: 运行无 KubeJS 启动冒烟测试与带 KubeJS 集成测试**，预期两者通过。
- [ ] **Step 5: 提交** `feat: expose optional KubeJS definition APIs`。

### Task 13: 文档、完整验证与发布 JAR

**Files:**
- Create: `docs/CONFIGURATION.md`, `docs/DATAPACK_API.md`, `docs/KUBEJS_API.md`, `docs/FILE_REFERENCE.md`
- Modify: `README.md`, `CHANGELOG.md`, `CREDITS.md`
- Create: `examples/datapack/**`, `examples/kubejs/**`

**Interfaces:**
- Produces: 可安装的 `build/libs/superreforge-<version>.jar` 和 SHA-256；逐文件用途/影响说明。

- [ ] **Step 1: 编写文档测试/链接检查**，确认所有示例 ID、路径、字段和注册 API 与生产代码一致，并扫描未完成标记与空占位实现。
- [ ] **Step 2: 写文档**，逐一说明源文件和资源目录用途、修改会影响什么；所有示例加入中文注释，JSON 严格文件同时提供对应 JSONC 阅读版。
- [ ] **Step 3: 运行** `./gradlew clean test gameTestServer build`，记录测试数量、警告和构建产物。
- [ ] **Step 4: 分别验证核心-only、+Curios、+KubeJS、+Curios+KubeJS 四种依赖矩阵；至少执行服务端启动到加载完成，并对客户端执行菜单/模型/纹理冒烟检查。
- [ ] **Step 5: 检查 JAR**，确认不包含参考模组源码、未声明自定义 Attribute、包含许可证/元数据/默认数据；计算 `Get-FileHash -Algorithm SHA256`。
- [ ] **Step 6: 提交** `release: build Super Reforge initial jar`，向用户提供 JAR 的绝对可点击路径、SHA-256、验证结果和已知限制。

## Self-Review Results

- **规格覆盖：** 14 个规格章节均映射到 Task 1-13；名称、概率、等级、多个类型、Curios 通用槽、KubeJS 覆盖、全服阶段、事务动画和依赖矩阵均有明确任务。
- **占位符扫描：** 每一步都有明确文件、接口、测试命令和完成标准；KubeJS 示例固定放在 `examples/kubejs`，不会被误装成自动执行脚本。
- **类型一致性：** `DefinitionSnapshot`、`ReforgeData`、`CandidatePool`、`RollResult`、`Cost`、`ResolvedModifier` 的生产者与消费者已对齐；重铸报价和执行共享服务端验证路径。
- **风险检查：** 最早验证 NeoForge 工具链；可选依赖在核心完成后接入；资产和 GUI 在事务正确性后实现；最终用四种依赖组合验证。
