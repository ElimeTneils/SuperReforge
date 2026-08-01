# KubeJS API

首次使用时，先按 [KubeJS 从零到可用教程](KUBEJS_TUTORIAL.md) 完成文件位置、六种 `add*` 方法、覆盖和阶段流程；需要核对定义目录或严格 JSON 时，参阅 [Datapack 从零到可用教程](DATAPACK_TUTORIAL.md)。

安装 KubeJS 后，Super Reforge 只在 `server_scripts` 中提供顶层全局对象 `SuperReforge`。不要使用 `startup_scripts` 或 `client_scripts`；定义直接在服务器脚本加载期调用下列方法。

四种定义方法接收的对象与数据包 JSON 完全相同，字段见 [DATAPACK_API.md](DATAPACK_API.md)。KubeJS 层与数据包层分开收集；同一种定义使用相同资源 ID 时，脚本层覆盖数据包层。单次 `server_scripts` reload 内同类别的重复 ID 会报错。

## 方法签名

```js
SuperReforge.addLevel(id, definition)
SuperReforge.addItemType(id, definition)
SuperReforge.addModifier(id, definition)
SuperReforge.addCatalyst(id, definition)
SuperReforge.addPredicate(id, predicate)
SuperReforge.addStage(id, priority, materialMultiplier, materialAddition, experienceMultiplier, experienceAddition)
SuperReforge.setStageActive(server, id, active)
SuperReforge.getActiveStage(server)
```

- `id` 在四个定义方法中是有效资源位置字符串，如 `"example:rare"`；在 `addPredicate` 和阶段 API 中是非空普通字符串。
- `definition` 必须是可被对应 Codec 接受的 JS 对象，结构、必填字段和默认值与数据包一致。
- `predicate` 是接收 `ItemStack` 并返回布尔值的函数，对应 selector 的 `kubejs_predicate`。
- `server` 必须是实际 `MinecraftServer`，在事件回调中传入该回调给出的 `event.server`。

## 可复制示例：定义

将以下内容放入 `kubejs/server_scripts/super_reforge.js`。所有 `add*` 定义调用都在脚本加载期执行。

```js
// 供 item type selector 的 kubejs_predicate 使用；参数是 ItemStack。
SuperReforge.addPredicate('example:durable', stack => stack.maxDamage > 100)

// 等级对象与 data/<namespace>/superreforge/levels/*.json 内容相同。
SuperReforge.addLevel('example:rare', {
  rank: 4,
  name: { text: '稀有', color: 'aqua' }
})

// include 命中其一即可；exclude 命中会否决。selector 可使用 kubejs_predicate。
SuperReforge.addItemType('example:durable_tool', {
  include: [
    { tag: 'minecraft:tools' },
    { kubejs_predicate: 'example:durable' }
  ],
  exclude: [{ item: 'minecraft:wooden_pickaxe' }]
})

// 词条对象与 modifiers JSON 相同；amount 可为固定数字或 { min, max }。
SuperReforge.addModifier('example:swift_tool', {
  level: 'example:rare',
  item_types: ['example:durable_tool'],
  name: { text: '迅捷', color: 'aqua' },
  weight: 30,
  attributes: [{
    id: 'speed',
    attribute: 'minecraft:generic.attack_speed',
    amount: { min: 0.02, max: 0.04 },
    operation: 'add_multiplied_base',
    slots: ['mainhand'],
    show_in_tooltip: true
  }]
})

// 媒介对象与 catalysts JSON 相同；权重是相对权重，会自动归一化。
SuperReforge.addCatalyst('example:tool_stone', {
  ingredient: { item: 'minecraft:amethyst_shard' },
  count: 1,
  experience: 5,
  allow_same_modifier: false,
  allowed_item_types: ['example:durable_tool'],
  denied_item_types: [],
  levels: [{ level: 'example:rare', weight: 1 }]
})
```

这些定义方法只能在 `server_scripts` 加载期调用；不要缓存 API 调用上下文，也不要从异步线程调用它们。

## 阶段与服务器状态

`addStage` 定义全服阶段：

```js
SuperReforge.addStage(
  'example:hardmode', // id
  100,                // priority：多个已激活阶段中数值更高者生效；并列按 id 排序
  1.5,                // materialMultiplier
  1,                  // materialAddition
  2.0,                // experienceMultiplier
  3                   // experienceAddition
)
```

阶段对媒介与经验分别计算 `floor(基础成本 × multiplier) + addition`，结果会钳制到不小于 0。`multiplier` 必须是有限非负数。只有当前激活阶段中优先级最高的一项会影响成本。

用拥有 `event.server` 的服务器事件切换或查询状态；状态会保存到该服务器世界数据中：

```js
ServerEvents.loaded(event => {
  // 必须传事件给出的服务器，而非玩家、level 或字符串。
  SuperReforge.setStageActive(event.server, 'example:hardmode', true)

  // 没有激活阶段时返回空字符串；否则返回最高优先级阶段的 id。
  if (SuperReforge.getActiveStage(event.server) === 'example:hardmode') {
    console.info('硬核重铸成本已启用')
  }
})
```

## 发布语义

每次 `server_scripts` reload 开始时，API 都会使用新的临时收集器。只有所有服务器脚本没有报错、所有定义通过交叉校验后，四类定义会一起替换为新脚本层；随后才替换谓词与阶段。任一步失败都会保留上一份已发布的脚本定义层、谓词和阶段，而不会产生半更新状态。删除脚本中的定义后，下一次成功 reload 会把它从脚本层移除；若没有同 ID 的数据包定义，该定义将不再存在。

首次创建/进入世界时，KubeJS 可能早于 datapack 基础定义执行。若脚本引用了尚未发布的等级或物品类型，API 会暂存本轮完整 bundle，并在 datapack 成功加载后自动重试；定义、谓词和阶段仍然只会一次性整体生效，不要求玩家手动执行 `/reload`。datapack 已就绪后的校验失败不会延迟，而会作为真实配置错误拒绝。
