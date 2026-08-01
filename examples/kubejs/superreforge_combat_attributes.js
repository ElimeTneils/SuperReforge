// 放置位置：kubejs/server_scripts/superreforge_combat_attributes.js
// 本示例只引用可选模组已有的 Attribute；Super Reforge 不会、也不应注册 Attribute。
// 未安装 Critical Strike、Ranged Weapon API 或 Curios 时，请删掉对应词条/物品类型后再 reload。

// 这一份 JSON 是脚本实际执行的配置表：复制一个等级 range 行和对应前缀即可扩充内容。
const SR_COMBAT_SPEC = JSON.parse(`{
  "levels": [
    { "id": "superreforge:worn", "rank": 1, "name": "绛夌骇 1", "color": "gray", "weight": 92 },
    { "id": "superreforge:common", "rank": 2, "name": "绛夌骇 2", "color": "white", "weight": 84 },
    { "id": "superreforge:fine", "rank": 3, "name": "绛夌骇 3", "color": "green", "weight": 76 },
    { "id": "superreforge:rare", "rank": 4, "name": "绛夌骇 4", "color": "aqua", "weight": 68 },
    { "id": "superreforge:epic", "rank": 5, "name": "绛夌骇 5", "color": "light_purple", "weight": 60 },
    { "id": "superreforge:legendary", "rank": 6, "name": "绛夌骇 6", "color": "gold", "weight": 52 },
    { "id": "superreforge:mythic", "rank": 7, "name": "绛夌骇 7", "color": "red", "weight": 44 },
    { "id": "superreforge:divine", "rank": 8, "name": "绛夌骇 8", "color": "yellow", "weight": 36 }
  ],
  "ranges": [
    { "criticalChance": [0.01, 0.02], "criticalDamage": [0.03, 0.05], "rangedDamage": [0.02, 0.03], "haste": [0.02, 0.03], "velocity": [0.01, 0.02], "pullTime": [-0.03, 0.02] },
    { "criticalChance": [0.02, 0.035], "criticalDamage": [0.05, 0.08], "rangedDamage": [0.03, 0.05], "haste": [0.03, 0.05], "velocity": [0.02, 0.035], "pullTime": [-0.05, 0.03] },
    { "criticalChance": [0.035, 0.05], "criticalDamage": [0.08, 0.12], "rangedDamage": [0.05, 0.08], "haste": [0.05, 0.08], "velocity": [0.035, 0.05], "pullTime": [-0.07, 0.05] },
    { "criticalChance": [0.05, 0.07], "criticalDamage": [0.12, 0.17], "rangedDamage": [0.08, 0.11], "haste": [0.08, 0.11], "velocity": [0.05, 0.075], "pullTime": [-0.10, 0.07] },
    { "criticalChance": [0.07, 0.095], "criticalDamage": [0.17, 0.23], "rangedDamage": [0.11, 0.15], "haste": [0.11, 0.15], "velocity": [0.075, 0.10], "pullTime": [-0.13, 0.10] },
    { "criticalChance": [0.095, 0.125], "criticalDamage": [0.23, 0.30], "rangedDamage": [0.15, 0.20], "haste": [0.15, 0.19], "velocity": [0.10, 0.13], "pullTime": [-0.16, 0.13] },
    { "criticalChance": [0.125, 0.16], "criticalDamage": [0.30, 0.38], "rangedDamage": [0.20, 0.25], "haste": [0.19, 0.23], "velocity": [0.13, 0.16], "pullTime": [-0.19, 0.16] },
    { "criticalChance": [0.16, 0.20], "criticalDamage": [0.38, 0.48], "rangedDamage": [0.25, 0.32], "haste": [0.23, 0.28], "velocity": [0.16, 0.20], "pullTime": [-0.22, 0.19] }
  ],
  "itemTypes": [
    { "id": "example:armor", "selectors": ["minecraft:head_armor", "minecraft:chest_armor", "minecraft:leg_armor", "minecraft:foot_armor"] },
    { "id": "example:tool", "selectors": ["minecraft:pickaxes", "minecraft:shovels", "minecraft:hoes"] }
  ],
  "pools": [
    {
      "id": "melee",
      "names": ["閿愭剰", "寮鸿", "鐚庢潃", "鑷村懡", "鐙傛垬", "鐮村啗", "寮戠", "缁堢剦"],
      "itemTypes": ["superreforge:sword", "superreforge:axe", "superreforge:trident", "superreforge:mace"],
      "slots": ["mainhand"],
      "effects": [
        { "id": "critical_chance", "attribute": "critical_strike:chance", "range": "criticalChance", "operation": "add_multiplied_base" },
        { "id": "critical_damage", "attribute": "critical_strike:damage", "range": "criticalDamage", "operation": "add_multiplied_base" }
      ]
    },
    {
      "id": "ranged",
      "names": ["绋冲鸡", "鍔插皠", "鐤剧窘", "楣扮溂", "椋庤", "绌夸簯", "閫愭槦", "澶╃┕"],
      "itemTypes": ["superreforge:bow", "superreforge:crossbow"],
      "slots": ["mainhand"],
      "effects": [
        { "id": "critical_chance", "attribute": "critical_strike:chance", "range": "criticalChance", "operation": "add_multiplied_base" },
        { "id": "critical_damage", "attribute": "critical_strike:damage", "range": "criticalDamage", "operation": "add_multiplied_base" },
        { "id": "ranged_damage", "attribute": "ranged_weapon:damage", "range": "rangedDamage", "operation": "add_multiplied_total" },
        { "id": "ranged_haste", "attribute": "ranged_weapon:haste", "range": "haste", "operation": "add_multiplied_base" },
        { "id": "ranged_velocity", "attribute": "ranged_weapon:velocity", "range": "velocity", "operation": "add_multiplied_total" },
        { "id": "ranged_pull_time", "attribute": "ranged_weapon:pull_time", "range": "pullTime", "operation": "add_multiplied_total" }
      ]
    },
    {
      "id": "armor",
      "names": ["鍧氶煣", "瀹堝娍", "閾佸", "涓嶅眻", "纾愮煶", "鍦ｄ綉", "涓嶇伃", "姘告亽"],
      "itemTypes": ["example:armor"],
      "slots": ["head", "chest", "legs", "feet"],
      "effects": [
        { "id": "critical_chance", "attribute": "critical_strike:chance", "range": "criticalChance", "operation": "add_multiplied_base" },
        { "id": "critical_damage", "attribute": "critical_strike:damage", "range": "criticalDamage", "operation": "add_multiplied_base" }
      ]
    },
    {
      "id": "tool",
      "names": ["鐔熺粌", "鍒╄惤", "绮惧伐", "杩呮嵎", "澶у笀", "濂囪抗", "绁炲尃", "鍒涗笘"],
      "itemTypes": ["example:tool"],
      "slots": ["mainhand"],
      "effects": [
        { "id": "critical_chance", "attribute": "critical_strike:chance", "range": "criticalChance", "operation": "add_multiplied_base" },
        { "id": "critical_damage", "attribute": "critical_strike:damage", "range": "criticalDamage", "operation": "add_multiplied_base" }
      ]
    },
    {
      "id": "curio",
      "names": ["寰厜", "鐏佃緣", "绁濈", "瀹堟姢", "鏄熻緣", "鍛借繍", "绁炶皶", "瓒呰秺"],
      "itemTypes": ["superreforge:curio"],
      "slots": ["curios:any"],
      "effects": [
        { "id": "critical_chance", "attribute": "critical_strike:chance", "range": "criticalChance", "operation": "add_multiplied_base" },
        { "id": "critical_damage", "attribute": "critical_strike:damage", "range": "criticalDamage", "operation": "add_multiplied_base" }
      ]
    }
  ]
}`)

// selector 可换成整合包自己的 tag；改 ID 时请同步修改 pools 的 itemTypes。
SR_COMBAT_SPEC.itemTypes.forEach(itemType => SuperReforge.addItemType(itemType.id, {
  include: itemType.selectors.map(tag => ({ tag }))
}))

SR_COMBAT_SPEC.levels.forEach(level => SuperReforge.addLevel(level.id, {
  rank: level.rank,
  name: { text: level.name, color: level.color, bold: level.rank >= 7 }
}))

const range = values => ({ min: values[0], max: values[1] })

SR_COMBAT_SPEC.pools.forEach(pool => SR_COMBAT_SPEC.levels.forEach((level, index) => {
  const values = SR_COMBAT_SPEC.ranges[index]
  SuperReforge.addModifier(`example:combat_${pool.id}_${level.rank}`, {
    level: level.id,
    item_types: pool.itemTypes,
    name: { text: pool.names[index], color: level.color, bold: level.rank >= 7 },
    // 权重为相对值，整合包可按等级调整；当前严格为 100 - rank * 8。
    weight: level.weight,
    attributes: pool.effects.map(effect => ({
      id: effect.id,
      attribute: effect.attribute,
      amount: range(values[effect.range]),
      operation: effect.operation,
      slots: pool.slots,
      // 所有词条都显示在 tooltip，便于玩家确认随机结果。
      show_in_tooltip: true
    }))
  })
}))

// Critical Strike 的 chance/damage 以 100 为基础百分比；0.01 即基础值的 +1%。
// pull_time 越低拉弓越快，故 ranges 中允许负数并使用 add_multiplied_total。
