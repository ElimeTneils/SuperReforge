# Super Reforge 文件参考

本文按目录排列，记录仓库中当前源码、资源、测试和根构建文件的职责与修改影响。这里描述的是已由代码和资源直接体现的行为；可选依赖未安装时不会推断其运行结果。

## `src/main/java/com/mutuo/superreforge`

- `SuperReforge.java`：NeoForge 模组入口，串联注册表、数据组件、网络、定义重载、原版属性注入、物品生命周期事件和服务器配置；仅在检测到 Curios 后以反射加载桥接类。修改注册顺序、`MOD_ID` 或可选类名会影响整个模组启动、资源命名空间与缺少 Curios 时的类加载隔离。

### `api`

- `DefinitionContribution.java`：脚本定义贡献的函数接口，向收集器写入定义。改变签名会破坏 KubeJS/其他 API 调用方。
- `ScriptDefinitionBundle.java`：不可变脚本层打包值，含等级、类型、词条、媒介、脚本谓词和进度阶段。字段变更会改变 KubeJS 重载时可发布的数据边界。
- `ScriptDefinitionCollector.java`：同步收集脚本添加的四类定义、谓词与阶段，`build()` 生成 bundle。修改覆盖/重复 ID 规则会影响脚本的最终数据层。
- `SuperReforgeApi.java`：公开读取当前定义、以贡献集合替换脚本定义的门面。修改会影响外部 Java 集成及脚本层发布成功/失败的语义。

### `block`

- `PendingReforge.java`：保存待揭晓的结果栈、总 tick 与剩余 tick，提供递减和完成判定。改动会影响锻造动画时长、存档恢复和掉落保护。
- `ReforgerBlock.java`：熔核锻台方块；维护四向水平朝向和随方向旋转的非整块碰撞轮廓，服务端打开菜单、移除时掉落库存与待完成结果，并只在服务端挂方块实体 tick。改朝向/碰撞会影响模型与锤架命中范围；改交互、ticker 或移除逻辑会影响 GUI 打开、动画提交与物品丢失/重复掉落风险。
- `ReforgerBlockEntity.java`：锻台状态机，维护目标/媒介两槽、失败码、待完成结果，发起报价、扣媒介和经验、运行粒子音效及 NBT/客户端同步。修改影响重铸原子性、创造模式付款、断档恢复、方块破坏掉落和菜单进度显示。
- `ReforgerMenu.java`：容器菜单，建立两个机器槽与背包槽、处理开始按钮和 shift-click；由服务端重新报价并发预览载荷。改槽位索引、按钮 ID 或同步条件会影响客户端容器兼容性和预览可信边界。

### `client`

- `ReforgerBlockEntityRenderer.java`：客户端方块实体渲染器，读取同步后的 pending 状态并按方块朝向渲染动态锻锤。改其渲染计算只影响视觉，不应在此改服务端结果。
- `ReforgerRenderState.java`：把总/剩余 tick 与 partial tick 转为进度、锤高、核心强度。改公式会影响动画曲线和测试预期。
- `ReforgerScreen.java`：锻台容器屏幕，绘制背景、标签、进度和开始交互，并消费菜单/预览状态。改坐标、文案或状态分支会影响 GUI 可用性而非重铸判定。
- `SuperReforgeClient.java`：客户端事件注册入口，绑定锻台屏幕、方块实体渲染器和预览接收。改注册项会导致客户端界面或渲染缺失。

### `compat`

#### `compat/curios`

- `CuriosCompat.java`：Curios 可选桥接，初始化后向选择器钩子注入 Curios 槽位匹配，并把 `curios:any` 效果接入 Curios 属性路径。它只由入口在确认 Curios 已安装后反射加载；直接让核心包引用 Curios 类型会破坏未安装 Curios 的启动隔离。

#### `compat/kubejs`

- `KubeReforgeBindings.java`：KubeJS 暴露的绑定，接收等级、类型、词条、媒介、谓词、阶段及服务器阶段开关调用。改绑定名称或入参会破坏现有脚本；应保持定义先收集、再整体发布的方式。
- `SuperReforgeKubeJSPlugin.java`：KubeJS 插件生命周期桥；脚本加载前清理收集器，加载后发布定义层/谓词/阶段。改生命周期钩子会影响脚本删除后的清理以及数据包与脚本层的覆盖关系。

### `config`

- `ExperienceMode.java`：经验扣费枚举（等级或精确经验点）。改枚举值会影响 TOML 兼容和 `ExperienceService` 的扣费方式。
- `GlobalSettings.java`：业务使用的不可变全局设置快照，含经验、自动首词条、媒介、提示行、动画、创造付款和缺定义策略；构造时拒绝无媒介/非正动画时长。改默认值或字段会影响所有报价和生命周期路径。
- `MissingDefinitionPolicy.java`：词条定义消失时的 `REMOVE`/`KEEP_INACTIVE` 策略枚举。改语义会影响旧物品数据组件的保留方式。
- `SuperReforgeConfig.java`：声明服务器 `superreforge-server.toml` 并将 NeoForge holder 转为 `GlobalSettings`；非法自动媒介 ID 回退默认值。修改键名、范围或 snapshot 映射会影响服务器配置迁移及运行时规则。

### `definition`

- `AttributeEffectDefinition.java`：一个词条属性效果的数据模型/Codec（效果 ID、属性、值、运算、槽位、tooltip 开关）。改字段会同时影响数据包 JSON、KubeJS 定义和属性注入。
- `AttributeOperation.java`：数据层的三种属性运算枚举及字符串 Codec。改序列化名会使已有词条 JSON 失效或改变数值含义。
- `CatalystDefinition.java`：媒介定义与 Codec，描述匹配选择器、消耗量、经验、可否重复词条、等级权重和类型限制。修改会直接改变报价、候选池和 JSON 结构。
- `DefinitionLayer.java`：脚本层四类定义的不可变覆盖层及 builder。改其复制/构建逻辑会影响 KubeJS 重载的原子替换。
- `DefinitionManager.java`：维护 datapack、脚本和合并后的活动快照；校验成功后原子发布，脚本同 ID 覆盖数据包。改合并优先级或发布顺序会改变 reload 可见性与失败时保留旧状态的保证。
- `DefinitionReloadListener.java`：资源重载监听器，从 `superreforge/levels`、`item_types`、`modifiers`、`catalysts` 读取 JSON，经 Codec 解析后发布；传给 1.21.1 资源管理器的目录不含末尾斜杠。改目录、ID 转换或错误聚合会影响数据包布局和 reload 失败条件。
- `DefinitionSnapshot.java`：等级、类型、词条、媒介四张不可变映射组成的活动快照。改字段会波及所有解析、报价和预览消费者。
- `DefinitionValidationException.java`：定义加载/解析错误的专用非法参数异常。改异常类型会影响重载报错路径及测试断言。
- `DefinitionValidator.java`：补充 Codec 无法表示的约束：空选择器、权重、等级/类型引用、效果 ID、数值范围、媒介数量与经验。减少校验会让坏数据进入运行时；加约束会改变旧数据包的可加载性。
- `ItemSelector.java`：物品选择器的 Codec，支持单 ID、ID 列表、标签、`curios:any` 和 KubeJS 谓词。改匹配字段会影响类型和媒介 JSON 的表达能力。
- `ItemTypeDefinition.java`：物品类型的 include/exclude 选择器集合及 Codec。它决定物品可进入哪些词条池；改匹配组合会改变候选词条。
- `LevelDefinition.java`：等级的 rank 和本地化名称/颜色数据模型。改 rank/名称字段会影响预览排序与显示。
- `ModifierDefinition.java`：词条定义的等级、适用类型、名称、权重和属性效果。改字段直接影响随机池、展示和属性。
- `SlotTarget.java`：主/副手、护甲及 `CURIOS_ANY` 槽位枚举与 Codec。改序列化名会破坏 JSON；把 Curios 槽映射进原版路径会破坏兼容分层。
- `ValidationReport.java`：校验错误集合，提供有效性与抛出方法。改它会影响重载失败的呈现方式。
- `ValueDefinition.java`：密封值定义（固定值或区间值）及 Codec，是词条效果数值的数据表示。改分支或格式会影响确定性数值计算和 JSON 兼容。
- `WeightedLevel.java`：媒介所指等级及其相对权重的 Codec 值。改权重语义会改变两阶段抽取概率。

### `item`

- `ModifierLifecycle.java`：校正单件物品的词条：缺定义时按策略移除/保留，并可通过配置自动抽取首词条；跳过可堆叠物品。改这里会影响铁砧、升级等改变物品后的一致性和自动赋词条范围。
- `ModifierLifecycleEvents.java`：每 20 tick 在服务端遍历玩家背包，调用生命周期校正。改间隔/范围会影响性能、词条生效延迟和随机种子消耗。
- `ModifierNameService.java`：把词条名前缀与当前物品名组合的显示工具。改格式只影响由 mixin 生成的名称表现。
- `ModifierResolver.java`：从物品 `ReforgeData` 按当前快照解析词条和确定性效果；词条不存在或类型不匹配即失效。改此处会影响属性、名称和 reload 后旧物品的解释。
- `ReforgeData.java`：存入 ItemStack 数据组件的最小持久状态（词条 ID、seed、schema），含磁盘与网络 Codec。改 schema/字段需兼顾已存档物品和同步兼容。
- `ResolvedEffect.java`：已解析效果值（属性、实际 amount、运算、槽位、tooltip）的运行时值。改字段会影响属性注入和预览/显示消费者。
- `ResolvedModifier.java`：已解析词条 ID、名称和效果列表的运行时值。改结构会影响原版/Curios 属性桥和名称显示。
- `VanillaAttributeApplicator.java`：监听原版属性事件，注入非 Curios 效果；并控制 Attribute tooltip 跳过项，生成稳定效果 ID。改槽映射、稳定 ID 或隐藏规则会影响属性叠加、tooltip 与去重。

### `mixin`

- `ItemStackNameMixin.java`：混入 ItemStack 名称获取路径，在已解析词条时用 `ModifierNameService` 加前缀。改注入目标/时机可能影响所有物品名称；对应配置在 `superreforge.mixins.json`。

### `network`

- `ClientPreviewState.java`：客户端按容器 ID 缓存最新重铸预览。改缓存键/清理规则会影响切换容器后的预览正确性。
- `ModNetwork.java`：在模组事件总线注册自定义 payload。改注册会影响客户端能否解码服务端预览。
- `PreviewLevel.java`：预览中的等级、显示名、概率和词条集合值。改字段需同步 `ReforgePreviewPayload` 编解码与屏幕。
- `PreviewModifier.java`：预览中的词条 ID、名称、概率值。改字段会影响网络格式及界面展示。
- `ReforgePreviewPayload.java`：服务端从真实报价和快照构造的重铸预览 payload，并定义 type/stream codec 与客户端处理。改概率构造或 Codec 会影响 GUI 展示和网络兼容；它不是客户端可提交的报价。

### `progress`

- `ProgressService.java`：维护脚本定义的阶段列表，在服务器 SavedData 中查询最高优先级的活动阶段，并提供开关。改选择排序或替换策略会改变成本修正来源。
- `ProgressStage.java`：阶段 ID、优先级及媒介/经验 `CostModifier` 的值模型。改字段会影响 KubeJS 阶段 API 和报价。
- `ServerProgressData.java`：世界持久化的活动阶段集合，使用 SavedData 读写。改数据键会影响已有世界的阶段状态迁移。

### `reforge`

- `CandidateLevel.java`：候选等级及其等级权重、等级内词条权重列表。改结构会影响两阶段随机与预览概率计算。
- `CandidatePool.java`：根据目标类型、媒介限制、已有词条、等级和词条权重构建最终候选池。改过滤条件会直接改变可抽词条和重复词条规则。
- `Cost.java`：媒介数量和经验数量的不可变报价值。改字段会波及提交、预览和付款。
- `CostModifier.java`：成本乘法加法修正，负责把阶段修正应用到基础成本。改取整/顺序会改变阶段实际收费。
- `CostService.java`：合成媒介基础成本、经验开关与最高活动阶段的成本修正。改这里影响所有菜单报价。
- `DeterministicValue.java`：以物品 seed 和效果 ID 解析固定/区间数值，使重载前后同一词条结果可复现。改随机算法会改变已有物品的效果值。
- `ExperienceService.java`：按 `LEVELS` 或 `POINTS` 检查/扣除经验；创造免费由调用者决定。改它会影响付款判定及扣费单位。
- `ItemTypeResolver.java`：返回一个物品命中的全部类型（不是首个），include 命中且 exclude 未命中才有效。改它会改变多类型词条池合并。
- `PreparedReforge.java`：已准备的目标结果栈和媒介余量值，尚未写入方块实体。改结构会影响事务提交边界。
- `ReforgeFailure.java`：报价/启动失败原因枚举（空目标、数量、类型、媒介、材料、候选、经验、忙碌、陈旧状态等）。改顺序会影响菜单同步的 ordinal。
- `ReforgeQuote.java`：成功报价值，含媒介 ID/定义、成本与候选池。改字段会影响提交及预览。
- `ReforgeQuoteResult.java`：成功报价或失败原因的互斥返回包装。改构造约束会影响调用方错误处理。
- `ReforgeTransaction.java`：纯服务端报价与准备阶段：检查目标、选择最小 ID 媒介、成本、候选、写入 `ReforgeData`、按需要消耗媒介。改此处会影响重铸规则；不要在这里引入直接玩家/方块副作用。
- `RollEngine.java`：以 seed 先抽等级再抽词条，浮点边界回退末项。改抽样次序或随机实现会改变所有既定种子结果。
- `RollResult.java`：抽取结果的词条 ID 与 seed。改字段会影响数据组件写入。
- `SelectorHooks.java`：隔离核心代码与 KubeJS/Curios 的可选谓词表；脚本重载整体替换谓词，Curios 初始化注入匹配器。改默认或线程安全性会影响缺可选模组时的匹配。
- `SelectorMatcher.java`：执行一个选择器的 item/items/tag/Curios/KubeJS 匹配。改判定优先级会影响类型和媒介选择。
- `WeightedValue.java`：携带值、原始权重、归一概率的泛型值；两参构造用于未归一输入。改结构影响随机和预览。
- `WeightNormalizer.java`：拒绝非法权重，去掉零权重并把相对权重归一。改零权重或浮点处理会影响随机分布及测试。

### `registry`

- `ModBlockEntities.java`：注册熔核锻台的方块实体类型。改注册 ID/绑定方块会使世界方块实体无法加载。
- `ModBlocks.java`：注册 `reforger` 方块及其强度、亮度和非整块遮挡属性。改 ID 或物理属性会影响配方、标签、模型、光照和已有世界方块。
- `ModDataComponents.java`：注册 `reforge_data` 数据组件及其持久/网络 Codec。改 ID 或 Codec 会影响物品存档和同步。
- `ModItems.java`：注册锻台物品、三种重铸石和锻造锤。改 ID 会影响配方、媒介 JSON、语言和模型。
- `ModMenus.java`：注册锻台菜单类型及客户端缓冲区构造器。改注册或 buf 格式会导致菜单无法打开。
- `ModRegistries.java`：集中调用方块、物品、菜单、方块实体注册。改调用会造成对应内容未注册。

## `src/main/resources`

### 顶层资源

- `pack.mcmeta`：资源包格式与描述；Minecraft 版本升级时需同步格式号。
- `superreforge.mixins.json`：声明 Java 21 的 `ItemStackNameMixin`，且注入默认必须命中。改包名/类名会使名称 mixin 加载失败。
- `kubejs.plugins.txt`：将 `SuperReforgeKubeJSPlugin` 绑定到 `kubejs` mod ID；该文件本身也维持 KubeJS 未安装时不加载插件的隔离。

### `assets/superreforge`

- `blockstates/reforger.json`：把四个 `facing` 状态映射到同一 3D 模型的 0/90/180/270 度旋转；改状态键或角度会使世界模型与碰撞/动态锻锤方向不一致。
- `models/block/reforger.json`：锻台方块模型及纹理引用；改几何/纹理 ID 只影响渲染。
- `models/item/reforger.json`、`common_reforge_stone.json`、`refined_reforge_stone.json`、`supreme_reforge_stone.json`、`forge_hammer.json`：对应方块物品/媒介/工具的物品模型。它们应与 `ModItems` 的注册 ID 对齐，否则物品会显示为缺失模型。
- `lang/en_us.json`、`lang/zh_cn.json`：英文/简体中文词条，覆盖锻台、失败提示、等级和示例词条名；例如 `level.superreforge.common`、`modifier.superreforge.melee_1`、`container.superreforge.reforger`。改键名会影响 JSON `translate`、屏幕与 mixin 名称显示。

### `data/minecraft`

- `tags/block/needs_iron_tool.json`：将锻台纳入铁镐需求标签；改它影响挖掘等级判定。
- `tags/block/mineable/pickaxe.json`：将锻台纳入镐可挖标签；改它影响采掘效率/掉落。

### `data/superreforge`

- `loot_table/blocks/reforger.json`：锻台方块战利品表；方块实体额外库存/待完成结果由 Java 破坏路径掉落，不能把这些动态内容误写成固定 loot。
- `recipe/reforger.json`：熔核锻台合成配方。改输入或结果 ID 会影响生存获取方式。
- `recipe/common_reforge_stone.json`、`refined_reforge_stone.json`、`supreme_reforge_stone.json`：三种媒介的合成配方，对应下列媒介定义与 `ModItems` 注册。

### `data/superreforge/superreforge`（可重载定义）

- `levels/worn.json`、`common.json`、`fine.json`、`rare.json`、`epic.json`、`legendary.json`、`mythic.json`、`divine.json`：八个等级定义，提供 rank 与本地化名称；`common.json` 例为 rank 2、白色“普通”。这些 ID 被媒介与词条引用，删除/改名会被校验拒绝。
- `item_types/axe.json`、`bow.json`、`crossbow.json`、`curio.json`、`mace.json`、`sword.json`、`trident.json`：七类目标物品选择器。`sword.json` 例含 `minecraft:swords` 标签与 `minecraft:stick` 单品；`curio.json` 使用可选 Curios 匹配时，在无 Curios 环境不会由核心硬加载该依赖。改 include/exclude 会改变全部词条候选池。
- `catalysts/common_reforge_stone.json`、`refined_reforge_stone.json`、`supreme_reforge_stone.json`：三种媒介的物品匹配、数量/经验、允许等级权重及类型限制。普通石例匹配 `superreforge:common_reforge_stone`，消耗 1、经验 3、覆盖 worn/common/fine/rare；权重是相对值，不要求和为 1。
- `modifiers/melee_1.json` 至 `melee_8.json`：近战词条样本，分别落在八个等级；`melee_1` 适用 sword/axe/trident/mace，例含主手攻击伤害和攻速的 `add_multiplied_base` 效果。
- `modifiers/ranged_1.json` 至 `ranged_8.json`：远程武器词条样本，面向 bow/crossbow 等类型；改属性、槽位、权重或等级会改变候选概率和原版属性。
- `modifiers/curio_1.json` 至 `curio_8.json`：Curios 词条样本，面向 `curio` 类型并使用 `curios:any` 槽位；其定义可被读取，但实际 Curios 属性桥只在可选依赖存在时初始化。

## `src/test/java/com/mutuo/superreforge`

- `BootstrapMetadataTest.java`：检查构建/模组元数据相关约定。
- `DocumentationContractTest.java`：检查文档约定，更新对外格式时应同步测试。

### `api`

- `ScriptDefinitionCollectorTest.java`：验证脚本收集器对定义、谓词和阶段的汇总行为。

### `block`

- `PendingReforgeTest.java`：验证 pending tick 递减与 ready 边界。
- `ReforgerBlockTest.java`：验证真实注册锻台具有默认北向和四个水平朝向。

### `client`

- `ReforgerRenderStateTest.java`：验证剩余 tick 到锤/核心动画状态的计算。

### `config`

- `SuperReforgeConfigTest.java`：验证配置快照、默认值和配置约束。

### `definition`

- `DefaultResourcesTest.java`：读取默认数据包并用生产 Codec 检查等级、类型、词条、媒介引用，同时验证 3D 模型元素和四向 blockstate。
- `DefinitionCodecTest.java`：验证定义 JSON Codec 的编码/解码。
- `DefinitionReloadListenerTest.java`：使用原版 `MultiPackResourceManager` 回归验证 reload 目录参数合法，防止真实服务器在 datapack 阶段中止。
- `DefinitionManagerTest.java`：验证 datapack/脚本层的原子发布、覆盖和失败保留旧快照。
- `DefinitionValidatorTest.java`：验证不合法权重、引用、效果与媒介被拒绝。

### `item`

- `ModifierLifecycleTest.java`：验证缺定义策略与自动初始词条生命周期。
- `ModifierNameServiceTest.java`：验证名称前缀组合。
- `ModifierResolverTest.java`：验证数据组件按快照解析与类型失配处理。
- `ReforgeDataCodecTest.java`：验证数据组件磁盘/网络 Codec 与 schema 约束。
- `VanillaAttributeApplicatorTest.java`：验证操作/槽位映射、稳定效果 ID 和 tooltip 隐藏规则。

### `progress`

- `ProgressServiceTest.java`：验证活动阶段选择与成本修正规则。

### `reforge`

- `CostServiceTest.java`：验证媒介基础成本、经验开关和阶段修正的报价。
- `ItemTypeResolverTest.java`：验证 include/exclude 及多类型解析。
- `ReforgeTransactionTest.java`：验证报价失败码、候选构建、准备结果和媒介消耗。
- `RollEngineTest.java`：验证种子确定性的两阶段抽取。
- `WeightNormalizerTest.java`：验证零/非法权重过滤、归一化和概率总和。

## 根构建文件

- `build.gradle`：Java 21 + NeoForge ModDevGradle 构建、资源生成、JUnit、客户端/服务端/Data/GameTest 运行配置；Curios/KubeJS 为 `compileOnly`，不会被打包。改依赖范围会改变无可选模组时的可加载性；改 `unitTest` 会影响这些真实 Minecraft 类型测试。
- `settings.gradle`：Gradle 插件仓库与固定根项目名 `superreforge`。改名会改变构建坐标/IDE 项目名。
- `gradle.properties`：Minecraft 1.21.1、NeoForge、Parchment、模组坐标/版本、Curios/KubeJS 编译版本及 Gradle 性能参数。改版本必须与 `build.gradle` 和兼容源码 API 一并核对。
- `gradlew`、`gradlew.bat`：Unix/Windows Gradle Wrapper 启动器。一般不手改业务逻辑；升级 wrapper 时与 `gradle/wrapper` 元数据一起处理。
