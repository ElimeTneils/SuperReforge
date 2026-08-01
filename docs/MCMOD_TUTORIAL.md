# 超级重铸完整使用与自定义教程

本教程面向 Minecraft `1.21.1`、Java `21`，以及 NeoForge `21.1.244` 或后续仍兼容 Minecraft `1.21.1` 的构建。Super Reforge 是双端必需模组；Curios 与 KubeJS 是双端可选依赖。不安装它们时，原版武器和数据包重铸仍可使用。

## 适用版本与安装

将 Super Reforge 安装到客户端和服务器的模组目录。服务器规则由世界目录中的 `serverconfig/superreforge-server.toml` 决定，客户端本地配置不应覆盖服务器规则。若要使用 Curios 饰品或 KubeJS 脚本能力，再在两端安装相应的可选模组。

本教程基于当前内置内容：8 个品质等级（`worn` 至 `divine`）、7 个物品类型（含剑、斧、弓、弩、三叉戟、锤与 Curios）和 3 种重铸媒介。具体可重铸目标与候选词条始终以服务器当前的数据包和脚本定义为准。

## 基础重铸流程

1. 在熔核锻造台中放入 **数量恰为 1** 的目标物品；整叠物品不能作为一次重铸目标。
2. 放入能匹配某个媒介定义 `ingredient` 的物品。媒介还会限制允许/禁止的物品类型，并提供候选品质、材料数量与经验成本。
3. 查看服务器权威的概率预览，确认目标、媒介和候选词条均有效后开始重铸。
4. 正常情况下会扣除媒介定义的 `count` 个材料，以及其 `experience` 经阶段修正后的经验成本；经验是否启用及按等级还是经验点支付由服务器配置决定。
5. 默认动画为 `20 tick`（约 1 秒）；完成后，物品获得抽中的词条。铁砧重命名不会被词条名称写死或丢失。媒介定义默认不允许在仍有其他候选词条时重复抽到当前词条。

创造模式且 `creativePlayersPay = false` 时，材料和经验成本为 0；但仍须放入一个可匹配媒介定义的物品，且该媒介必须能产生有效候选池。自动首词条功能也只处理数量为 1 的物品，并不会经过重铸台、消耗材料或经验。

## 默认品质、物品类型与媒介

品质决定词条的第一层抽取范围；物品类型决定目标可进入哪些词条池；媒介决定材料、经验以及可抽取品质。目标可同时命中多个物品类型，所有命中类型的词条候选池会合并，并按词条资源 ID 去重。

内置媒介为 `common_reforge_stone`、`refined_reforge_stone` 与 `supreme_reforge_stone`。内置品质为 `worn`、`common`、`fine`、`rare`、`epic`、`legendary`、`mythic`、`divine`。这些只是随模组提供的定义；服务器可以通过数据包或 KubeJS 替换或扩展它们。

## 概率如何计算

抽取分两阶段进行：媒介先从自身 `levels` 中按正 `weight` 抽取品质；然后从该品质内、对目标有效的词条中按 `modifier.weight` 抽取词条。每个阶段都将正权重按总和归一化，权重为 `0` 的候选不会进入抽取；若任一阶段没有正权重的有效候选，重铸不能开始。

```text
某词条最终概率 = 该品质归一化概率 × 该品质内该词条归一化概率
```

例如，媒介的三个品质权重为 `20`、`30`、`30`，总和为 `80`，所以归一化结果依次是 `25%`、`37.5%`、`37.5%`。若第二个品质中两条有效词条的 `weight` 分别为 `1` 和 `3`，则其中权重为 `3` 的词条最终概率为 `37.5% × 3/(1+3) = 28.125%`。不需要把 `weight` 手算成 1、100 或百分数。

## 服务器配置

编辑世界目录的 `serverconfig/superreforge-server.toml` 的 `[general]` 节。默认配置如下：

```toml
[general]
experienceEnabled = true
experienceMode = "LEVELS"
automaticInitialModifier = false
automaticCatalyst = "superreforge:common_reforge_stone"
showAttributeLines = true
animationTicks = 20
creativePlayersPay = false
missingDefinitionPolicy = "REMOVE"
```

| 键 | 作用 |
| --- | --- |
| `experienceEnabled` | 是否收取媒介定义中的经验成本；设为 `false` 时经验成本恒为 0，材料成本仍照常计算。 |
| `experienceMode` | 经验支付方式：`LEVELS` 比较并扣除经验等级；`POINTS` 比较并扣除精确总经验点。成本为 8 时，前者要求至少 8 级，后者要求至少 8 点。`experienceEnabled = false` 时两种模式均不检查也不扣除经验。 |
| `automaticInitialModifier` | 是否让数量为 1、命中物品类型且尚无有效词条的物品，在生命周期校正时自动获得首个词条。 |
| `automaticCatalyst` | 自动首词条采用的媒介定义 ID；它选择候选品质与词条池但不消耗媒介或经验。无效资源 ID 会回退到默认值；找不到媒介定义时不会自动赋词条。 |
| `showAttributeLines` | 是否显示本模组生成的原版和 Curios Attribute 提示行。`false` 隐藏全部；`true` 仍尊重每个 effect 的 `show_in_tooltip: false`。 |
| `animationTicks` | 重铸动画时长，允许 `1` 至 `1200`；默认 `20 tick`。 |
| `creativePlayersPay` | 创造模式是否仍支付材料与经验。`false` 时免费，但不跳过媒介匹配和候选池校验。 |
| `missingDefinitionPolicy` | 保存的词条 ID 在活动定义中消失后的处理方式：`REMOVE` 移除物品上的词条数据；`KEEP_INACTIVE` 保留词条 ID 和随机种子，但暂不显示名称或提供 Attribute。恢复相同 ID 后，`KEEP_INACTIVE` 的词条会重新生效。 |

## 使用数据包添加自定义内容

将数据包放入世界的 `datapacks` 目录并执行 `/reload`。定义路径为 `data/<namespace>/superreforge/` 下的 `levels`、`item_types`、`modifiers`、`catalysts`；本例使用命名空间 `example`。四类定义必须能交叉引用并全部通过校验，新的数据包快照才会发布；失败时服务器继续使用旧快照。

正式数据包只能使用无注释的 `.json`。仓库中 `readable/` 内的 `.jsonc` 仅供阅读和抄写，加载时不会被识别。以下是与仓库 `examples/datapack` 一致的一组可安装 JSON：

`pack.mcmeta`

```json
{
  "pack": {
    "pack_format": 48,
    "description": "Super Reforge 1.21.1 完整数据示例"
  }
}
```

`data/example/superreforge/levels/legendary.json`（ID：`example:legendary`）

```json
{
  "rank": 4,
  "name": {
    "text": "传说",
    "color": "gold",
    "bold": true
  }
}
```

`data/example/superreforge/item_types/custom_weapons.json`（ID：`example:custom_weapons`）

```json
{
  "include": [
    {"item": "minecraft:stick"},
    {"items": ["minecraft:diamond_sword", "minecraft:netherite_sword"]},
    {"tag": "minecraft:swords"}
  ],
  "exclude": [
    {"item": "minecraft:wooden_sword"}
  ]
}
```

`data/example/superreforge/modifiers/legendary_blade.json`（ID：`example:legendary_blade`）

```json
{
  "level": "example:legendary",
  "item_types": ["example:custom_weapons"],
  "name": {
    "translate": "modifier.example.legendary_blade",
    "fallback": "传说",
    "color": "gold",
    "bold": true
  },
  "weight": 20,
  "attributes": [
    {
      "id": "damage",
      "attribute": "minecraft:generic.attack_damage",
      "amount": 0.04,
      "operation": "add_multiplied_base",
      "slots": ["mainhand"],
      "show_in_tooltip": true
    },
    {
      "id": "speed",
      "attribute": "minecraft:generic.attack_speed",
      "amount": {"min": 0.06, "max": 0.10},
      "operation": "add_multiplied_total",
      "slots": ["mainhand"],
      "show_in_tooltip": false
    }
  ]
}
```

`data/example/superreforge/catalysts/mixed_stone.json`（ID：`example:mixed_stone`）

```json
{
  "ingredient": {"item": "minecraft:diamond"},
  "count": 2,
  "experience": 5,
  "allow_same_modifier": false,
  "allowed_item_types": ["example:custom_weapons"],
  "levels": [
    {"level": "example:legendary", "weight": 20}
  ]
}
```

这里的媒介消耗 2 个钻石并有 5 的基础经验成本；`allowed_item_types` 使其仅接受命中 `example:custom_weapons` 的目标。`exclude` 命中优先于 `include`，因此木剑不会落入这个物品类型。

## 使用 KubeJS 覆盖同 ID 定义

安装 KubeJS 后，只在 `kubejs/server_scripts/super_reforge.js` 中调用顶层对象 `SuperReforge`；不要放在 `startup_scripts` 或 `client_scripts`。KubeJS 定义对象与数据包 JSON 采用相同字段。

沿用上一节的数据包，写入下面的同 ID 词条定义：

```js
SuperReforge.addModifier('example:legendary_blade', {
  level: 'example:legendary',
  item_types: ['example:custom_weapons'],
  name: { text: '熔铸传说', color: 'gold' },
  weight: 10,
  attributes: [{
    id: 'damage',
    attribute: 'minecraft:generic.attack_damage',
    amount: { min: 0.06, max: 0.10 },
    operation: 'add_multiplied_base',
    slots: ['mainhand'],
    show_in_tooltip: true
  }]
})
```

一次成功的 `server_scripts` reload 会把这条脚本定义作为一个原子发布，替换数据包中同类型、同 ID 的 `example:legendary_blade`。脚本加载报错或任一定义未通过交叉校验时，会继续保留上一份已发布的脚本层，不会产生半更新状态。单次 reload 内同类别重复 ID 会报错。

## 删除定义与处理已有物品

继续上述案例时，按以下顺序操作可以明确观察定义来源和已有物品策略：

```text
删除 KubeJS 同 ID 定义并成功 reload
→ 数据包中的 example:legendary_blade 重新生效

再删除数据包中的 example:legendary_blade.json 并执行 /reload
→ 活动定义中不再存在该 ID

missingDefinitionPolicy = REMOVE
→ 已有物品上的失效词条数据会被移除

missingDefinitionPolicy = KEEP_INACTIVE
→ 保留词条 ID 与随机种子，但暂不显示名称或提供 Attribute；恢复同 ID 定义后重新生效
```

先删除 `kubejs/server_scripts/super_reforge.js` 中的该 `addModifier` 调用并让 `server_scripts` reload 成功，脚本层移除后数据包层会重新提供相同 ID。然后删除数据包中的 `data/example/superreforge/modifiers/legendary_blade.json` 并执行 `/reload`，该 ID 才会从活动定义消失。

选择 `REMOVE` 时，已有物品保存的失效词条数据会被删除；选择 `KEEP_INACTIVE` 时，ID 和随机种子仍保留，但词条不显示名称且不提供 Attribute。以后恢复同一个 ID 的定义，保留的词条会再次解析并生效。若 ID 仍存在、只是物品不再匹配它的 `item_types`，词条同样不生效，但这不是缺失定义策略所删除的情形。

## 常见失败提示

| 现象 | 检查方向 |
| --- | --- |
| 无法开始重铸 | 目标数量必须为 1；媒介物品须命中 `ingredient`；目标须满足媒介的允许/禁止物品类型，并且两个抽取阶段都要有正 `weight` 的有效候选。 |
| 数据包 reload 后没有新内容 | 确认路径在 `data/<namespace>/superreforge/` 的四个定义目录中，文件为正式 `.json` 而非 `.jsonc`；检查跨文件 ID 引用、空 selector 和 JSON 语法。校验失败会保留旧快照。 |
| KubeJS 定义没有覆盖数据包 | 确认调用在 `server_scripts` 加载期而非 `startup_scripts` 或 `client_scripts`，ID 与定义类别均相同，并检查脚本是否报错。 |
| 属性没有显示或不生效 | `showAttributeLines` 设为 `false` 会隐藏所有提示行；单个 effect 的 `show_in_tooltip: false` 也会隐藏其行。还要确认 Attribute 已注册、`operation` 和 `slots` 合法。 |
| 已有物品在定义删除后表现异常 | 核对 `missingDefinitionPolicy` 是 `REMOVE` 还是 `KEEP_INACTIVE`，并确认是否恢复了完全相同的词条 ID。 |
| 无法开始重铸且提示定义同步 | 等待客户端完成当前服务端定义的同步；在数据包或脚本 reload 后重新打开锻台并检查同步是否完成。 |
| 锻台正在执行上一轮重铸 | 等待当前锻造动画完成后再开始下一次重铸。 |

## 投稿前作者人工核对（不要直接投稿）

- 实际启动版本：确认最终 JAR 的 Minecraft、NeoForge、Java 与模组版本，并与投稿页填写内容一致。
- 客户端/服务端安装方式：确认 Super Reforge 在目标客户端与专用服务器的安装步骤，并完成所需实机验收。
- Curios 与 KubeJS 可选关系：确认安装和未安装二者的组合环境，以及依赖展示、实际启用功能一致。
- 配方和游戏截图：确认熔核锻台、三种重铸石的配方与投稿所用截图均来自最终发布版本。
- 外部链接有效性：逐一核验下载、源码、许可证及相关页面链接。
- 作者亲自改写与事实确认：作者逐段改写本文，并确认所有投稿字段和功能描述与最终发布包一致。
