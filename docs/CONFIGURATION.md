# 服务端配置

配置文件位于世界目录的 `serverconfig/superreforge-server.toml`。它是服务端配置：客户端无需也不应以本地配置覆盖服务器规则。词条、等级、物品类型和媒介不在此文件定义；它们来自数据包或 KubeJS。

所有键都在 `[general]` 节：

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

| 键 | 默认值 | 作用 |
| --- | --- | --- |
| `experienceEnabled` | `true` | 是否收取媒介定义中的 `experience` 成本。`false` 时经验成本恒为 0，但媒介成本仍照常计算。 |
| `experienceMode` | `LEVELS` | 经验支付方式，见下节。 |
| `automaticInitialModifier` | `false` | 是否让符合条件、数量恰为 1 且尚无有效词条的物品自动获得首个词条。 |
| `automaticCatalyst` | `superreforge:common_reforge_stone` | 自动首词条使用的媒介定义 ID；它决定候选等级、词条池及权重，但不会消耗媒介或经验。无效资源 ID 会回退为该默认值；找不到对应媒介定义时不会自动赋词条。 |
| `showAttributeLines` | `true` | 是否显示本模组生成的原版及 Curios Attribute 提示行。`false` 隐藏全部；`true` 时仍会隐藏词条中 `show_in_tooltip: false` 的 effect。 |
| `animationTicks` | `20` | 重铸动画持续 tick 数，允许范围 `1` 至 `1200`（20 tick 约 1 秒）。 |
| `creativePlayersPay` | `false` | 创造模式玩家是否仍支付媒介与经验。`false` 时创造模式免费，但仍必须放入能匹配某个媒介定义的物品，以选择候选池。 |
| `missingDefinitionPolicy` | `REMOVE` | 已保存词条 ID 在当前定义中消失时的处理方式，见下节。 |

## `experienceMode`：`LEVELS` 与 `POINTS`

媒介的 `experience` 是一个整数；阶段修正后得到的整数成本按下列方式支付：

- `LEVELS`：比较并扣除玩家经验等级。例如成本为 `8` 时，需要至少 8 级，并调用等级扣除。
- `POINTS`：比较并扣除精确总经验点。例如成本为 `8` 时，需要至少 8 点总经验，并调用经验点扣除。

`experienceEnabled = false` 时不检查也不扣除经验，无论 `experienceMode` 为何。

## 自动首词条

开启 `automaticInitialModifier` 后，物品在生命周期校正时会尝试从 `automaticCatalyst` 指向的媒介候选池抽取词条。它只处理数量为 1 的物品，避免整叠物品共用一个词条数据；物品仍须命中至少一个物品类型并有可用候选词条。该过程不经过重铸台，因此不消耗媒介、经验或动画时间。

## 创造模式免费

当玩家处于创造模式且 `creativePlayersPay = false`，重铸结果的媒介与经验成本均为 0；普通玩家始终支付。免费不等于跳过校验：目标必须可重铸，媒介栏必须放入可匹配媒介定义的物品，且该媒介必须能产生候选词条。

## 失效词条策略

物品只保存词条 ID 和随机种子；定义 reload 后会按当前定义重新解析。

- `REMOVE`：如果保存的词条 ID 已不存在，移除物品上的词条数据。之后若开启自动首词条且候选池可用，物品可在同一次校正中获得新词条。
- `KEEP_INACTIVE`：保留保存的词条 ID 和种子，但不提供名称或 Attribute 效果。日后恢复同一 ID 的定义后，它会再次生效。

若词条 ID 仍存在但物品已不再命中该词条的 `item_types`，该词条同样不会生效；此情形不会被上述策略删除。
