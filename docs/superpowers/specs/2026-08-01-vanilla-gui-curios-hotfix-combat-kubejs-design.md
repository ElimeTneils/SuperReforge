# Super Reforge 原版界面、Curios 热修复与战斗 KubeJS 内容设计

日期：2026-08-01  
状态：用户已确认  
适用版本：Minecraft 1.21.1、NeoForge 21.1.244

## 1. 目标与交付顺序

本次迭代在不改变既有数据层优先级、持久化格式和服务端权威抽取规则的前提下，完成以下工作：

1. 首先交付一个可直接放入 `kubejs/server_scripts/` 的战斗词条脚本。
2. 修复 Curios 在创造栏、JEI 或异步搜索提示中传入空实体上下文时导致的客户端崩溃。
3. 将重铸台 GUI 改成用户选定的 A 方案：严格贴近原版 Minecraft 容器。
4. 把默认等级显示名改成“等级 1”至“等级 8”，保持内部 ID 不变。
5. 恢复上一版 45° 斜置锤子，修复模型部件重叠和动画穿模。
6. 更新可安装 JAR、GitHub PR，以及 Datapack 与 KubeJS 两篇独立教程。

首个脚本交付不等待 GUI 与 JAR 完成；它必须能在当前 `0.1.0` API 上加载，方便用户先测试词条内容。

## 2. 保持不变的边界

- Super Reforge 不注册任何 Attribute。
- Curios、KubeJS、Critical Strike 和 Ranged Weapon API 仍是可选依赖。
- 缺少第三方 Attribute 时只跳过对应效果并记录警告，不使整条词条、其他效果或核心重铸功能失效。
- KubeJS 同 ID 定义继续覆盖 datapack；全局 config 继续控制全局行为。
- 物品仍保存词条 ID 与随机种子；已有物品按当前定义动态重新解析。
- 等级总数保持 8，不增加第 9 至第 12 级。
- 权重继续自动按 `weight / sum(weight)` 归一化。

## 3. 首个交付：战斗 Attribute KubeJS 脚本

### 3.1 文件与可用方式

新增：

`examples/kubejs/superreforge_combat_attributes.js`

文件开头明确标注目标位置：

`kubejs/server_scripts/superreforge_combat_attributes.js`

脚本只使用已经公开的顶层 `SuperReforge` API，并给每个定义块、Attribute 单位、运算方式、槽位和权重添加中文注释。用户复制后执行 `/reload` 即可发布；脚本不得要求修改 Java 代码或注册新 Attribute。

### 3.2 八级显示

脚本以相同 ID 覆盖内置八级，仅修改显示 Component 和保持既有 rank：

| rank | 内部 ID | 默认显示 |
| ---: | --- | --- |
| 1 | `superreforge:worn` | 等级 1 |
| 2 | `superreforge:common` | 等级 2 |
| 3 | `superreforge:fine` | 等级 3 |
| 4 | `superreforge:rare` | 等级 4 |
| 5 | `superreforge:epic` | 等级 5 |
| 6 | `superreforge:legendary` | 等级 6 |
| 7 | `superreforge:mythic` | 等级 7 |
| 8 | `superreforge:divine` | 等级 8 |

保持 ID 能保证现有媒介引用和已有重铸物品继续工作。名称、颜色和 rank 仍可由整合包作者再次用 KubeJS 同 ID 覆盖。

### 3.3 物品类型

脚本复用现有 `sword`、`axe`、`bow`、`crossbow`、`trident`、`mace` 和 `curio` 类型，并补充：

- 护甲：匹配原版头盔、胸甲、护腿和靴子标签。
- 工具：匹配镐、锹和锄标签；斧已由现有 `axe` 类型处理，避免重复定义。

物品仍可同时命中多个类型；候选词条按词条 ID 去重。新增 selector 优先使用标准 item tag，只有 tag 无法表达的 KubeJS 自定义集合才使用 `addPredicate`。

### 3.4 Critical Strike 语义

已对照用户安装的 `critical_strike-neoforge-1.0.4+1.21.1`：

- `critical_strike:chance` 的内部基准值是 `100`，模组用 `(当前值 - 100) / 100` 得到 0～1 概率。
- `critical_strike:damage` 的内部基准值是 `100`，模组用 `当前值 / 100` 得到伤害倍率。

因此两个 Attribute 都使用：

`operation: "add_multiplied_base"`

示例 `amount: 0.05` 代表增加 5% 暴击率或 5 个百分点的暴击伤害倍率；所有 chance 配置范围保持在 `0` 至 `1`。脚本不得误用 `add_value: 0.05`，否则实际只会改变基准值的 0.05，而不是 5%。

Critical Strike 效果用于近战武器、远程武器和 Curios 饰品；护甲与工具可以按等级获得较低幅度的通用暴击效果，使这些类别也拥有完整八级候选，但不强制每条词条同时包含两种暴击 Attribute。

### 3.5 Ranged Weapon API 语义

已对照用户安装的 `ranged_weapon_api-neoforge-2.3.3+1.21.1`：

- `ranged_weapon:damage`：远程伤害，基础数值由武器以固定加值提供；百分比增强使用 `add_multiplied_total`。
- `ranged_weapon:velocity`：箭矢速度，基础数值由武器提供；百分比增强使用 `add_multiplied_total`。
- `ranged_weapon:haste`：上弦加速，以 `100` 为基准；正向百分比使用 `add_multiplied_base`。
- `ranged_weapon:pull_time`：上弦时间倍率，数值越低越快；缩短 5% 使用 `add_multiplied_total` 与 `amount: -0.05`。

这些效果仅用于 `bow` 和 `crossbow` 类型，并作用于 `mainhand`。每级至少提供一个弓弩候选；高等级可以在一条前缀中组合远程伤害、上弦速度和箭矢速度，但数值区间必须保持有限且不使 pull time 降到非正数。

### 3.6 八级内容密度

“更多等级”按用户确认解释为：等级总数仍为 8，但饰品、近战武器、弓弩、护甲和工具五类内容都覆盖完整 1～8 级，而不是只给部分类型提供少量等级。

每类每级至少有一个可抽取词条。脚本可以通过数组与循环减少机械重复，但生成的 ID、名称、数值、权重和 Attribute 列表必须清晰可查。注释解释各数组列的含义以及如何复制一行添加自定义词条。

## 4. Curios 空实体崩溃修复

### 4.1 根因

崩溃报告指向：

`CuriosCompat.onCurioAttributes(CuriosCompat.java:51)`

Curios 9.5.1 在创造栏、JEI 搜索索引和无玩家 Tooltip 上下文中会创建 `SlotContext(identifier, null, ...)`。当前代码在第 51 行和第 64 行直接调用 `entity().level()`，从而抛出 `NullPointerException`。

BountifulBaubles 不是空指针的实现来源；它提供 Curios 物品后触发了这条提示生成路径。修复责任属于 Super Reforge 的 Curios 兼容层。

### 4.2 修复行为

- 先保存 `SlotContext` 与可空 `LivingEntity`，只读取一次。
- 实体非空且位于服务端时，继续执行 `ModifierLifecycle.reconcile`。
- 实体为空时视为合成的客户端展示上下文：不得迁移物品数据或取随机数，但仍可从物品已保存的 ID/种子解析现有词条并生成 Attribute 提示。
- 客户端 `showAttributeLines` 与 effect 自身的 `show_in_tooltip` 继续控制提示显示。
- `identifier` 与 `index` 仍参与稳定 Curios modifier ID；不得因为实体为空而生成随机 ID。

修复不能吞掉其他异常，也不能用宽泛 `try/catch` 隐藏定义错误。

## 5. 严格原版风格 GUI（方案 A）

### 5.1 布局

使用用户在视觉草图中选择的 A 方案：

- 浅灰原版容器底板。
- 原版高光/阴影边界和标准槽位框。
- 原版 `Button` 外观，不绘制自定义大灰块冒充按钮。
- 左上为目标、媒介、成本和重铸按钮。
- 右上为可获得前缀和滚动条。
- 下方严格绘制 9×3 背包与 9 格快捷栏，共 36 个玩家槽位。

玩家背包面板使用标准 176 像素宽度；槽位从面板左侧标准内边距开始。当前额外约一列宽的背景必须删除，右边距与左边距对称。

### 5.2 日志语义

不再把等级与词条绘制为两条近似同级的记录。每个最终候选只占一行：

`等级 5　辉光　30.0%`

概率使用服务端已经计算的最终联合概率。滚轮、拖动滚动条、裁剪区域、预览截断警告和悬停 Attribute 详情全部保留。悬停目标是整条最终候选行。

## 6. 锤子姿态与模型

- 静止状态恢复竖直改版之前的 45° 斜置方向。
- 锤头、铜箍与木柄的立方体不能共享体积；当前铜箍与铁锤头重叠 1 个模型像素导致 z-fighting，必须从源模型坐标消除。
- 动画绕固定锤架连接点抬起和下砸，不再把整把锤子沿世界 Y 轴穿过支架。
- 四种方块朝向共享同一局部枢轴和角度曲线。
- 接触帧锤面只到达中央砧面，不进入砧面或左右工作台边。

静止角、枢轴、锤面边界和接触角集中到纯几何计算类，并用测试覆盖；最终必须从正面、侧面与俯视角度进行客户端视觉验证。

## 7. 默认等级资源

内置八个 `levels/*.json` 只修改显示 Component：

- 中文 fallback 与 `zh_cn` 翻译改为“等级 1”至“等级 8”。
- 英文改为 `Level 1` 至 `Level 8`。
- rank、资源 ID、颜色和所有引用保持不变。

现有物品与媒介继续通过原 ID 解析，不进行数据迁移。

## 8. 教程

在代码与脚本验证后新增两篇独立教程：

- `docs/DATAPACK_TUTORIAL.md`
- `docs/KUBEJS_TUTORIAL.md`

Datapack 教程覆盖目录、严格 JSON、等级、类型、词条、媒介、Curios 和排错。KubeJS 教程覆盖四类 add 方法、谓词、同 ID 覆盖、阶段持久化、Critical Strike/Ranged Weapon 示例和排错。README 与现有 API 文档增加入口。

正式 JSON 不写非法注释；教程同时给出带解释的 JSONC 阅读版和可直接加载的严格 JSON。所有 JavaScript 示例带中文注释。

## 9. 测试与验证

实施遵循先失败测试、后最小修复：

1. KubeJS 脚本文本契约测试：八级 ID、五类池、六个第三方 Attribute、运算方式、槽位和注释均存在。
2. KubeJS 服务器 reload：0 errors、0 warnings，并发布脚本层定义。
3. Curios 合成空实体上下文回归测试：不崩溃、不执行服务端 reconcile、仍能生成允许显示的词条 Attribute。
4. 真实 Curios 玩家上下文测试：服务端刷新与属性应用保持有效。
5. GUI 布局测试：36 个槽位全部位于 176 像素面板内，左右边距对称，不存在第十列。
6. 日志模型测试：一行对应一个最终词条，显示等级数字、前缀与联合概率，滚动边界保持正确。
7. 锤子几何测试：部件不重叠，四向枢轴一致，静止和接触帧不穿模。
8. 使用 Curios 9.5.1、BountifulBaubles 1.2.5、JEI 与用户同类创造栏搜索路径进行客户端复现测试。
9. 运行核心、Curios、KubeJS、Curios+KubeJS 四种服务端组合以及完整 Gradle 测试。
10. 构建新 JAR，检查不捆绑 Curios、KubeJS、Critical Strike、Ranged Weapon API 或自定义 Attribute 资源，并记录 SHA-256。

## 10. 完成标准

- 首个 KubeJS 文件可直接复制并成功 reload，完整覆盖五类内容的 1～8 级。
- 第三方 Attribute 使用与实际模组版本一致的百分比语义。
- 创造栏和 JEI 构建 Curios Tooltip 时不再因空实体崩溃。
- GUI 与视觉草图 A 一致，物品栏没有多余列，日志没有“史诗/辉光”双品质歧义。
- 45° 锤子在静止与动画期间没有 z-fighting 或结构穿模。
- 新 JAR、两篇教程与全部源代码已更新到 GitHub PR。

