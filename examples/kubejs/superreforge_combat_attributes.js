// 放置位置：kubejs/server_scripts/superreforge_combat_attributes.js
// 本示例只引用可选模组已有的 Attribute；Super Reforge 不会、也不应注册 Attribute。
// 未安装 Critical Strike、Ranged Weapon API 或 Curios 时，请删掉对应词条/物品类型后再 reload。

const SR_LEVELS = [
  { id: 'superreforge:worn', rank: 1, name: '绛夌骇 1', color: 'gray' },
  { id: 'superreforge:common', rank: 2, name: '绛夌骇 2', color: 'white' },
  { id: 'superreforge:fine', rank: 3, name: '绛夌骇 3', color: 'green' },
  { id: 'superreforge:rare', rank: 4, name: '绛夌骇 4', color: 'aqua' },
  { id: 'superreforge:epic', rank: 5, name: '绛夌骇 5', color: 'light_purple' },
  { id: 'superreforge:legendary', rank: 6, name: '绛夌骇 6', color: 'gold' },
  { id: 'superreforge:mythic', rank: 7, name: '绛夌骇 7', color: 'red' },
  { id: 'superreforge:divine', rank: 8, name: '绛夌骇 8', color: 'yellow' }
]

const MELEE_NAMES = ['閿愭剰', '寮鸿', '鐚庢潃', '鑷村懡', '鐙傛垬', '鐮村啗', '寮戠', '缁堢剦']
const RANGED_NAMES = ['绋冲鸡', '鍔插皠', '鐤剧窘', '楣扮溂', '椋庤', '绌夸簯', '閫愭槦', '澶╃┕']
const ARMOR_NAMES = ['鍧氶煣', '瀹堝娍', '閾佸', '涓嶅眻', '纾愮煶', '鍦ｄ綉', '涓嶇伃', '姘告亽']
const TOOL_NAMES = ['鐔熺粌', '鍒╄惤', '绮惧伐', '杩呮嵎', '澶у笀', '濂囪抗', '绁炲尃', '鍒涗笘']
const CURIO_NAMES = ['寰厜', '鐏佃緣', '绁濈', '瀹堟姢', '鏄熻緣', '鍛借繍', '绁炶皶', '瓒呰秺']

const RANGES = [
  { criticalChance: [0.01, 0.02], criticalDamage: [0.03, 0.05], rangedDamage: [0.02, 0.03], haste: [0.02, 0.03], velocity: [0.01, 0.02], pullTime: [-0.03, 0.02] },
  { criticalChance: [0.02, 0.035], criticalDamage: [0.05, 0.08], rangedDamage: [0.03, 0.05], haste: [0.03, 0.05], velocity: [0.02, 0.035], pullTime: [-0.05, 0.03] },
  { criticalChance: [0.035, 0.05], criticalDamage: [0.08, 0.12], rangedDamage: [0.05, 0.08], haste: [0.05, 0.08], velocity: [0.035, 0.05], pullTime: [-0.07, 0.05] },
  { criticalChance: [0.05, 0.07], criticalDamage: [0.12, 0.17], rangedDamage: [0.08, 0.11], haste: [0.08, 0.11], velocity: [0.05, 0.075], pullTime: [-0.10, 0.07] },
  { criticalChance: [0.07, 0.095], criticalDamage: [0.17, 0.23], rangedDamage: [0.11, 0.15], haste: [0.11, 0.15], velocity: [0.075, 0.10], pullTime: [-0.13, 0.10] },
  { criticalChance: [0.095, 0.125], criticalDamage: [0.23, 0.30], rangedDamage: [0.15, 0.20], haste: [0.15, 0.19], velocity: [0.10, 0.13], pullTime: [-0.16, 0.13] },
  { criticalChance: [0.125, 0.16], criticalDamage: [0.30, 0.38], rangedDamage: [0.20, 0.25], haste: [0.19, 0.23], velocity: [0.13, 0.16], pullTime: [-0.19, 0.16] },
  { criticalChance: [0.16, 0.20], criticalDamage: [0.38, 0.48], rangedDamage: [0.25, 0.32], haste: [0.23, 0.28], velocity: [0.16, 0.20], pullTime: [-0.22, 0.19] }
]

// 如需扩充内容，复制一行范围和对应前缀，再使用新的 rank 与 ID；所有范围必须保持 min <= max。
const range = values => ({ min: values[0], max: values[1] })

SR_LEVELS.forEach(level => SuperReforge.addLevel(level.id, {
  rank: level.rank,
  name: { text: level.name, color: level.color, bold: level.rank >= 7 }
}))

// selector 可换成整合包自己的 tag；这里的两个 ID 可按需改名，并同步改下方 item_types。
SuperReforge.addItemType('example:armor', {
  include: [
    { tag: 'minecraft:head_armor' },
    { tag: 'minecraft:chest_armor' },
    { tag: 'minecraft:leg_armor' },
    { tag: 'minecraft:foot_armor' }
  ]
})

SuperReforge.addItemType('example:tool', {
  include: [
    { tag: 'minecraft:pickaxes' },
    { tag: 'minecraft:shovels' },
    { tag: 'minecraft:hoes' }
  ]
})

// Critical Strike 的 chance/damage 以 100 为基础百分比；0.01 即基础值的 +1%。
const criticalAttributes = (values, slots) => [
  { id: 'critical_chance', attribute: 'critical_strike:chance', amount: range(values.criticalChance), operation: 'add_multiplied_base', slots, show_in_tooltip: true },
  { id: 'critical_damage', attribute: 'critical_strike:damage', amount: range(values.criticalDamage), operation: 'add_multiplied_base', slots, show_in_tooltip: true }
]

const addModifier = (id, rank, name, itemTypes, attributes) => SuperReforge.addModifier(id, {
  level: SR_LEVELS[rank - 1].id,
  item_types: itemTypes,
  name: { text: name, color: SR_LEVELS[rank - 1].color, bold: rank >= 7 },
  // 权重是相对值；同池会自动归一化，等级越高出现概率越低。
  weight: 100 - rank * 8,
  attributes
})

SR_LEVELS.forEach((level, index) => {
  const rank = level.rank
  const values = RANGES[index]
  addModifier(`example:combat_melee_${rank}`, rank, MELEE_NAMES[index],
    ['superreforge:sword', 'superreforge:axe', 'superreforge:trident', 'superreforge:mace'],
    criticalAttributes(values, ['mainhand']))

  addModifier(`example:combat_ranged_${rank}`, rank, RANGED_NAMES[index],
    ['superreforge:bow', 'superreforge:crossbow'], [
      ...criticalAttributes(values, ['mainhand']),
      { id: 'ranged_damage', attribute: 'ranged_weapon:damage', amount: range(values.rangedDamage), operation: 'add_multiplied_total', slots: ['mainhand'], show_in_tooltip: true },
      { id: 'ranged_haste', attribute: 'ranged_weapon:haste', amount: range(values.haste), operation: 'add_multiplied_base', slots: ['mainhand'], show_in_tooltip: true },
      { id: 'ranged_velocity', attribute: 'ranged_weapon:velocity', amount: range(values.velocity), operation: 'add_multiplied_total', slots: ['mainhand'], show_in_tooltip: true },
      // pull_time 越低拉弓越快，所以使用负数的最终乘算值。
      { id: 'ranged_pull_time', attribute: 'ranged_weapon:pull_time', amount: range(values.pullTime), operation: 'add_multiplied_total', slots: ['mainhand'], show_in_tooltip: true }
    ])

  addModifier(`example:combat_armor_${rank}`, rank, ARMOR_NAMES[index], ['example:armor'],
    criticalAttributes(values, ['head', 'chest', 'legs', 'feet']))
  addModifier(`example:combat_tool_${rank}`, rank, TOOL_NAMES[index], ['example:tool'],
    criticalAttributes(values, ['mainhand']))
  addModifier(`example:combat_curio_${rank}`, rank, CURIO_NAMES[index], ['superreforge:curio'],
    criticalAttributes(values, ['curios:any']))
})
