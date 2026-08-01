# 数据包定义 API

Super Reforge 在服务器资源 reload 时读取四个目录，其中 `<namespace>` 是你的数据包命名空间：

```text
data/<namespace>/superreforge/levels/<path>.json
data/<namespace>/superreforge/item_types/<path>.json
data/<namespace>/superreforge/modifiers/<path>.json
data/<namespace>/superreforge/catalysts/<path>.json
```

文件 ID 由命名空间与相对路径生成：`data/example/superreforge/levels/rare.json` 的等级 ID 是 `example:rare`。四类定义必须能交叉引用；若解析或校验失败，该次数据包快照不会发布，旧快照继续使用。

## 共用格式

### `Component`

`levels[].name` 与 `modifiers[].name` 使用 Minecraft 标准文本组件 JSON，而不是纯字符串。可使用 `text`，也可使用 `translate`、`fallback`、`color`、`bold` 等组件字段；具体可用字段遵循当前 Minecraft 的 `ComponentSerialization`。

```json
{"text": "精良", "color": "green"}
```

```json
{"translate": "level.example.fine", "fallback": "精良", "color": "green"}
```

### `ItemSelector`

`ItemSelector` 出现在物品类型的 `include` / `exclude`，以及媒介的 `ingredient`。一个 selector 对象中可同时写多个字段；任意一个规则命中即视为该 selector 命中。

| 字段 | 类型 | 规则 |
| --- | --- | --- |
| `item` | 资源位置 | 精确匹配一个物品 ID。 |
| `items` | 资源位置数组，默认 `[]` | 任一物品 ID 匹配即可。 |
| `tag` | 资源位置 | 匹配物品标签。 |
| `curios` | 字符串 | 仅 `"any"`（不区分大小写）有含义：安装 Curios 后匹配任意被 Curios 识别的饰品；未安装 Curios 时不命中。 |
| `kubejs_predicate` | 字符串 | 匹配 KubeJS 通过 `SuperReforge.addPredicate` 注册的同名谓词；不存在时不命中。 |

selector 至少要有一个字段实际提供匹配规则；空 selector 会使定义校验失败。

## `levels`

```json
{
  "rank": 3,
  "name": {"text": "精良", "color": "green"}
}
```

| 字段 | 类型 | 必填 | 含义 |
| --- | --- | --- | --- |
| `rank` | 整数 | 是 | 等级排序值；没有额外范围限制。 |
| `name` | `Component` | 是 | 显示名称。 |

## `item_types`

```json
{
  "include": [{"tag": "minecraft:swords"}, {"item": "minecraft:stick"}],
  "exclude": [{"item": "minecraft:wooden_sword"}]
}
```

| 字段 | 类型 | 默认值 | 含义 |
| --- | --- | --- | --- |
| `include` | `ItemSelector[]` | `[]` | 至少一个 selector 命中才可进入此类型。 |
| `exclude` | `ItemSelector[]` | `[]` | 任意 selector 命中即否决，优先于 `include`。 |

一个物品可以同时命中多个物品类型。所有命中类型的词条池会合并；同一词条 ID 无论经由多少类型命中，都只会保留一个候选项。

## `modifiers`

```json
{
  "level": "example:fine",
  "item_types": ["example:sword"],
  "name": {"text": "锐利", "color": "green"},
  "weight": 30.0,
  "attributes": [
    {
      "id": "damage",
      "attribute": "minecraft:generic.attack_damage",
      "amount": {"min": 0.01, "max": 0.02},
      "operation": "add_multiplied_base",
      "slots": ["mainhand"],
      "show_in_tooltip": true
    }
  ]
}
```

| 字段 | 类型 | 默认值 | 含义 |
| --- | --- | --- | --- |
| `level` | 资源位置 | — | 已定义等级 ID。 |
| `item_types` | 资源位置数组 | — | 已定义物品类型 ID；目标命中其中任一个即可成为候选。 |
| `name` | `Component` | — | 词条显示名称。 |
| `weight` | 浮点数 | `1.0` | 同一被选中等级内的相对权重；必须是有限且非负的数。 |
| `attributes` | Attribute effect 数组 | `[]` | 应用到物品的 Attribute 效果。 |

### Attribute effect

| 字段 | 类型 | 默认值 | 含义 |
| --- | --- | --- | --- |
| `id` | 字符串 | — | effect 标识；同一词条内不能重复且不能为空。它也参与稳定 Attribute modifier ID 和范围值的确定。 |
| `attribute` | 资源位置 | — | 要修改的已注册 Attribute，例如 `minecraft:generic.attack_damage`。本模组不会注册外部 Attribute；若该 ID 在运行时不存在，只记录警告并让此 effect 失效，不使整条词条失效。 |
| `amount` | 数字或 `{ "min": 数字, "max": 数字 }` | — | 数字为固定值。范围值按物品保存的种子和 effect `id` 确定，之后稳定不变；`min` 与 `max` 必须有限且 `min <= max`。 |
| `operation` | 字符串 | — | `add_value`、`add_multiplied_base`、`add_multiplied_total` 三者之一，分别对应 Minecraft 的固定加值、按基础值乘加、按最终值乘加。 |
| `slots` | 字符串数组 | — | 至少一个：`mainhand`、`offhand`、`head`、`chest`、`legs`、`feet`、`curios:any`。每个 effect 可独立选槽位。`curios:any` 只由已安装的 Curios 兼容层应用到非外观饰品槽。 |
| `show_in_tooltip` | 布尔值 | `true` | 是否允许显示此 effect 的 Attribute 提示行；仍受服务端 `showAttributeLines` 总开关控制。 |

## `catalysts`

```json
{
  "ingredient": {"item": "example:reforge_stone"},
  "count": 2,
  "experience": 8,
  "allow_same_modifier": false,
  "allowed_item_types": ["example:sword"],
  "denied_item_types": [],
  "levels": [
    {"level": "example:fine", "weight": 70.0},
    {"level": "example:rare", "weight": 30.0}
  ]
}
```

| 字段 | 类型 | 默认值 | 含义 |
| --- | --- | --- | --- |
| `ingredient` | `ItemSelector` | — | 媒介栏中必须命中的 selector。 |
| `count` | 整数 | — | 基础媒介消耗，至少为 `1`。 |
| `experience` | 整数 | `0` | 基础经验成本，必须非负；是否收取由服务端配置决定。 |
| `allow_same_modifier` | 布尔值 | `false` | 是否允许在存在其他候选词条时再次抽到当前词条。 |
| `allowed_item_types` | 资源位置数组 | `[]` | 非空时，目标必须命中其中至少一种类型。 |
| `denied_item_types` | 资源位置数组 | `[]` | 命中任一种即禁止，优先于允许列表。 |
| `levels` | `{ level, weight }[]` | — | 可抽取等级及相对权重；`level` 必须存在，`weight` 必须为有限非负数。 |

## 候选与权重

媒介先按 `levels` 选择等级，再在该等级的可用词条中按 `modifier.weight` 选择词条。每一步都自动把正权重除以该步所有正权重之和：无需把权重写成 1、100 或 100%。权重为 `0` 的等级或词条不会进入抽取；若某一步没有正权重的有效候选，重铸不能开始。

词条匹配的是所有命中 `item_types` 的并集，并按词条资源 ID 去重。比如同一个 `example:swift` 同时声明在 `example:sword` 和 `example:tool`，目标同时命中二者时 `example:swift` 仍只参与一次抽取。
