# Super Reforge MC 百科事实核对表

本表只记录当前 `HEAD`（`8506b07`）已提交且已有项目验证证据的事实；正文中的每项均附本地来源或核对命令。这里的“已实现”不代替作者对最终发布 JAR 的投稿前人工确认。（`git rev-parse --short HEAD`、`git status --short`）

## 发布元数据

- 中文名称为超级重铸，原名为 Super Reforge；作者为 MUTUO，模组版本为 `0.1.0`。（`gradle.properties`、`docs/superpowers/plans/2026-08-01-mcmod-page-content.md`）
- 目标游戏版本为 Minecraft `1.21.1`；编译与验证基线为 NeoForge `21.1.244`。（`gradle.properties`、`src/main/templates/META-INF/neoforge.mods.toml`）
- NeoForge 与 Minecraft 都是双端必需依赖；其中 NeoForge 版本范围为 `[21.1.244,)`，Minecraft 版本范围由 `${minecraft_version_range}` 固定。（`src/main/templates/META-INF/neoforge.mods.toml`）
- 当前变更日志将 `0.1.0` 记为 2026-08-01 的首次发布。（`CHANGELOG.md`）

## 已实现的玩家功能

- 已有熔核锻台、锻造日志界面、服务端权威概率预览和约 20 tick 的锻造动画。（`CHANGELOG.md`、`docs/CONFIGURATION.md`）
- 重铸可为物品动态显示前缀；铁砧重命名不会被写死或丢失。（`README.md`）
- 一个词条可含多个固定值或范围随机 Attribute，并可分别指定运算方式和装备槽位。（`README.md`、`docs/DATAPACK_API.md`）
- 命中的多个物品类型会合并词条池，并按词条 ID 去重；媒介权重按 `weight / sum(weight)` 自动归一化。（`README.md`、`docs/DATAPACK_API.md`）
- 经验消耗可关闭，并支持 `LEVELS` 与 `POINTS` 两种支付方式。（`README.md`、`docs/CONFIGURATION.md`）
- 锻造日志保留完整的服务端预览行，玩家可用滚轮浏览列表。（`src/main/java/com/mutuo/superreforge/client/ReforgerLogModel.java`、`src/main/java/com/mutuo/superreforge/client/ReforgerScreen.java`、`src/test/java/com/mutuo/superreforge/client/ReforgerLogModelTest.java`）
- 概率列表的滚动滑块可以直接拖动，并会在内容边界内钳制位置。（`src/main/java/com/mutuo/superreforge/client/ReforgerLogModel.java`、`src/main/java/com/mutuo/superreforge/client/ReforgerScreen.java`、`src/test/java/com/mutuo/superreforge/client/ReforgerLogModelTest.java`）
- 悬停词条行会显示词条 ID，以及每条效果的 Attribute 资源 ID、固定值或范围、运算方式和槽位。（`src/main/java/com/mutuo/superreforge/client/ModifierTooltipFormatter.java`、`src/test/java/com/mutuo/superreforge/client/ModifierTooltipFormatterTest.java`）
- 预览达到网络展示安全上限时，界面会明确显示截断警告；该上限只限制发送给客户端的展示负载，不改变服务端候选池或抽取判定。（`src/main/java/com/mutuo/superreforge/network/ReforgePreviewPayload.java`、`src/main/java/com/mutuo/superreforge/client/ReforgerScreen.java`、`src/test/java/com/mutuo/superreforge/network/ReforgePreviewPayloadTest.java`）
- 动画锻锤已校正为锤头向下的竖直姿态，并保持抬起、接触、回弹和复位动画；这是视觉与几何修正，不改变重铸规则。（`src/main/java/com/mutuo/superreforge/client/ReforgerHammerGeometry.java`、`src/test/java/com/mutuo/superreforge/client/ReforgerHammerGeometryTest.java`、`src/test/java/com/mutuo/superreforge/client/ReforgerRenderStateTest.java`）
- 已有独立 `Super Reforge` 创造模式页签，依次公开熔核锻台、普通重铸石、精炼重铸石和至高重铸石；动画专用锻锤不作为普通物品展示。（`src/main/java/com/mutuo/superreforge/registry/ModCreativeTabs.java`、`src/test/java/com/mutuo/superreforge/registry/ModCreativeTabsTest.java`）

## 默认内容

- 内置 8 个品质等级：`worn`、`common`、`fine`、`rare`、`epic`、`legendary`、`mythic`、`divine`。（`rg --files src/main/resources/data/superreforge/superreforge/levels`）
- 内置 7 个物品类型：`sword`、`axe`、`bow`、`crossbow`、`trident`、`mace`、`curio`。（`rg --files src/main/resources/data/superreforge/superreforge/item_types`）
- 内置 24 条示例词条，分为近战、远程和 Curios 三组各 8 条。（`rg --files src/main/resources/data/superreforge/superreforge/modifiers`、`README.md`）
- 内置 3 种重铸媒介：`common_reforge_stone`、`refined_reforge_stone`、`supreme_reforge_stone`。（`rg --files src/main/resources/data/superreforge/superreforge/catalysts`）
- 默认内容还包含由 22 个几何部件组成的熔核锻台与独立 3D 锻锤动画。（`README.md`）

## 可选兼容

- 对外兼容范围只写 Curios 9.x；项目验证使用 `9.5.1+1.21.1`。元数据中的 `[9,)` 只是接受版本的下限，不代表其他 Curios 代际已经验证。（`gradle.properties`、`src/main/templates/META-INF/neoforge.mods.toml`、`run/logs/2026-08-01-3.log.gz`）
- 对外兼容范围只写 KubeJS 2101；项目验证使用 `2101.7.2-build.372`。元数据中的 `[2101,)` 只是接受版本的下限，不代表其他 KubeJS 代际已经验证。（`gradle.properties`、`src/main/templates/META-INF/neoforge.mods.toml`、`run/logs/2026-08-01-2.log.gz`）
- 不安装 Curios 或 KubeJS 时，原版武器和数据包重铸功能仍可运行。（`README.md`、`src/main/templates/META-INF/neoforge.mods.toml`）

## 数据包与 KubeJS 能力

- 数据包会在资源 reload 时读取 `levels`、`item_types`、`modifiers`、`catalysts` 四类定义；四者必须通过交叉校验才会发布新快照。（`docs/DATAPACK_API.md`）
- 正式数据包文件必须使用 `.json`；`.jsonc` 仅用于阅读和抄写，不能直接加载。（`README.md`、`examples/datapack/`）
- KubeJS 仅在 `server_scripts` 提供 `SuperReforge`，可添加四类定义、谓词和全服阶段；同 ID 的脚本定义覆盖数据包定义。（`docs/KUBEJS_API.md`、`examples/kubejs/`）
- 阶段状态存储在服务器世界数据中；多个活动阶段时只采用最高优先级阶段。（`docs/KUBEJS_API.md`、`examples/kubejs/superreforge_progression.js`）

## 服务器配置

- 配置文件位于世界目录的 `serverconfig/superreforge-server.toml`，由服务器决定且不应由客户端本地配置覆盖。（`docs/CONFIGURATION.md`）
- 默认设置为：启用经验、`LEVELS` 经验模式、关闭自动首词条、自动媒介为 `superreforge:common_reforge_stone`、显示 Attribute 行、动画 20 tick、创造模式免费、缺失定义策略 `REMOVE`。（`docs/CONFIGURATION.md`）
- `animationTicks` 可配置范围为 1 至 1200；创造模式免费仍须放入能匹配媒介定义的物品。（`docs/CONFIGURATION.md`）

## 最终项目验证与写作边界

- `clean test build` 已完成，现有测试结果共 79 项，失败、错误与跳过均为 0。（`build/test-results/test/*.xml`；汇总各 `testsuite` 的 `tests`、`failures`、`errors`、`skipped`）
- 专用服务器已完成四种启动组合：仅核心、Curios 9.x、KubeJS 2101、Curios 9.x + KubeJS 2101；四次均加载定义并启动至 `Done`，KubeJS 组合还成功发布脚本层和冒烟阶段。（`run/logs/2026-08-01-4.log.gz`、`run/logs/2026-08-01-3.log.gz`、`run/logs/2026-08-01-2.log.gz`、`run/logs/2026-08-01-1.log.gz`）
- NeoForge 客户端冒烟检查已完成至模组初始化、资源重载、音频启动和 GUI 纹理图集创建。（`run/logs/latest.log`）
- 上述记录是当前项目工作树的验证证据；不得把它扩展成所有硬件、整合包、其他可选模组版本或最终发布 JAR 均已实测，也不得使用无来源的绝对性或时效性声明。（`.superpowers/sdd/2026-08-01-mcmod-page-content/progress.md`、`.superpowers/sdd/2026-08-01-mcmod-page-content/task-1-brief.md`）

## 投稿前需由作者人工确认

- 确认最终发布包的文件名、下载地址、支持语言、许可证展示文案与截图素材。（待作者提供：`build/libs/superreforge-0.1.0.jar`、`docs/RELEASE_ASSETS.md`）
- 使用最终准备上传的 JAR，在作者的目标客户端、专用服务器与实际整合包组合中人工复核启动、界面交互和联机表现；这项投稿前确认不由上述开发工作树验证代替。（待作者提供：最终发布 JAR 的人工验收记录）
- 确认投稿页面的游戏版本与依赖展示是否与实际上传的 JAR 完全一致。（待作者提供：`build/libs/superreforge-0.1.0.jar` 内的 `META-INF/neoforge.mods.toml`）
