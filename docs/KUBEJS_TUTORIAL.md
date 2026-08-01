# Super Reforge KubeJS 从零到可用教程

这篇教程面向已安装 KubeJS 2101 的 Minecraft 1.21.1 / NeoForge 整合包。Super Reforge 只在 `server_scripts` 中提供全局对象 `SuperReforge`；不要在 `startup_scripts` 或 `client_scripts` 中调用定义 API。完整签名见 [KubeJS API](KUBEJS_API.md)，四类定义对象的字段见 [Datapack API](DATAPACK_API.md)。

## 1. 文件位置和完整方法集

在游戏或服务器根目录确认有以下目录；先不要创建脚本文件，第 2 节会要求二选一：

```text
kubejs/
└─ server_scripts/
```

当前真实支持的公开方法是：

| 方法签名 | 用途 |
| --- | --- |
| `SuperReforge.addLevel(id, definition)` | 定义等级；对象与 `levels` datapack JSON 相同。 |
| `SuperReforge.addItemType(id, definition)` | 定义物品类型；对象与 `item_types` datapack JSON 相同。 |
| `SuperReforge.addModifier(id, definition)` | 定义词条；对象与 `modifiers` datapack JSON 相同。 |
| `SuperReforge.addCatalyst(id, definition)` | 定义媒介；对象与 `catalysts` datapack JSON 相同。 |
| `SuperReforge.addPredicate(id, predicate)` | 注册供 `kubejs_predicate` selector 引用的 `ItemStack` 布尔谓词。 |
| `SuperReforge.addStage(id, priority, materialMultiplier, materialAddition, experienceMultiplier, experienceAddition)` | 定义全服成本阶段。 |
| `SuperReforge.setStageActive(server, id, active)` | 持久化激活或停用阶段。 |
| `SuperReforge.getActiveStage(server)` | 返回生效阶段 ID；无激活阶段时返回空字符串。 |

所有 `SuperReforge.add*` 调用都必须直接在 `server_scripts` 加载期执行。不要把它们放进延迟任务、异步线程或普通运行期事件。

## 2. 先二选一：最小教学脚本或完整战斗脚本

下面的方案 A 与方案 B 必须二选一，不能同时放进 `server_scripts` 运行。KubeJS 定义可以用同 ID 覆盖 datapack 定义；但两个脚本都属于同一 KubeJS 层，同类别的重复 ID 会直接让 reload 失败，绝不会由后一个 KubeJS 脚本覆盖前一个。

| 选择 | 唯一启用的定义脚本 | 不要启用 |
| --- | --- | --- |
| 方案 A：最小教学脚本 | `superreforge_tutorial.js` | `superreforge_combat_attributes.js` |
| 方案 B：完整战斗脚本 | `superreforge_combat_attributes.js` | `superreforge_tutorial.js` |

先做选择再复制代码。选择方案 B 时直接跳到“方案 B”，不要复制方案 A 的任何注册代码块。

### 方案 A：最小教学脚本

创建 `kubejs/server_scripts/superreforge_tutorial.js`，依次复制本节以及第 3、4 节的代码。这个方案演示 8 级显示覆盖、selector、谓词、词条与媒介；不要再把完整战斗脚本放入 `server_scripts`。

下方代码覆盖内置 8 个等级的显示名和样式，同时保留 `superreforge:worn` 至 `superreforge:divine` 资源 ID，因此已引用这些 ID 的词条和媒介无需改名。

```js
// 覆盖内置等级显示；rank 仍用于排序。
const SR_LEVELS = [
  ['superreforge:worn', 1, '等级 1', 'gray'],
  ['superreforge:common', 2, '等级 2', 'white'],
  ['superreforge:fine', 3, '等级 3', 'green'],
  ['superreforge:rare', 4, '等级 4', 'aqua'],
  ['superreforge:epic', 5, '等级 5', 'light_purple'],
  ['superreforge:legendary', 6, '等级 6', 'gold'],
  ['superreforge:mythic', 7, '等级 7', 'red'],
  ['superreforge:divine', 8, '等级 8', 'yellow']
]

SR_LEVELS.forEach(([id, rank, text, color]) => {
  SuperReforge.addLevel(id, {
    rank,
    name: { text, color, bold: rank >= 7 }
  })
})
```

### 方案 B：完整战斗脚本

不要创建或启用 `superreforge_tutorial.js`，只复制 [`examples/kubejs/superreforge_combat_attributes.js`](../examples/kubejs/superreforge_combat_attributes.js) 到 `kubejs/server_scripts/`。完整脚本自身已经注册同一组 8 个 `superreforge:*` 等级；因此第 2～4 节的最小教学注册代码只阅读、不运行。若之前用过方案 A，先从 `server_scripts` 删除或移走 `superreforge_tutorial.js`，再启用方案 B。

第 6 节的进度阶段不注册等级，可以按需单独放入另一个 progression 脚本，与方案 B 共用。

## 3. 物品类型、选择器、KubeJS 组与谓词

Super Reforge 的 selector 对象只接受 `item`、`items`、`tag`、`curios` 和 `kubejs_predicate`。同一 selector 内多条规则是“或”；`include` 任意命中即包含，`exclude` 任意命中优先否决。

- KubeJS 组如果以物品 tag 存在，用 `{"tag":"example:reforge_group"}` 按其资源 ID 引用，不要加 `#`。原版 tag 也用同样方式，例如 `minecraft:swords`。
- 无法用 ID/tag 表达的逻辑先用 `SuperReforge.addPredicate` 注册，再用 `kubejs_predicate` 引用。谓词接收 `ItemStack` 并返回布尔值；抛错时会安全地当作不匹配并限频记录日志。

```js
// 这个谓词把可用久度超过 100 的物品作为自定义候选。
SuperReforge.addPredicate('example:durable', stack => stack.maxDamage > 100)

// tag 可以是原版 tag，也可以是整合包通过 KubeJS 建立的物品组/tag。
SuperReforge.addItemType('example:script_weapon', {
  include: [
    { tag: 'minecraft:swords' },
    { kubejs_predicate: 'example:durable' }
  ],
  exclude: [
    { item: 'minecraft:wooden_sword' }
  ]
})
```

数据包中的 `kubejs_predicate` 字段也可引用这个谓词，但只有安装 KubeJS 且脚本已成功发布时才能命中。

## 4. 词条、媒介与相对权重

继续在同一文件添加下列代码。定义对象与 datapack JSON 字段完全相同，但 JS 对象可以写中文注释。

```js
// 一条词条可有多个 Attribute effect；amount 可为固定值或 { min, max }。
SuperReforge.addModifier('example:swift_weapon', {
  level: 'superreforge:rare',
  item_types: ['example:script_weapon'],
  name: { text: '迅捷', color: 'aqua' },
  weight: 30,
  attributes: [
    {
      id: 'damage',
      attribute: 'minecraft:generic.attack_damage',
      amount: 1.0,
      operation: 'add_value',
      slots: ['mainhand'],
      show_in_tooltip: true
    },
    {
      id: 'speed',
      attribute: 'minecraft:generic.attack_speed',
      amount: { min: 0.03, max: 0.05 },
      operation: 'add_multiplied_base',
      slots: ['mainhand'],
      show_in_tooltip: true
    }
  ]
})

// 权重是相对值，系统会自动归一化；单个等级写 70 仍是 100%。
SuperReforge.addCatalyst('example:diamond_reforge', {
  ingredient: { item: 'minecraft:diamond' },
  count: 1,
  experience: 5,
  allow_same_modifier: false,
  allowed_item_types: ['example:script_weapon'],
  denied_item_types: [],
  levels: [
    { level: 'superreforge:rare', weight: 70 }
  ]
})
```

抽取先归一化媒介的等级权重，再归一化选中等级内的词条权重。例如同一步的有效正权重为 20、30、50，实际概率就是 20%、30%、50%；无需先手算成 0.2、0.3、0.5。

## 5. Critical Strike 与 Ranged Weapon Attribute

Super Reforge 不注册任何外部 Attribute，只按资源 ID 使用当前运行时已注册的 Attribute。方案 B 使用的 8 级、5 物品池完整脚本是 [`examples/kubejs/superreforge_combat_attributes.js`](../examples/kubejs/superreforge_combat_attributes.js)。它不是方案 A 的追加模块：如要改用这份脚本，必须先停用最小教学脚本，避免在同一 KubeJS 层重复注册 8 个等级 ID。

完整示例使用下列六个第三方 Attribute ID 和实际运算：

| Attribute ID | 运算 | 用途 |
| --- | --- | --- |
| `critical_strike:chance` | `add_multiplied_base` | Critical Strike 暴击几率 |
| `critical_strike:damage` | `add_multiplied_base` | Critical Strike 暴击伤害 |
| `ranged_weapon:damage` | `add_multiplied_total` | Ranged Weapon 远程伤害 |
| `ranged_weapon:haste` | `add_multiplied_base` | Ranged Weapon 速度 |
| `ranged_weapon:velocity` | `add_multiplied_total` | Ranged Weapon 抛射物速度 |
| `ranged_weapon:pull_time` | `add_multiplied_total` | Ranged Weapon 拉弓时间；负值范围代表缩短时间 |

这些都是可选模组的 ID。未安装 Critical Strike 或 Ranged Weapon API 时，删除完整示例中对应的 effect/物品池，或接受服务器对未注册 Attribute 的警告与该 effect 失效。Curios 同样是可选依赖；未安装时 `curios:any` 选择器不命中，对应槽位不应用。

## 6. 持久化全服阶段与最高优先级成本

`SuperReforge.addStage` 定义全服阶段。激活 ID 会保存到 Overworld 的服务器世界数据中，所以是全服共享且会在重启后持久化，不是玩家个人状态。

```js
// 在 server_scripts 加载期定义阶段：材料 floor(基础值 × 1.5) + 1，经验 floor(基础值 × 2.0) + 0。
SuperReforge.addStage(
  'example:dragon_defeated',
  100,
  1.5,
  1,
  2.0,
  0
)

ServerEvents.loaded(event => {
  // 这里仅演示读写 API；正式整合包应在真实进度事件中激活。
  // 必须传 event.server，不能传玩家、level 或字符串。
  SuperReforge.setStageActive(event.server, 'example:dragon_defeated', true)

  // 无激活阶段时返回空字符串，否则返回当前生效 ID。
  console.info(`Super Reforge 当前阶段：${SuperReforge.getActiveStage(event.server)}`)
})
```

材料和经验分别按 `floor(基础成本 × multiplier) + addition` 计算，结果下限为 0。可同时激活多个阶段，但成本只采用 `priority` 最高的一个；优先级相同时按 ID 字符串排序选择。调用 `SuperReforge.setStageActive(event.server, id, false)` 可停用指定阶段。

## 7. Reload 与验收

1. 再确认只启用一种定义脚本：方案 A 保存 `kubejs/server_scripts/superreforge_tutorial.js`；方案 B 只保存 `kubejs/server_scripts/superreforge_combat_attributes.js`。不要同时保留两者。
2. 执行 KubeJS 的 `/kubejs reload server_scripts`；如当前整合包未暴露该命令，重启服务器以完整重载 `server_scripts`。
3. 查看服务器日志，确认脚本无错且出现 Super Reforge 已发布 KubeJS 层的记录。
4. 方案 A 用钻石与命中 `example:script_weapon` 的物品验证候选、权重和成本；方案 B 用内置重铸石与脚本支持的武器验证 40 条战斗词条。两种方案都应检查物品名称中的等级前缀显示。

每次 reload 都使用全新临时收集器。只有所有服务器脚本没有报错、四类定义通过交叉校验时，定义、谓词和阶段才会整体发布。任一步失败都保留上一份有效脚本层，不会只更新一半。

## 常见问题与排错

| 现象 | 检查 |
| --- | --- |
| `SuperReforge is not defined` | 确认 KubeJS 2101 已安装，文件在 `kubejs/server_scripts/`，而不是 `startup_scripts` 或 `client_scripts`。 |
| 提示 `add*` 只能在加载期调用 | 把 `SuperReforge.addLevel/addItemType/addModifier/addCatalyst/addPredicate/addStage` 移回脚本顶层，不要放进延迟或异步回调。 |
| reload 后仍是旧定义 | 查看当次 KubeJS 错误和交叉校验日志；发布失败会主动保留上一份有效层。 |
| 报同类别重复 ID | 同一次 `server_scripts` reload 中每类定义的 ID 只能注册一次；脚本覆盖 datapack 不等于脚本内可重复。 |
| `kubejs_predicate` 不命中 | 确认 `SuperReforge.addPredicate` 的字符串 ID 完全一致，谓词返回布尔值，且没有抛出异常。 |
| 阶段无法切换 | `SuperReforge.setStageActive` 必须接收服务器事件给出的 `event.server`；阶段 ID 必须已用 `addStage` 定义才能影响成本。 |
| 外部 Attribute 无效 | 确认对应可选模组已安装并真正注册了该资源 ID；Super Reforge 不会代替 Critical Strike 或 Ranged Weapon API 注册 Attribute。 |
| Curios 池不工作 | 确认 Curios 已安装且物品被它识别，类型 selector 使用 `{"curios":"any"}`，effect 槽位使用 `"curios:any"`。 |
