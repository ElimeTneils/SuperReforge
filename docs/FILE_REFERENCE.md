# Super Reforge 文件参考

本文按目录排列，记录仓库中当前源码、资源、测试和根构建文件的职责与修改影响。这里描述的是已由代码和资源直接体现的行为；可选依赖未安装时不会推断其运行结果。

## `src/main/java/com/mutuo/superreforge`

- `SuperReforge.java`：NeoForge 模组入口，串联注册表、数据组件、网络、定义重载、原版属性注入、物品生命周期事件和服务器配置；仅在检测到 Curios 后以反射加载桥接类。修改注册顺序、`MOD_ID` 或可选类名会影响整个模组启动、资源命名空间与缺少 Curios 时的类加载隔离。

### `api`

- `DefinitionContribution.java`：脚本定义贡献的函数接口，向收集器写入定义。改变签名会破坏 KubeJS/其他 API 调用方。
- `ScriptDefinitionBundle.java`：不可变脚本层打包值，含等级、类型、词条、媒介、脚本谓词和进度阶段。字段变更会改变 KubeJS 重载时可发布的数据边界。
- `ScriptDefinitionPublisher.java`：原子发布脚本 bundle；首次开服缺少 datapack 基础等级/类型时暂存并在资源重载完成后自动重试，正常运行期的无效脚本则立即拒绝。它同时提交/清理谓词与阶段，修改重试或清理时机会影响首次进世界是否需要手动 `/reload`、半发布防护及跨世界隔离。
- `ScriptDefinitionCollector.java`：同步收集脚本添加的四类定义、谓词与阶段，`build()` 生成 bundle。修改覆盖/重复 ID 规则会影响脚本的最终数据层。
- `SuperReforgeApi.java`：公开读取当前定义、以贡献集合替换脚本定义的门面。修改会影响外部 Java 集成及脚本层发布成功/失败的语义。

### `block`

- `PendingReforge.java`：保存待揭晓的结果栈、总 tick 与剩余 tick，提供递减和完成判定。改动会影响锻造动画时长、存档恢复和掉落保护。
- `ReforgerBlock.java`：熔核锻台方块；维护四向水平朝向和随方向旋转的非整块碰撞轮廓，服务端打开菜单、移除时掉落库存与待完成结果，并只在服务端挂方块实体 tick。改朝向/碰撞会影响模型与锤架命中范围；改交互、ticker 或移除逻辑会影响 GUI 打开、动画提交与物品丢失/重复掉落风险。
- `ReforgerBlockEntity.java`：锻台状态机，维护目标/媒介两槽、失败码、待完成结果，发起报价、扣媒介和经验、运行粒子音效及 NBT/客户端同步。修改影响重铸原子性、创造模式付款、断档恢复、方块破坏掉落和菜单进度显示。
- `ReforgerLayout.java`：菜单与屏幕共享的 GUI 尺寸、机器槽、日志裁剪区、滚动条和警告命中坐标；不可变 `PlayerSlotPosition` 表是 176px 原版背包面板中 27 个背包槽与 9 个快捷栏槽（共 36 个）的唯一几何来源，同时给出 16px 内容区与 18px 槽框坐标。修改它会同步影响客户端绘制与服务端容器槽位；错误坐标会造成第十列、槽位错位、滚轮误触或悬停区域覆盖背包。
- `ReforgerMenu.java`：容器菜单，使用 `ReforgerLayout` 建立两个机器槽，并直接消费共享的 36 项 `PlayerSlotPosition` 表建立玩家背包槽；处理原版开始按钮和 shift-click，由服务端重新报价并发预览载荷。改槽位索引、共享顺序、按钮 ID 或同步条件会影响客户端容器兼容性和预览可信边界。

### `client`

- `ModifierTooltipFormatter.java`：把客户端同步的词条定义格式化为悬停 Component，显示词条 ID、外部 Attribute ID、固定值/随机范围、运算和槽位。修改它只影响候选详情展示；它不会提前生成新随机种子或改变物品实际属性。
- `ReforgerBlockEntityRenderer.java`：客户端方块实体渲染器，读取同步后的 pending 状态并逐步执行 `ReforgerHammerGeometry.TransformPlan`；实际 PoseStack 的固定枢轴、方块 yaw、局部 Z 旋转、缩放与模型偏移顺序因此与几何测试完全相同。改其变换只影响视觉，不应在此改服务端结果。
- `ReforgerHammerGeometry.java`：集中声明锤子缩放、砧面高度、真实模型边界、固定连接枢轴、-45° 静止/接触角、-75° 抬起角、-47° 回弹角和四向 yaw；不可变 `TransformPlan`/`TransformStep` 同时供 renderer 与测试消费，并提供基于分离轴定理（SAT）的旋转盒正体积相交判定。修改会影响锤头是否对准中央砧面、四向关键帧是否穿模以及渲染矩阵顺序。
- `ReforgerLogModel.java`：把服务端嵌套等级/词条预览扁平化为完整有序的单行日志；每个 `Row` 同时保留 `levelName`、词条 ID/名称、最终概率和连续几何位置，等级不再额外占一行，并集中计算滚轮、滑块和视口边界。修改会影响日志顺序、行高、Attribute 悬停解析和滚动距离，不会改变服务端概率。
- `ReforgerRenderState.java`：把总/剩余 tick 与 partial tick 转为静止、抬锤、接触、回弹、复位五段局部 Z 角度及核心强度；关键帧输出直接进入共享 `TransformPlan`。改公式会影响约一秒动画曲线和接触时点。
- `ReforgerScreen.java`：286×218 的熔核锻造日志屏幕，使用原版 `Button`，绘制操作/176px 背包/日志分区，从共享位置表绘制全部 36 个槽框，将等级、词条和最终概率绘制在同一行；`canStart` 保证无有效报价、定义未同步或正在锻造时按钮禁用；屏幕还负责裁剪完整列表并处理滚轮、拖动滑块、Attribute 悬停和截断警告。改坐标、裁剪或状态分支会影响 GUI 可用性而非服务端重铸判定。
- `SuperReforgeClient.java`：客户端事件注册入口，绑定锻台屏幕、方块实体渲染器和预览接收。改注册项会导致客户端界面或渲染缺失。

### `compat`

#### `compat/curios`

- `CuriosCompat.java`：Curios 可选桥接，初始化后注入任意饰品匹配及属性路径；Attribute ID 包含栏位类型与序号，同类多槽可叠加。它先用 `CuriosContextPolicy` 区分真实服务端实体与客户端 tooltip 上下文，只有服务端实体才校正物品数据；定义 reload 后还会清理并重装已穿戴饰品属性。该类只在确认 Curios 已安装后反射加载。
- `CuriosContextPolicy.java`：把 null 实体明确归为 `SYNTHETIC_CLIENT`，把真实实体分为客户端/服务端上下文；创意搜索与 JEI 合成 tooltip 因而可读取已保存词条，但不会对 nullable entity 调用 `level()` 或触发服务端权威校正。

#### `compat/kubejs`

- `KubeReforgeBindings.java`：KubeJS 暴露的绑定，接收等级、类型、词条、媒介、谓词、阶段及服务器阶段开关调用。改绑定名称或入参会破坏现有脚本；应保持定义先收集、再整体发布的方式。
- `SuperReforgeKubeJSPlugin.java`：KubeJS 插件生命周期桥；脚本加载前清理收集器与旧待发布候选，加载后通过统一发布器提交定义层/谓词/阶段；首次开服若 datapack 尚未就绪则等待自动补发。改生命周期钩子会影响首次加载、脚本删除后的清理以及数据包与脚本层的覆盖关系。

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
- `DefinitionManager.java`：维护 datapack、脚本和合并后的活动快照；校验成功后原子发布，脚本同 ID 覆盖数据包；客户端断线只清显示镜像，服务端停止才清定义层，并记录 datapack 是否已经完成首次发布。改合并优先级、会话清理或发布顺序会改变 reload 可见性、跨世界隔离与失败时保留旧状态的保证。
- `ModifierDisplayDefinition.java`：只含词条名称和 Attribute 效果的客户端镜像值，不下发权重、类型或脚本谓词。修改字段需要同步网络 Codec；加入服务端抽取规则会扩大不必要的信息暴露面。
- `DefinitionReloadListener.java`：资源重载监听器，从 `superreforge/levels`、`item_types`、`modifiers`、`catalysts` 读取 JSON，经 Codec 解析后发布；传给 1.21.1 资源管理器的目录不含末尾斜杠；成功后自动重试可能早于 datapack 执行的 KubeJS bundle。改目录、ID 转换、错误聚合或重试位置会影响数据包布局、首次世界加载和 reload 失败条件。
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

- `AttributeRefreshService.java`：定义成功发布后清理在线玩家身上全部 `superreforge:effect/*` 瞬时 modifier，再按当前定义重装原版装备并调用可选饰品刷新钩子。修改所有权判断或刷新顺序会影响 reload 后旧属性能否可靠移除。
- `ModifierLifecycle.java`：校正单件物品的词条：缺定义时按策略移除/保留，并可通过配置自动抽取首词条；跳过可堆叠物品。改这里会影响铁砧、升级等改变物品后的一致性和自动赋词条范围。
- `ModifierLifecycleEvents.java`：每 20 tick 在服务端遍历玩家背包，调用生命周期校正。改间隔/范围会影响性能、词条生效延迟和随机种子消耗。
- `ModifierNameService.java`：把词条名前缀与当前物品名组合的显示工具。改格式只影响由 mixin 生成的名称表现。
- `ModifierResolver.java`：从物品 `ReforgeData` 按当前快照解析词条和确定性效果；词条不存在或类型不匹配即失效。改此处会影响属性、名称和 reload 后旧物品的解释。
- `ReforgeData.java`：存入 ItemStack 数据组件的最小持久状态（词条 ID、seed、schema、是否生效），含磁盘与网络 Codec；旧 schema 缺少 `active` 时按生效迁移。改 schema/字段需兼顾已存档物品和同步兼容。
- `ResolvedEffect.java`：已解析效果值（属性、实际 amount、运算、槽位、tooltip）的运行时值。改字段会影响属性注入和预览/显示消费者。
- `ResolvedModifier.java`：已解析词条 ID、名称和效果列表的运行时值。改结构会影响原版/Curios 属性桥和名称显示。
- `VanillaAttributeApplicator.java`：监听原版属性事件，注入非 Curios 效果；并控制 Attribute tooltip 跳过项，生成稳定效果 ID。改槽映射、稳定 ID 或隐藏规则会影响属性叠加、tooltip 与去重。

### `mixin`

- `ItemStackNameMixin.java`：混入 ItemStack 名称获取路径，在已解析词条时用 `ModifierNameService` 加前缀。改注入目标/时机可能影响所有物品名称；对应配置在 `superreforge.mixins.json`。

### `network`

- `ClientPreviewState.java`：客户端按容器 ID 缓存最新重铸预览。改缓存键/清理规则会影响切换容器后的预览正确性。
- `ClientDefinitionSync.java`：客户端按代次与块序号原子组装只读显示快照；缺块、冲突块和陈旧块不会覆盖最后一份完整定义。修改组装规则会影响登录/reload 后的名称与 Attribute 显示一致性。
- `DefinitionSyncAckPayload.java`：客户端完成或拒绝一次定义同步后发给服务端的确认。修改格式必须同时升级网络协议版本。
- `DefinitionSyncPayload.java`：带代次、块序号和总块数的词条显示快照载荷，限制每块 128 条、总计 8192 条。修改上限或 Codec 会影响大型数据包同步与内存边界。
- `DefinitionSyncTracker.java`：服务端按玩家记录期望代次与成功 ACK；未确认、失败或陈旧确认都会禁止该玩家开始重铸。修改判定会影响定义不同步时的安全门。
- `ModNetwork.java`：注册预览、分块定义同步和 ACK payload；登录/datapack reload 与 KubeJS 独立 reload 都走同一同步入口。同步前刷新已穿戴属性，退出时清理玩家状态。修改时机会影响动态解析、实体数值和重铸安全门。
- `PreviewLevel.java`：预览中的等级、显示名、概率和词条集合值。改字段需同步 `ReforgePreviewPayload` 编解码与屏幕。
- `PreviewModifier.java`：预览中的词条 ID、名称、概率值。改字段会影响网络格式及界面展示。
- `ReforgePreviewPayload.java`：服务端从真实报价和快照构造的重铸预览 payload，并定义 type/stream codec、等级/单级词条安全上限和 `truncated` 标记。改概率构造或 Codec 会影响 GUI 展示和网络兼容；它不是客户端可提交的报价。

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
- `ModCreativeTabs.java`：注册独立 `Super Reforge` 创造页签，并以唯一清单固定显示重铸台与强化石 1–6 级的顺序；动画锤子有意隐藏。修改清单会改变玩家创造栏可见内容，不影响物品注册本身。
- `ModDataComponents.java`：注册 `reforge_data` 数据组件及其持久/网络 Codec。改 ID 或 Codec 会影响物品存档和同步。
- `ModItems.java`：注册锻台物品、六级强化石和锻造锤。1–3 级沿用原三种石头的稳定 ID，4–6 级使用新 ID；改 ID 会影响配方、KubeJS 媒介、语言和模型。
- `ModMenus.java`：注册锻台菜单类型及客户端缓冲区构造器。改注册或 buf 格式会导致菜单无法打开。
- `ModRegistries.java`：集中调用方块、物品、独立创造页签、菜单和方块实体注册。改调用顺序或遗漏调用会造成对应内容未注册。

## `src/main/resources`

### 顶层资源

- `pack.mcmeta`：资源包格式与描述；Minecraft 版本升级时需同步格式号。
- `superreforge.mixins.json`：声明 Java 21 的 `ItemStackNameMixin`，且注入默认必须命中。改包名/类名会使名称 mixin 加载失败。
- `kubejs.plugins.txt`：将 `SuperReforgeKubeJSPlugin` 绑定到 `kubejs` mod ID；该文件本身也维持 KubeJS 未安装时不加载插件的隔离。

### `assets/superreforge`

- `blockstates/reforger.json`：把四个 `facing` 状态映射到同一 3D 模型的 0/90/180/270 度旋转；改状态键或角度会使世界模型与碰撞/动态锻锤方向不一致。
- `models/block/reforger.json`：锻台方块模型及纹理引用；改几何/纹理 ID 只影响渲染。
- `models/item/reforger.json`、`common_reforge_stone.json`、`refined_reforge_stone.json`、`supreme_reforge_stone.json`、`enhancement_stone_4.json` 至 `enhancement_stone_6.json`：对应方块物品和六级强化石模型。它们应与 `ModItems` 的注册 ID 对齐，否则物品会显示为缺失模型；4–6 级暂用原版图标且不内置获取配方。
- `models/item/forge_hammer.json`：方块实体动画专用的原创 3D 动力锻锤，四个具名元素（handle/socket/head/cap）只在边界相接；模型由共享固定枢轴旋转，`fixed` 不再叠加倾斜。修改元素范围必须同步 `ReforgerHammerGeometry`，否则 SAT 穿模、接触高度测试与实际烘焙模型会失配。
- `lang/en_us.json`、`lang/zh_cn.json`：英文/简体中文词条，覆盖独立页签、锻台、滚动/悬停说明、失败提示、等级和示例词条名；八个默认等级目前统一显示为 “Level 1” 至 “Level 8” / “等级 1” 至 “等级 8”。改键名会影响 JSON `translate`、屏幕与 mixin 名称显示。

### `data/minecraft`

- `tags/block/needs_iron_tool.json`：将锻台纳入铁镐需求标签；改它影响挖掘等级判定。
- `tags/block/mineable/pickaxe.json`：将锻台纳入镐可挖标签；改它影响采掘效率/掉落。

### `data/superreforge`

- `loot_table/blocks/reforger.json`：锻台方块战利品表；方块实体额外库存/待完成结果由 Java 破坏路径掉落，不能把这些动态内容误写成固定 loot。
- `recipe/reforger.json`：熔核锻台合成配方。改输入或结果 ID 会影响生存获取方式。
- `recipe/common_reforge_stone.json`、`refined_reforge_stone.json`、`supreme_reforge_stone.json`：三种媒介的合成配方，对应下列媒介定义与 `ModItems` 注册。

### `data/superreforge/superreforge`（可重载定义）

- `levels/worn.json`、`common.json`、`fine.json`、`rare.json`、`epic.json`、`legendary.json`、`mythic.json`、`divine.json`：八个等级定义，保留原资源 ID、rank 与颜色，通过本地化键显示为数值等级 1–8；这些 ID 被媒介、词条和旧物品引用，删除/改名会破坏引用与存档兼容。
- `item_types/axe.json`、`bow.json`、`crossbow.json`、`curio.json`、`mace.json`、`sword.json`、`trident.json`：七类目标物品选择器。`sword.json` 例含 `minecraft:swords` 标签与 `minecraft:stick` 单品；`curio.json` 使用可选 Curios 匹配时，在无 Curios 环境不会由核心硬加载该依赖。改 include/exclude 会改变全部词条候选池。
- `catalysts/common_reforge_stone.json`、`refined_reforge_stone.json`、`supreme_reforge_stone.json`：三种媒介的物品匹配、数量/经验、允许等级权重及类型限制。普通石例匹配 `superreforge:common_reforge_stone`，消耗 1、经验 3、覆盖 worn/common/fine/rare；权重是相对值，不要求和为 1。
- `modifiers/melee_1.json` 至 `melee_8.json`：近战词条样本，分别落在八个等级；`melee_1` 适用 sword/axe/trident/mace，例含主手攻击伤害和攻速的 `add_multiplied_base` 效果。
- `modifiers/ranged_1.json` 至 `ranged_8.json`：远程武器词条样本，面向 bow/crossbow 等类型；改属性、槽位、权重或等级会改变候选概率和原版属性。
- `modifiers/curio_1.json` 至 `curio_8.json`：Curios 词条样本，面向 `curio` 类型并使用 `curios:any` 槽位；其定义可被读取，但实际 Curios 属性桥只在可选依赖存在时初始化。

## `docs` 与发布入口

- `README.md`：安装、快速开始和全部公开指南入口；现在分别链接 Datapack 与 KubeJS 教程。修改入口时必须保持仓库内相对链接可解析。
- `docs/DATAPACK_API.md`、`docs/KUBEJS_API.md`：字段/绑定的完整参考，并交叉链接两条教程；API 签名、Codec 或示例路径变化时需同步。
- `docs/DATAPACK_TUTORIAL.md`：面向数据包作者的独立中文实作流程，覆盖 pack 布局、四类严格 JSON、JSONC 阅读版、相对权重、`curios:any`、`/reload` 与常见错误；示例代码块必须保持可解析。
- `docs/KUBEJS_TUTORIAL.md`：面向脚本作者的独立中文实作流程，覆盖脚本放置、定义 API、最小教学/完整战斗/纯 Curios 脚本三选一、同层重复 ID、相对权重、可选 Attribute、持久化阶段、成本公式与排错。
- `docs/FILE_REFERENCE.md`：本文件；发布审查用它核对每个源码、资源、测试、示例与指南的责任边界。

## `examples/kubejs`

- `examples/kubejs/superreforge_combat_attributes.js`：可复制的八级战斗配置；可执行 `SR_COMBAT_SPEC` UTF-8 JSON 表由循环直接消费，为近战、远程、头盔、胸甲、护腿、靴子、工具与 Curios 八个池各生成八级、共 64 个词条，并演示 Critical Strike、Ranged Weapon API 与 Curios Attribute。四种护甲分别绑定单独 tag、类型和槽位；玩家应按教程与其他定义脚本三选一，且只有安装相应 Attribute 提供模组后才启用这份完整脚本。
- `examples/kubejs/superreforge_curio_progression.js`：MUTUO 的纯 Curios 八级配置；解析 `SR_CURIO_SPEC` 注册 32 条固定饰品词条与六级强化石精确权重，用零权重/空气类型覆盖内置 24 条词条，并统一把效果限制为 `curios:any`。它必须替换而非叠加最小教学、完整战斗或旧自定义定义脚本。
- `superreforge_definitions.js`：最小 KubeJS 定义示例，演示四类定义和脚本谓词；不能与完整战斗脚本同时注册相同八级 ID。
- `superreforge_progression.js`：全服阶段开关与持久化进度示例。

## `src/test/java/com/mutuo/superreforge`

- `BootstrapMetadataTest.java`：检查构建/模组元数据相关约定。
- `CombatKubeJsExampleTest.java`：解析完整战斗脚本的真实 `SR_COMBAT_SPEC`，结构化验证 8 个 UTF-8 等级名、8 个池/64 个 ID、相对权重、四种护甲 tag 与精确槽位、Critical Strike 与仅远程池可用的 Ranged Weapon API operation。
- `CurioProgressionKubeJsExampleTest.java`：解析纯饰品脚本的 `SR_CURIO_SPEC`，验证 8 级 × 4 条固定词条、六种强化石的精确等级权重、外部 Attribute、重复交互距离去重、内置词条停用与 `curios:any` 限制。
- `DocumentationContractTest.java`：检查发布文件/真实 Markdown 链接、双教程工作流，以及 `CHANGELOG.md` 和本文件必须记录的原版 GUI、Curios、共享几何、单行日志与锻锤发布契约。

### `api`

- `ScriptDefinitionCollectorTest.java`：验证脚本收集器对定义、谓词和阶段的汇总行为。

### `block`

- `PendingReforgeTest.java`：验证 pending tick 递减与 ready 边界。
- `ReforgerBlockTest.java`：验证真实注册锻台具有默认北向和四个水平朝向。
- `ReforgerLayoutTest.java`：验证 176px 背包面板居中、共享 `PlayerSlotPosition` 恰有 36 个唯一位置且与菜单同源、9 列索引/坐标顺序、16px 内容与 18px 槽框的对称边距，以及日志、滚动条和截断警告命中区域。

### `client`

- `ModifierTooltipFormatterTest.java`：验证固定值、百分比范围、外部 Attribute、三种运算/槽位说明和同步定义缺失回退。
- `ReforgerHammerGeometryTest.java`：直接解析锤子与锻台模型 JSON，验证四个锤部件只边界接触、`TransformPlan` 固定枢轴/步骤顺序、四向 yaw，以及静止/抬起/接触/回弹/复位各关键帧通过 SAT 检查与全部 22 个真实锻台盒无正体积相交。
- `ReforgerLogModelTest.java`：逐一验证全部最终词条单行的等级 Component、词条 ID/Component、服务端概率、连续 top/height，不因视口截断，并覆盖滚轮、滑块和边界钳制。
- `ReforgerRenderStateTest.java`：验证 20 tick 内 -45° 静止、-75° 抬起、-45° 接触、-47° 回弹、复位关键帧和熔核峰值。
- `ReforgerScreenTest.java`：验证 pending、缺少报价、报价失败或定义代次过期时原版重铸按钮保持禁用，只有当前代次的成功服务端报价可启用按钮。

### `compat/curios`

- `CuriosContextPolicyTest.java`：回归验证 Curios 传入 null entity 时分类为 `SYNTHETIC_CLIENT`，防止创意搜索/JEI tooltip 再触发空实体崩溃。

### `config`

- `SuperReforgeConfigTest.java`：验证配置快照、默认值和配置约束。

### `definition`

- `DefaultResourcesTest.java`：读取默认数据包并用生产 Codec 检查等级、类型、词条、媒介引用，验证默认 rank 1–8 的数字本地化 Component，同时验证 3D 模型元素和四向 blockstate。
- `DefinitionCodecTest.java`：验证定义 JSON Codec 的编码/解码。
- `DefinitionReloadListenerTest.java`：使用原版 `MultiPackResourceManager` 回归验证 reload 目录参数合法，防止真实服务器在 datapack 阶段中止。
- `DefinitionManagerTest.java`：验证 datapack/脚本层的原子发布、覆盖、失败保留旧快照，以及客户端显示镜像清理不会误删同进程服务端定义。
- `ScriptDefinitionPublisherTest.java`：验证首次开服时暂缓的 KubeJS bundle 会在 datapack 就绪后整体发布，并验证正常运行期的无效脚本会被拒绝且不进入待重试队列。
- `DefinitionValidatorTest.java`：验证不合法权重、引用、效果与媒介被拒绝。

### `item`

- `AttributeRefreshServiceTest.java`：验证 reload 清理器只识别本模组拥有的 `superreforge:effect/*` Attribute modifier。
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
- `SelectorHooksTest.java`：验证 KubeJS 谓词抛出运行时异常时安全返回不匹配，不会中断重铸流程。

### `network`

- `DefinitionSyncPayloadTest.java`：验证大型显示快照分块、乱序原子组装以及成功/失败/陈旧 ACK 的服务端门控。
- `ReforgePreviewPayloadTest.java`：验证等级/单级词条超过网络上限时设置截断标记，小预览保持完整，并验证标记的网络往返。

### `registry`

- `ModCreativeTabsTest.java`：验证独立页签的四个公开物品及其顺序、动画锤子隐藏和中英文标题资源。

## 根构建文件

- `build.gradle`：Java 21 + NeoForge ModDevGradle 构建、资源生成、JUnit、客户端/服务端/Data/GameTest 运行配置；Curios/KubeJS 正式依赖均为 `compileOnly`，不会被打包，`withCuriosRuntime`/`withKubeJSRuntime` 属性只供本地兼容矩阵临时加入运行类路径。改依赖范围会改变无可选模组时的可加载性；改 `unitTest` 会影响这些真实 Minecraft 类型测试。
- `settings.gradle`：Gradle 插件仓库与固定根项目名 `superreforge`。改名会改变构建坐标/IDE 项目名。
- `gradle.properties`：Minecraft 1.21.1、NeoForge、Parchment、模组坐标/版本、Curios/KubeJS 编译版本及 Gradle 性能参数。改版本必须与 `build.gradle` 和兼容源码 API 一并核对。
- `gradlew`、`gradlew.bat`：Unix/Windows Gradle Wrapper 启动器。一般不手改业务逻辑；升级 wrapper 时与 `gradle/wrapper` 元数据一起处理。
