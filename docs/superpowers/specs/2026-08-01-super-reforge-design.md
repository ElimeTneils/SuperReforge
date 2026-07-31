# Super Reforge 设计规格

- 日期：2026-08-01
- 作者：MUTUO
- 模组名称：Super Reforge
- 模组 ID：`superreforge`
- Java 包：`com.mutuo.superreforge`
- 目标平台：Minecraft 1.21.1、NeoForge 21.1.244、Java 21
- 代码许可证：LGPL-3.0-or-later
- 衍生美术许可证：CC BY-NC-SA 4.0

## 1. 目标

Super Reforge 提供一个独立的 3D 熔核锻台，让玩家为武器、任意指定物品和 Curios 饰品重铸一个前缀词条。一个词条可以包含多个 Attribute 效果。等级、概率、适用物品、Attribute、重铸媒介和成本均由 datapack 或可选 KubeJS 集成驱动。

核心要求：

- 每件物品最多拥有一个前缀词条。
- 词条名称动态加在当前物品名称前，铁砧重命名不会移除前缀。
- 物品只保存词条 ID、随机种子和数据版本；当前定义通过 ID 动态解析。
- `/reload` 修改定义后，已有物品使用同一种子重新映射新属性范围。
- Super Reforge 不注册任何新 Attribute，只兼容原版和其他模组已经注册的 Attribute。
- Curios 和 KubeJS 均为可选依赖；未安装时武器重铸和 datapack 功能继续工作。
- 所有 Curios 饰品共用一个默认词条池，并通过 `curios:any` 在任意有效 Curios 栏位生效。
- 物品可匹配多个自定义类型，词条池合并后按词条资源 ID 去重。

## 2. 非目标

- 不提供后缀、多词条槽或前后缀组合系统。
- 不注册远程伤害、拉弓速度、暴击率等 Attribute。
- 不把脚本作为核心运行依赖。
- 不复制 Remodifier、Modifiers 或 BountifulBaubles 的实现代码。
- 不让客户端决定词条、概率或成本。

## 3. 参考与许可证边界

设计参考：

- MCTeamPotato/Remodifier：单词条 ID、加权词条池、Curios 可选集成。
- CursedFlames/Modifiers：词条名称前缀、Attribute 组合、重铸配方思想。
- CursedFlames/BountifulBaubles：独立重铸台、铁砧/工作台/熔岩的 3D 视觉语言。

BountifulBaubles 代码为保留权利的自定义许可，因此 Super Reforge 的 Java 实现必须重新设计和编写。直接使用或改作的 BountifulBaubles 美术资源按 CC BY-NC-SA 4.0 发布，项目必须包含清晰署名、原资源链接、许可证文本、非商业限制和相同方式共享声明。

## 4. 总体架构

### 4.1 DefinitionManager

`DefinitionManager` 从下列资源目录加载 JSON：

```text
data/<namespace>/superreforge/levels/*.json
data/<namespace>/superreforge/item_types/*.json
data/<namespace>/superreforge/modifiers/*.json
data/<namespace>/superreforge/catalysts/*.json
```

加载顺序：

```text
服务端全局 config
→ datapack（按包优先级覆盖同 ID）
→ KubeJS 新增/覆盖/删除
→ 交叉校验
→ 冻结不可变快照
→ 同步客户端显示数据
```

KubeJS 只覆盖 datapack 内容；全局行为仍由服务端 config 控制。

### 4.2 ReforgeData

物品使用可持久化、网络同步的 Data Component：

```text
ReforgeData(
  modifierId: ResourceLocation,
  rollSeed: long,
  schemaVersion: int
)
```

每条 Attribute 效果拥有词条内稳定 ID。实际数值由 `rollSeed + modifierId + effectId` 派生。调整 JSON 字段顺序不会改变结果；修改 `min/max` 后，相同确定性样本会映射到新范围。

### 4.3 ItemTypeResolver

类型选择器支持：

- 精确物品 ID；
- 物品标签；
- Ingredient；
- KubeJS 动态 `ItemStack` 谓词；
- 可选 Curios `any` 选择器。

同一物品可以匹配多个类型。所有对应词条合并后按词条 ID 去重。同 ID 具有不同最终定义时视为加载冲突，不依赖加载顺序随机选择。

### 4.4 RollEngine

抽取顺序固定为：

```text
识别并合并物品类型
→ 过滤媒介类型限制
→ 移除对当前物品没有合法候选词条的等级
→ 按媒介等级权重抽等级
→ 按同级合法词条权重抽词条
→ 生成并保存随机种子
```

等级概率在过滤空等级之后重新归一化。这样即使媒介覆盖等级 1–4，而某个自定义物品只在等级 3–4 有词条，也不会抽到空结果。`allow_same_modifier = false` 导致某等级候选变空时，同样先移除该等级再计算最终概率。

权重自动归一化。对于 `20、30、30`：

```text
总权重 = 80
实际概率 = 25%、37.5%、37.5%
```

规则：

- 权重支持有限非负整数或小数；
- 省略词条权重时默认为 `1`，自然形成均分；
- `0` 表示保留定义但当前不可抽中；
- 负数、NaN、无穷值为错误；
- 某候选组所有权重均为零时，该组不可抽取并报告错误；
- GUI 显示服务端归一化后的真实概率。

### 4.5 CostService 与 ServerProgressData

基础成本来自媒介定义：一种媒介、可配置消耗数量、可配置经验。服务端 config 可完全关闭经验，并选择按等级或经验点扣除。

KubeJS 可以设置全服持久化阶段：

```text
材料成本 = ceil(基础数量 × material.multiply + material.add)
经验成本 = ceil(基础经验 × experience.multiply + experience.add)
```

材料至少为 1，经验至少为 0。多个阶段激活时只采用 `priority` 最高的阶段。同优先级的多个最高阶段视为冲突并报告错误。状态保存在 Overworld `SavedData` 中，随世界持久化。

### 4.6 ReforgerBlock

方块为四向旋转的非完整 3D 熔核锻台，采用“A·熔核锻台”方向：低矮厚重基座、中央熔核、铁砧工作面、两侧工具结构。美术基于 BountifulBaubles 允许改作的铁砧、工作台和熔岩资源，重新组合并扩展为更精细模型。

方块实体保存：

- 目标物品槽；
- 重铸媒介槽；
- 当前动画状态与开始游戏时间；
- 已扣费并确定结果的 `pendingResult`。

GUI 采用“B·锻造日志”：左侧操作区，右侧显示媒介可抽等级及实际概率，下方为玩家背包。

### 4.7 兼容层

- NeoForge 装备事件提供主手、副手和护甲栏 Attribute。
- Curios 安装时监听 `CurioAttributeModifierEvent`，通过栏位唯一 ID 避免多个饰品冲突。
- KubeJS 安装时注册专用事件组、定义构建器和全服阶段绑定。
- Super Reforge 自身不注册任何 Attribute。

## 5. 全局配置

文件：`<世界目录>/serverconfig/superreforge-server.toml`

```toml
# 是否启用重铸经验消耗。
[experience]
enabled = true

# "LEVELS" 按等级扣除；"POINTS" 按精确经验点扣除。
mode = "LEVELS"

# 是否在首次获得或装备无词条物品时自动抽取。
# 默认 false，表示只能使用熔核锻台获得第一个词条。
[automatic_modifiers]
enabled = false

# 自动抽取开启时使用的媒介概率定义。
default_catalyst = "superreforge:common_reforge_stone"

# 是否默认显示词条产生的原版或 Curios Attribute 行。
[display]
show_attribute_values = true

# 重铸动画长度；20 tick 约等于 1 秒。
[reforging]
animation_ticks = 20

# 创造模式玩家是否免除材料和经验。
creative_players_free = true

# 词条定义消失后的处理："REMOVE" 或 "KEEP_INACTIVE"。
invalid_modifier_policy = "REMOVE"
```

该文件只控制全服行为，不定义具体等级、词条、物品类型或媒介概率。

## 6. Datapack 数据模型

以下代码使用 JSONC 展示注释。正式 `.json` 必须移除注释。项目同时提供可加载 JSON、带中文注释的 `.jsonc.example`、中文指南和 JSON Schema。

### 6.1 等级文件

文件：`data/example/superreforge/levels/legendary.json`  
资源 ID：`example:legendary`

```jsonc
{
  // 数字顺序，用于排序、范围判断和 GUI 显示。
  "rank": 4,

  // 等级显示名称；也可以使用 translate 和 fallback。
  "name": {
    "text": "传说",
    "color": "gold",
    "bold": true
  }
}
```

等级数量、ID、名称、颜色和 `rank` 均可自定义。内置提供 `tier_1` 至 `tier_8` 八个中性等级；具体前缀名称与等级名称相互独立。

### 6.2 物品标签

文件：`data/example/tags/item/reforgeable/swords.json`  
标签 ID：`#example:reforgeable/swords`

```jsonc
{
  // false 表示与其他数据包对同一标签的内容合并。
  "replace": false,

  // 标签只负责分组，不直接赋予词条。
  "values": [
    "minecraft:wooden_sword",
    "minecraft:stone_sword",
    "minecraft:iron_sword",
    "minecraft:golden_sword",
    "minecraft:diamond_sword",
    "minecraft:netherite_sword",
    "example:katana"
  ]
}
```

### 6.3 物品类型

文件：`data/example/superreforge/item_types/sword.json`  
资源 ID：`example:sword`

```jsonc
{
  // 满足任意 include 规则即可属于该类型。
  "include": [
    {
      // 批量匹配上一节的剑标签。
      "tag": "example:reforgeable/swords"
    },
    {
      // 将木棍硬绑定为剑类，即使其 Java 类不是剑。
      "items": ["minecraft:stick"]
    }
  ],

  // exclude 优先于 include。
  "exclude": [
    {
      "item": "example:forbidden_sword"
    }
  ]
}
```

### 6.4 武器词条

文件：`data/example/superreforge/modifiers/legendary_blade.json`  
词条 ID：`example:legendary_blade`

```jsonc
{
  // 该词条属于自定义的第 4 级品质。
  "level": "example:legendary",

  // 只有匹配这些类型的物品能抽到该词条。
  "item_types": ["example:sword"],

  // 动态加入物品当前名称之前，例如“传说 Excalibur”。
  "name": {
    "text": "传说",
    "color": "gold",
    "bold": true
  },

  // 同等级合法词条间的相对权重；省略时默认为 1。
  "weight": 20,

  // 一个词条可包含任意多个 Attribute。
  "attributes": [
    {
      // 词条内部稳定效果 ID，用于确定性随机和 Modifier ID。
      "id": "attack_damage",

      // 原版或其他模组注册的 Attribute ID。
      "attribute": "minecraft:generic.attack_damage",

      // 固定增加基础攻击力的 4%。
      "amount": 0.04,
      "operation": "add_multiplied_base",

      // 仅在主手持有时生效。
      "slots": ["mainhand"],
      "show_in_tooltip": true
    },
    {
      "id": "attack_speed",
      "attribute": "minecraft:generic.attack_speed",

      // 使用种子在 6% 至 10% 之间确定性取值。
      "amount": {
        "min": 0.06,
        "max": 0.10
      },
      "operation": "add_multiplied_base",
      "slots": ["mainhand"],
      "show_in_tooltip": false
    }
  ]
}
```

支持的操作：

- `add_value`：固定加值；
- `add_multiplied_base`：按基础值百分比加算；
- `add_multiplied_total`：最终乘算。

支持的位置：

- `mainhand`、`offhand`；
- `head`、`chest`、`legs`、`feet`；
- 可选集成的 `curios:any`。

### 6.5 Curios 通用类型

内置文件：`data/superreforge/superreforge/item_types/curio.json`  
类型 ID：`superreforge:curio`

```jsonc
{
  "include": [
    {
      // 匹配能装备到任意有效 Curios 栏位的物品。
      "curios": "any"
    }
  ],
  "exclude": []
}
```

未安装 Curios 时该选择器匹配为空，不导致启动失败。

### 6.6 Curios 词条

文件：`data/example/superreforge/modifiers/robust_curio.json`

```jsonc
{
  "level": "example:legendary",

  // 只进入所有 Curios 饰品共用的默认池。
  "item_types": ["superreforge:curio"],

  // 显示为“强健 <原饰品名称>”。
  "name": {
    "text": "强健",
    "color": "gold"
  },

  // 省略 weight 时默认为 1，与其他省略权重的同级词条均分。
  "attributes": [
    {
      "id": "max_health",
      "attribute": "minecraft:generic.max_health",

      // 固定增加 4 点最大生命值。
      "amount": 4.0,
      "operation": "add_value",

      // 在任何 Curios 栏位正确装备时生效。
      "slots": ["curios:any"],
      "show_in_tooltip": true
    },
    {
      "id": "movement_speed",
      "attribute": "minecraft:generic.movement_speed",
      "amount": {
        "min": 0.03,
        "max": 0.06
      },
      "operation": "add_multiplied_base",
      "slots": ["curios:any"],
      "show_in_tooltip": false
    }
  ]
}
```

### 6.7 重铸媒介

文件：`data/example/superreforge/catalysts/common_reforge_stone.json`

```jsonc
{
  // 媒介槽接受的物品或 Ingredient。
  "ingredient": {
    "item": "superreforge:common_reforge_stone"
  },

  // 每次成功重铸消耗数量。
  "count": 2,

  // 基础经验成本；单位由全局 config 决定。
  "experience": 5,

  // false 表示有其他候选时排除当前词条。
  "allow_same_modifier": false,

  // 空数组表示允许全部物品类型。
  "allowed_item_types": [],
  "denied_item_types": [],

  // 等级使用相对权重，系统自动除以总权重。
  "levels": [
    {
      "level": "superreforge:tier_1",
      "weight": 35
    },
    {
      "level": "superreforge:tier_2",
      "weight": 25
    },
    {
      "level": "superreforge:tier_3",
      "weight": 20
    },
    {
      // 35+25+20+20=100，因此这里实际为 20%。
      "level": "superreforge:tier_4",
      "weight": 20
    }
  ]
}
```

默认媒介：

- 普通重铸石：等级 1–4，权重 `35/25/20/20`；
- 精炼重铸石：等级 3–6，权重 `30/30/25/15`；
- 至臻重铸石：等级 5–8，权重 `30/30/25/15`。

每个媒介都能设置类型白名单和黑名单；未填写限制时适用于全部可重铸类型。

## 7. KubeJS API

### 7.1 内容定义脚本

文件：`kubejs/server_scripts/superreforge_definitions.js`

```js
// 该事件在 datapack 内容读取后、最终定义冻结前执行。
// 相同资源 ID 的 KubeJS 定义覆盖 datapack。
SuperReforgeEvents.definitions(event => {
  // 注册任意自定义等级。
  event.level('kubejs:ancient', {
    rank: 7,
    name: Text.of('远古').darkPurple().bold()
  })

  // 扩展或创建物品类型。
  event.itemType('example:sword')
    // 精确物品 ID。
    .addItem('minecraft:stick')
    // KubeJS 或数据包标签。
    .addTag('kubejs:custom_swords')
    // Ingredient/正则匹配。
    .addIngredient(Ingredient.of(/example:.*_blade/))
    // 任意服务端动态谓词，可读取 Data Components。
    .test(stack => {
      return stack.getComponent('example:weapon_type') === 'sword'
    })

  // 注册词条；对象字段与 JSON 数据模型一致。
  event.modifier('kubejs:ancient_blade', {
    level: 'kubejs:ancient',
    item_types: ['example:sword'],
    name: Text.of('远古').darkPurple().bold(),
    weight: 30,
    attributes: [
      {
        id: 'damage',
        attribute: 'minecraft:generic.attack_damage',
        amount: { min: 0.08, max: 0.12 },
        operation: 'add_multiplied_base',
        slots: ['mainhand']
      }
    ]
  })

  // 显式删除较低优先级数据包中的词条。
  event.removeModifier('example:unwanted_modifier')
})
```

动态谓词只在服务端执行。异常时本次视为不匹配并限频记录日志，不影响服务器继续运行。

### 7.2 全服进度脚本

文件：`kubejs/server_scripts/superreforge_progression.js`

```js
// 示例：击败末影龙后提高整个服务器的重铸成本。
EntityEvents.death('minecraft:ender_dragon', event => {
  // 只在逻辑服务器执行，避免重复调用。
  if (event.level.isClientSide()) {
    return
  }

  // 创建或覆盖全服持久化阶段。
  SuperReforge.stages.set('post_dragon', {
    // 多个阶段中只采用最高优先级。
    priority: 100,

    // 最终材料 = ceil(基础数量 × 2 + 1)。
    material: {
      multiply: 2.0,
      add: 1
    },

    // 最终经验 = ceil(基础经验 × 1.5 + 5)。
    experience: {
      multiply: 1.5,
      add: 5
    }
  })
})

// 查询世界持久化状态。
ServerEvents.loaded(event => {
  if (SuperReforge.stages.has('post_dragon')) {
    console.info('Super Reforge：末影龙后期成本阶段已激活')
  }
})

// 删除阶段：SuperReforge.stages.remove('post_dragon')
```

## 8. 默认内容

内置物品类型：

```text
superreforge:sword
superreforge:axe
superreforge:bow
superreforge:crossbow
superreforge:trident
superreforge:mace
superreforge:curio
```

内置提供：

- 熔核锻台及其合成配方；
- 普通、精炼、至臻三种重铸媒介及配方；
- `tier_1` 至 `tier_8` 八个默认等级；
- 每个默认池在八个等级中至少有一个可加载示例词条；
- 中英文语言文件；
- 完整示例 datapack、带注释 JSONC 和 KubeJS 脚本。

由于 Super Reforge 不注册 Attribute，弓与弩的内置词条只能使用确实存在的原版 Attribute，并明确避免把近战攻击力描述成箭矢伤害或装填速度。安装提供远程 Attribute 的其他模组后，数据包可直接引用其注册表 ID。

## 9. 名称、属性与物品转换

- 前缀只加入物品显示名称，不额外创建词条说明行。
- Attribute 是否显示由全局默认值和单效果 `show_in_tooltip` 控制。
- 铁砧只修改基础名称；前缀始终动态拼接在当前名称前。
- 铁砧修理、附魔和锻造升级保留目标槽物品词条。
- 转换结果仍匹配包含该词条的合并池时保留，否则自动移除。
- KubeJS 动态谓词或物品 Data Component 变化导致类型不再匹配时，词条停止提供 Attribute，并在下一次服务端合法性检查时按全局策略移除或保留为不激活状态。
- 数量不等于 1 的目标物品无法放入或无法启动重铸。

名称动态组合需要一个最小、严格限定的 ItemStack 名称钩子；Attribute 使用 NeoForge/Curios 事件，不通过广泛 Mixin 修改实体属性生命周期。

## 10. 重铸事务与动画

客户端点击按钮时只发送“请求重铸”，不发送词条、概率或成本。服务端重新验证：

- 玩家菜单、方块、距离和动画状态；
- 目标数量恰好为 1；
- 媒介匹配且数量足够；
- 经验足够；
- 至少存在一个合法候选。

验证全部通过后，服务器在同一事务中扣除材料和经验、确定词条与种子，并把结果放入方块实体 `pendingResult`。槽位锁定约 20 tick。

动画时间线：

```text
0–6 tick：熔核逐渐增亮
7–12 tick：锻锤落下
13 tick：铁砧声音和火花粒子
14–20 tick：锻锤复位，熔核恢复
20 tick：pendingResult 写回目标槽并显示新前缀
```

异常规则：

- 关闭 GUI 或断线不会退款或取消；
- 服务器重启后从持久化 `pendingResult` 完成；
- 动画中破坏方块会立即完成并掉落已重铸物品；
- 任一验证失败时不扣除任何资源。

## 11. 数据同步

客户端只接收等级/词条显示信息、媒介显示信息、GUI 概率和快照版本。服务端选择器、KubeJS 谓词和成本逻辑不发送。

登录及 `/reload` 后同步经过校验的显示快照；大型数据分块传输并设置上限。同步失败时禁止该玩家发起重铸，但不影响服务器运行。所有词条抽取、成本和事务始终由服务器权威执行。

## 12. 校验与错误处理

校验内容：

- 资源 ID、引用 ID 和 JSON 字段；
- Attribute 是否存在；
- 效果 ID 是否在词条内唯一；
- 随机范围是否满足 `min <= max`；
- 权重是否有限且非负；
- 每个候选组是否存在正权重；
- 媒介数量是否大于零；
- 槽位与可选模组是否可用；
- KubeJS 覆盖后引用是否完整；
- 多个媒介定义是否冲突匹配同一物品。

一个词条含有任何无效 Attribute 时禁用整个词条，不进行部分加载。高优先级数据包给出无效覆盖时禁用该 ID，不静默回退。只有无法恢复的加载器异常才保留上一份完整快照。

错误日志必须包含文件、资源 ID、字段路径、原因和处理结果。

管理员命令：

```text
/superreforge validate
/superreforge inspect
/superreforge probabilities <媒介ID>
/superreforge stages list
```

## 13. 测试策略

单元测试：

- 权重归一化，包括 `20/30/30`、小数、零和非法值；
- 固定值和随机范围的确定性；
- 同种子在新区间重新映射；
- 多类型合并与词条 ID 去重；
- 媒介白名单/黑名单和重复开关；
- 成本阶段优先级、乘加顺序和向上取整；
- datapack/KubeJS 覆盖；
- Codec 编解码和错误字段路径。

NeoForge GameTest：

- 目标堆叠限制；
- 成本不足时的原子失败；
- 成功扣费、锁槽和 20 tick 结果；
- 关闭 GUI、断线、重启和破坏方块；
- 铁砧名称、修理和锻造升级；
- `/reload` 后已有物品更新；
- 主手、副手和护甲槽生效位置；
- 不安装 Curios/KubeJS 时正常启动。

可选集成测试：

- Curios 任意栏装备/卸下；
- 多个 Curios 相同词条叠加；
- KubeJS ID、标签、Ingredient、动态谓词和覆盖；
- 全服阶段重启持久化；
- 恶意或过期网络请求。

人工验收：

- 四向 3D 模型、碰撞、光照；
- 锻锤、熔核、火花和声音时间线；
- 锻造日志 GUI 在不同缩放下可读；
- 中英文；
- BountifulBaubles 署名和 CC BY-NC-SA 4.0 文件。

## 14. 验收标准

在 Minecraft 1.21.1、NeoForge 21.1.244、Java 21 环境中：

1. 玩家能用独立熔核锻台为合法武器、硬绑定物品和 Curios 饰品重铸一个前缀。
2. datapack 与 KubeJS 能定义等级、类型、词条、权重、固定/随机 Attribute、槽位和媒介。
3. 权重自动归一化，GUI 展示服务端真实概率。
4. 已有物品在 `/reload` 后按 ID 和种子使用新定义。
5. Curios/KubeJS 缺失时核心模组仍能加载和运行。
6. Super Reforge 不注册任何 Attribute，但能解析其他模组 Attribute。
7. 事务对关闭 GUI、断线、重启、破坏方块及伪造请求保持原子和防复制。
8. 所有公开 API、复杂 Java 逻辑、KubeJS 示例和 JSONC 阅读示例包含有意义的中文注释与中文文档。
