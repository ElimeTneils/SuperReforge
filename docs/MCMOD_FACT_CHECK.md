# Super Reforge MC 百科事实核对表

本表只记录当前 `HEAD`（`fbe1e1d`）已有的可核对事实；正文中的每项均附本地来源或核对命令。未跟踪、未提交内容不构成已发布功能。（`git log -8 --oneline`、`git status --short`）

## 发布元数据

- 中文名称为超级重铸，原名为 Super Reforge；作者为 MUTUO，模组版本为 `0.1.0`。（`gradle.properties`、`docs/superpowers/plans/2026-08-01-mcmod-page-content.md`）
- 目标游戏版本为 Minecraft `1.21.1`；编译与验证基线为 NeoForge `21.1.244`。（`gradle.properties`、`src/main/templates/META-INF/neoforge.mods.toml`）
- NeoForge 与 Minecraft 都是双端必需依赖；其中 NeoForge 版本范围为 `[21.1.244,)`，Minecraft 版本范围由 `${minecraft_version_range}` 固定。（`src/main/templates/META-INF/neoforge.mods.toml`）
- 当前变更日志将 `0.1.0` 记为 2026-08-01 的首次发布。（`CHANGELOG.md`）

## 已实现的玩家功能

- 已有熔核锻造台、锻造日志界面、服务端权威概率预览和约 20 tick 的锻造动画。（`CHANGELOG.md`、`docs/CONFIGURATION.md`）
- 重铸可为物品动态显示前缀；铁砧重命名不会被写死或丢失。（`README.md`）
- 一个词条可含多个固定值或范围随机 Attribute，并可分别指定运算方式和装备槽位。（`README.md`、`docs/DATAPACK_API.md`）
- 命中的多个物品类型会合并词条池，并按词条 ID 去重；媒介权重按 `weight / sum(weight)` 自动归一化。（`README.md`、`docs/DATAPACK_API.md`）
- 经验消耗可关闭，并支持 `LEVELS` 与 `POINTS` 两种支付方式。（`README.md`、`docs/CONFIGURATION.md`）

## 默认内容

- 内置 8 个品质等级：`worn`、`common`、`fine`、`rare`、`epic`、`legendary`、`mythic`、`divine`。（`rg --files src/main/resources/data/superreforge/superreforge/levels`）
- 内置 7 个物品类型：`sword`、`axe`、`bow`、`crossbow`、`trident`、`mace`、`curio`。（`rg --files src/main/resources/data/superreforge/superreforge/item_types`）
- 内置 24 条示例词条，分为近战、远程和 Curios 三组各 8 条。（`rg --files src/main/resources/data/superreforge/superreforge/modifiers`、`README.md`）
- 内置 3 种重铸媒介：`common_reforge_stone`、`refined_reforge_stone`、`supreme_reforge_stone`。（`rg --files src/main/resources/data/superreforge/superreforge/catalysts`）
- 默认内容还包含由 22 个几何部件组成的熔核锻造台与独立 3D 锻锤动画。（`README.md`）

## 可选兼容

- Curios 为双端可选依赖，版本范围为 `[9,)`；其配置版本为 `9.5.1+1.21.1`。（`gradle.properties`、`src/main/templates/META-INF/neoforge.mods.toml`）
- KubeJS 为双端可选依赖，版本范围为 `[2101,)`；其配置版本为 `2101.7.2-build.372`。（`gradle.properties`、`src/main/templates/META-INF/neoforge.mods.toml`）
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

## 尚未完成或尚未验证，禁止写入成稿

- 工作树中存在未跟踪的 `ReforgerLayout.java`、`ModifierTooltipFormatter.java` 及其两份测试；它们涉及更宽界面中的背包/日志布局及候选词条悬浮信息，未提交，不能作为已发布功能宣传。（`git status --short`、`src/main/java/com/mutuo/superreforge/block/ReforgerLayout.java`、`src/main/java/com/mutuo/superreforge/client/ModifierTooltipFormatter.java`、`src/test/java/com/mutuo/superreforge/block/ReforgerLayoutTest.java`、`src/test/java/com/mutuo/superreforge/client/ModifierTooltipFormatterTest.java`）
- 本次仅完成仓库静态事实核对，未在真实 NeoForge 客户端、专用服务器、Curios 或 KubeJS 环境中进行运行时验收；兼容性和联机表现不得扩展为“已实测”。（`git status --short`、`git diff --check`、`rg --files src/main/resources/data/superreforge/superreforge`）
- 不得使用无来源的绝对性或时效性声明。（`rg -n '待补充|占位符|稍后完成|最新版本|旧版本|兼容所有|支持所有' docs/MCMOD_FACT_CHECK.md`）

## 投稿前需由作者人工确认

- 确认最终发布包的文件名、下载地址、支持语言、许可证展示文案与截图素材。（待作者提供：`build/libs/superreforge-0.1.0.jar`、`docs/RELEASE_ASSETS.md`）
- 确认是否已在目标客户端、专用服务器及安装/未安装 Curios、KubeJS 的组合环境完成实机测试。（待作者提供：`docs/TESTING.md` 中的组合验收记录）
- 确认投稿页面的游戏版本与依赖展示是否与实际上传的 JAR 完全一致。（待作者提供：`build/libs/superreforge-0.1.0.jar` 内的 `META-INF/neoforge.mods.toml`）
