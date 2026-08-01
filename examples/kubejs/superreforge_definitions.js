// 放置位置：kubejs/server_scripts/superreforge_definitions.js
// 本文件演示 KubeJS 如何覆盖或扩展 datapack 定义；所有 add* 调用都在脚本加载期执行。

// 注册一个供物品类型 selector 引用的脚本谓词。这里把木棍硬绑定为自定义武器。
SuperReforge.addPredicate('example:is_reforge_stick', stack => stack.id === 'minecraft:stick')

// 定义品质等级。rank 决定媒介的等级范围是否能抽到它，name 支持完整 Component 样式。
SuperReforge.addLevel('example:legendary', {
  rank: 4,
  name: { text: '传说', color: 'gold', bold: true }
})

// 同一物品可命中多个类型；最终词条池会按词条 ID 合并去重。
SuperReforge.addItemType('example:custom_weapon', {
  include: [
    { kubejs_predicate: 'example:is_reforge_stick' },
    { items: ['minecraft:diamond_sword', 'minecraft:netherite_sword'] },
    { tag: 'minecraft:swords' }
  ],
  exclude: [{ item: 'minecraft:wooden_sword' }]
})

// 一个前缀可同时提供多个 Attribute；amount 既可固定，也可用 min/max 按保存的随机种子计算。
SuperReforge.addModifier('example:legendary_blade', {
  level: 'example:legendary',
  item_types: ['example:custom_weapon'],
  name: { text: '传说', color: 'gold', bold: true },
  weight: 20,
  attributes: [
    {
      id: 'damage',
      attribute: 'minecraft:generic.attack_damage',
      amount: 0.04,
      operation: 'add_multiplied_base',
      slots: ['mainhand'],
      show_in_tooltip: true
    },
    {
      id: 'speed',
      attribute: 'minecraft:generic.attack_speed',
      amount: { min: 0.06, max: 0.10 },
      operation: 'add_multiplied_total',
      slots: ['mainhand'],
      show_in_tooltip: false
    }
  ]
})

// 权重不必手算成 1.0：20、30、30 会自动按 20/(20+30+30) 等比例归一化。
SuperReforge.addCatalyst('example:diamond_reforge', {
  ingredient: { item: 'minecraft:diamond' },
  count: 2,
  experience: 5,
  allow_same_modifier: false,
  allowed_item_types: ['example:custom_weapon'],
  denied_item_types: [],
  levels: [{ level: 'example:legendary', weight: 20 }]
})
