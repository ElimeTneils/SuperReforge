# Super Reforge Datapack 从零到可用教程

这篇教程面向 Minecraft 1.21.1 / NeoForge 21.1.244，会做出一个能重铸剑类和通用 Curios 饰品的最小 datapack。“Datapack”是正确拼写；它就是世界里的“数据包”。完整字段表见 [Datapack API](DATAPACK_API.md)，可对照的现成文件见 [`examples/datapack`](../examples/datapack)。

## 1. 安装与目录树

在已创建的世界目录下新建 `datapacks/example_reforge/`，再建立以下结构。`example` 是本教程的命名空间；换名时要同步改所有 `example:*` 引用。

```text
example_reforge/
├─ pack.mcmeta
└─ data/
   └─ example/
      └─ superreforge/
         ├─ levels/
         │  └─ fine.json
         ├─ item_types/
         │  ├─ custom_weapons.json
         │  └─ universal_curios.json
         ├─ modifiers/
         │  ├─ tempered_blade.json
         │  └─ blessed_curio.json
         └─ catalysts/
            └─ diamond_reforge.json
```

路径会生成资源 ID。例如 `data/example/superreforge/levels/fine.json` 的 ID 是 `example:fine`。四个定义目录的名字必须精确为 `superreforge/levels`、`superreforge/item_types`、`superreforge/modifiers` 和 `superreforge/catalysts`。

## 2. 先写 `pack.mcmeta`

下方及后续标注为 `json` 的代码块都是无注释的严格 JSON，可按给定文件名直接保存。

```json
{
  "pack": {
    "pack_format": 48,
    "description": "Super Reforge 自定义重铸示例"
  }
}
```

## 3. 定义数字等级

将下列内容保存为 `levels/fine.json`。`rank` 是用于排序的整数，不必连续，也不限于 1～8；`name` 是 Minecraft 文本组件。

```json
{
  "rank": 3,
  "name": {
    "text": "精良",
    "color": "green"
  }
}
```

## 4. 定义物品类型

`item_types/custom_weapons.json` 把剑类标签、两个精确 ID 和木棍归入同一类，再用 `exclude` 优先排除木剑。`include` 中任意 selector 命中即包含；`exclude` 中任意 selector 命中即否决。

```json
{
  "include": [
    {"tag": "minecraft:swords"},
    {"items": ["minecraft:diamond_sword", "minecraft:netherite_sword"]},
    {"item": "minecraft:stick"}
  ],
  "exclude": [
    {"item": "minecraft:wooden_sword"}
  ]
}
```

`item_types/universal_curios.json` 是通用 Curios 类型：

```json
{
  "include": [
    {"curios": "any"}
  ],
  "exclude": []
}
```

只有安装 Curios 且物品被 Curios 识别时，`{"curios":"any"}` 才会命中。未安装 Curios 时它安全地返回不匹配，剑类等其他 datapack 功能仍可使用。

## 5. 写多 Attribute 词条

将下列内容保存为 `modifiers/tempered_blade.json`。同一词条可以有多个 effect：`damage` 用固定值，`speed` 在范围内按物品种子稳定取值，重进世界不会重抽。

```json
{
  "level": "example:fine",
  "item_types": ["example:custom_weapons"],
  "name": {
    "text": "锻锋",
    "color": "green"
  },
  "weight": 30,
  "attributes": [
    {
      "id": "damage",
      "attribute": "minecraft:generic.attack_damage",
      "amount": 2.0,
      "operation": "add_value",
      "slots": ["mainhand"],
      "show_in_tooltip": true
    },
    {
      "id": "speed",
      "attribute": "minecraft:generic.attack_speed",
      "amount": {"min": 0.03, "max": 0.05},
      "operation": "add_multiplied_base",
      "slots": ["mainhand", "offhand"],
      "show_in_tooltip": true
    }
  ]
}
```

`operation` 只支持三种值：`add_value` 是固定加值，`add_multiplied_base` 按基础值乘加，`add_multiplied_total` 按最终值乘加。`slots` 只支持 `mainhand`、`offhand`、`head`、`chest`、`legs`、`feet` 和 `curios:any`，每个 effect 可独立选择。

再将通用饰品词条保存为 `modifiers/blessed_curio.json`：

```json
{
  "level": "example:fine",
  "item_types": ["example:universal_curios"],
  "name": {
    "text": "祝福",
    "color": "green"
  },
  "weight": 20,
  "attributes": [
    {
      "id": "health",
      "attribute": "minecraft:generic.max_health",
      "amount": 2.0,
      "operation": "add_value",
      "slots": ["curios:any"],
      "show_in_tooltip": true
    }
  ]
}
```

## 6. 定义媒介和相对权重

将下列内容保存为 `catalysts/diamond_reforge.json`。这会使用 1 颗钻石与 5 级经验，同时允许前面两种物品类型。

```json
{
  "ingredient": {"item": "minecraft:diamond"},
  "count": 1,
  "experience": 5,
  "allow_same_modifier": false,
  "allowed_item_types": [
    "example:custom_weapons",
    "example:universal_curios"
  ],
  "denied_item_types": [],
  "levels": [
    {"level": "example:fine", "weight": 70}
  ]
}
```

`weight` 是相对权重，不是百分比。系统先对媒介的等级权重做 `weight / 所有有效正权重之和`，再对选中等级内的词条权重做同样归一化。因此单个等级写 `70` 仍是 100% ；两个已定义且都有有效词条的等级写 `20` 与 `30` 时，实际为 40% 与 60%。权重 0 不进入抽取；不要引用未定义的等级或物品类型。

## 7. 严格 JSON 与带注释 JSONC

- 游戏只加载各定义目录下的 `.json`。正式文件必须是严格 JSON：不允许 `//` 或 `/* */` 注释，不允许尾随逗号。
- 仓库的 [`examples/datapack/readable`](../examples/datapack/readable) 是带中文注释的 JSONC 阅读版，只用于理解字段，不能直接放入 datapack。
- 需要复制时，使用 [`examples/datapack/data`](../examples/datapack/data) 的严格 JSON 版，或删除 JSONC 里所有注释并改为 `.json`。

## 8. 加载与验收

1. 确认数据包位于当前世界的 `datapacks/example_reforge/`，且 `pack.mcmeta` 没有多套一层目录。
2. 进入世界后执行 `/reload`。
3. 用 `/datapack list enabled` 确认数据包已启用。
4. 把一把剑或 1 根木棍与钻石放入熔核锻台，确认预览中出现成本与候选。安装 Curios 后，可再用一件被 Curios 识别的饰品验证通用饰品池。

数据包会在四类定义全部解析并通过交叉校验后一次发布。任一文件失败时，旧的有效快照继续使用，所以不要把“游戏没崩溃”当成新文件已成功的证据。

## 常见问题与排错

| 现象 | 检查 |
| --- | --- |
| `/reload` 后新定义不生效 | 查看服务器日志的 JSON 解析或“拒绝定义 reload”错误；失败时会保留旧快照。 |
| 文件没有被发现 | 核对 `data/<namespace>/superreforge/<directory>/<path>.json`，尤其是 `item_types` 复数下划线和 `.json` 后缀。 |
| 显示 JSON 语法错 | 不要将 `.jsonc` 直接改名；删除注释、尾随逗号和非法引号。 |
| 提示未知等级或物品类型 | 资源 ID 的命名空间与文件路径必须和 `level`、`item_types`、`allowed_item_types` 的引用完全一致。 |
| 有等级但无候选 | 该等级必须存在至少一条命中目标物品类型且正权重的词条。 |
| Curios 饰品不匹配 | 确认 Curios 已安装、物品被 Curios 识别，selector 为 `{"curios":"any"}`，词条槽位为 `"curios:any"`。 |
| 某个 Attribute 没有效果 | 确认 `attribute` 资源 ID 由当前运行时实际注册；未知 Attribute 只会记录警告并跳过该 effect。 |
