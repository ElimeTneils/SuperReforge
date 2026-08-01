# Super Reforge

`Super Reforge` 是 MUTUO 制作的 Minecraft 1.21.1 / NeoForge 21.1.244 数据驱动前缀重铸模组。它提供原创的 3D 熔核锻台、服务端权威概率预览和约一秒锻造动画，但不注册任何自定义 Attribute；原版以及其他模组已经注册的 Attribute 都可以直接按资源 ID 使用。

## 功能

- 只在物品当前名称前动态显示一个前缀，例如“传说 Excalibur”；铁砧重命名不会写死或丢失前缀。
- 一条词条可包含多个固定值或范围随机 Attribute，分别选择运算方式、主副手、护甲槽或 `curios:any`。
- 物品可同时属于多个自定义类型，词条池按词条 ID 合并去重；默认提供 sword、axe、bow、crossbow、trident、mace、curio。
- 媒介以任意非负数字作为相对权重，系统自动用 `weight / sum(weight)` 计算真实概率。
- 经验消耗可关闭，并在全局配置中选择 `LEVELS` 或 `POINTS`。
- KubeJS 可覆盖同 ID datapack 定义，并持久化全服进度阶段；多个激活阶段只采用最高优先级。
- Curios 与 KubeJS 都是可选依赖；不安装时原版武器和 datapack 功能照常运行。

## 安装

1. 安装 Minecraft 1.21.1、Java 21 和 NeoForge 21.1.244 或同版本后续兼容构建。
2. 把 `superreforge-0.1.0.jar` 放入客户端与服务器的 `mods` 文件夹。
3. 可选安装 Curios 9.x（启用通用饰品池）和/或 KubeJS 2101（启用脚本 API）。
4. 启动世界；默认配方可合成熔核锻台、普通/精炼/至高重铸石。
5. 把恰好 1 个目标物品和一种重铸媒介放入锻台，在“锻造日志”确认成本及概率后开始重铸。

## 自定义入口

- [全局配置说明](docs/CONFIGURATION.md)
- [Datapack 从零到可用教程](docs/DATAPACK_TUTORIAL.md)
- [KubeJS 从零到可用教程](docs/KUBEJS_TUTORIAL.md)
- [Datapack API 与全部字段](docs/DATAPACK_API.md)
- [KubeJS API 与可复制脚本](docs/KUBEJS_API.md)
- [逐文件用途与修改影响](docs/FILE_REFERENCE.md)
- [严格 JSON 示例](examples/datapack)
- [带中文注释的 JSONC 阅读版](examples/datapack/readable)
- [KubeJS 示例](examples/kubejs)
- [JSON Schema](schemas)

datapack 放入世界的 `datapacks` 后执行 `/reload`。正式文件必须使用 `.json`；`.jsonc` 只用于阅读和照抄注释，不能直接装入游戏。

## 默认内容

- 22 个几何部件组成的熔核锻台，以及独立 3D 锻锤动画；模型仅组合原版材质，不包含参考模组贴图。
- 8 个默认品质等级。
- 3 套完整 1～8 级示例：近战、弓/弩、Curios。
- 3 种媒介：默认覆盖 1～4、3～6、5～8 级，并展示自动归一化权重。
- 木棍按 ID 硬绑定剑类型，便于验证任意自定义物品接入。

## 从源码构建

环境要求：Java 21。Windows 运行：

```powershell
.\gradlew.bat test build
```

Linux/macOS 运行：

```bash
./gradlew test build
```

产物位于 `build/libs/`。项目代码采用 `LGPL-3.0-or-later`；设计参考与资产边界见 [CREDITS.md](CREDITS.md)，版本变化见 [CHANGELOG.md](CHANGELOG.md)。
