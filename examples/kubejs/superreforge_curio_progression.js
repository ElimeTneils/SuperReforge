// Super Reforge：MUTUO 的纯 Curios 八级词条配置。
// 文件位置：kubejs/server_scripts/superreforge_curio_progression.js。
// 本脚本会覆盖内置等级显示、停用 24 条内置词条，并让六级强化石只服务于 Curios 饰品。

// 把数据放入严格 JSON 字符串，便于测试、Excel 转换以及避免 KubeJS 2101 Rhino 的语法差异。
const SR_CURIO_SPEC = JSON.parse(`
{
  "levels": [
    {"id":"superreforge:worn","rank":1,"name":"等级 1","color":"gray"},
    {"id":"superreforge:common","rank":2,"name":"等级 2","color":"white"},
    {"id":"superreforge:fine","rank":3,"name":"等级 3","color":"green"},
    {"id":"superreforge:rare","rank":4,"name":"等级 4","color":"aqua"},
    {"id":"superreforge:epic","rank":5,"name":"等级 5","color":"light_purple"},
    {"id":"superreforge:legendary","rank":6,"name":"等级 6","color":"gold"},
    {"id":"superreforge:mythic","rank":7,"name":"等级 7","color":"red"},
    {"id":"superreforge:divine","rank":8,"name":"等级 8","color":"yellow"}
  ],
  "modifiers": [
    {"id":"mutuo:curio_1_1","rank":1,"name":"厚重","weight":55,"attributes":[
      {"id":"movement_speed","attribute":"minecraft:generic.movement_speed","amount":-0.05,"operation":"add_multiplied_base"},
      {"id":"attack_speed","attribute":"minecraft:generic.attack_speed","amount":-0.05,"operation":"add_multiplied_base"}
    ]},
    {"id":"mutuo:curio_1_2","rank":1,"name":"可怕","weight":25,"attributes":[
      {"id":"max_health","attribute":"minecraft:generic.max_health","amount":-2.0,"operation":"add_value"},
      {"id":"armor","attribute":"minecraft:generic.armor","amount":-1.0,"operation":"add_value"}
    ]},
    {"id":"mutuo:curio_1_3","rank":1,"name":"垃圾","weight":15,"attributes":[
      {"id":"luck","attribute":"minecraft:generic.luck","amount":-2.0,"operation":"add_value"},
      {"id":"movement_efficiency","attribute":"minecraft:generic.movement_efficiency","amount":-0.15,"operation":"add_value"}
    ]},
    {"id":"mutuo:curio_1_4","rank":1,"name":"贪婪","weight":5,"attributes":[
      {"id":"attack_damage","attribute":"minecraft:generic.attack_damage","amount":-0.03,"operation":"add_multiplied_base"},
      {"id":"max_health","attribute":"minecraft:generic.max_health","amount":-1.0,"operation":"add_value"}
    ]},

    {"id":"mutuo:curio_2_1","rank":2,"name":"普通","weight":55,"attributes":[
      {"id":"movement_speed","attribute":"minecraft:generic.movement_speed","amount":-0.02,"operation":"add_multiplied_base"}
    ]},
    {"id":"mutuo:curio_2_2","rank":2,"name":"简单","weight":25,"attributes":[
      {"id":"movement_efficiency","attribute":"minecraft:generic.movement_efficiency","amount":-0.05,"operation":"add_value"}
    ]},
    {"id":"mutuo:curio_2_3","rank":2,"name":"能用","weight":15,"attributes":[
      {"id":"armor","attribute":"minecraft:generic.armor","amount":-0.5,"operation":"add_value"}
    ]},
    {"id":"mutuo:curio_2_4","rank":2,"name":"还行","weight":5,"attributes":[
      {"id":"attack_damage","attribute":"minecraft:generic.attack_damage","amount":-0.01,"operation":"add_multiplied_base"}
    ]},

    {"id":"mutuo:curio_3_1","rank":3,"name":"稀有","weight":55,"attributes":[
      {"id":"movement_efficiency","attribute":"minecraft:generic.movement_efficiency","amount":0.05,"operation":"add_value"}
    ]},
    {"id":"mutuo:curio_3_2","rank":3,"name":"精良","weight":25,"attributes":[
      {"id":"armor","attribute":"minecraft:generic.armor","amount":0.5,"operation":"add_value"}
    ]},
    {"id":"mutuo:curio_3_3","rank":3,"name":"幸运","weight":15,"attributes":[
      {"id":"luck","attribute":"minecraft:generic.luck","amount":4.0,"operation":"add_value"}
    ]},
    {"id":"mutuo:curio_3_4","rank":3,"name":"优秀","weight":5,"attributes":[
      {"id":"attack_damage","attribute":"minecraft:generic.attack_damage","amount":0.005,"operation":"add_multiplied_base"}
    ]},

    {"id":"mutuo:curio_4_1","rank":4,"name":"史诗","weight":55,"attributes":[
      {"id":"movement_efficiency","attribute":"minecraft:generic.movement_efficiency","amount":0.10,"operation":"add_value"}
    ]},
    {"id":"mutuo:curio_4_2","rank":4,"name":"卓越","weight":25,"attributes":[
      {"id":"armor","attribute":"minecraft:generic.armor","amount":1.0,"operation":"add_value"}
    ]},
    {"id":"mutuo:curio_4_3","rank":4,"name":"精卓","weight":15,"attributes":[
      {"id":"attack_damage","attribute":"minecraft:generic.attack_damage","amount":0.01,"operation":"add_multiplied_base"}
    ]},
    {"id":"mutuo:curio_4_4","rank":4,"name":"非凡","weight":5,"attributes":[
      {"id":"critical_chance","attribute":"critical_strike:chance","amount":0.01,"operation":"add_value"}
    ]},

    {"id":"mutuo:curio_5_1","rank":5,"name":"传说","weight":55,"attributes":[
      {"id":"attack_damage","attribute":"minecraft:generic.attack_damage","amount":0.01,"operation":"add_multiplied_base"},
      {"id":"ranged_damage","attribute":"ranged_weapon:damage","amount":0.01,"operation":"add_multiplied_base"}
    ]},
    {"id":"mutuo:curio_5_2","rank":5,"name":"永恒","weight":25,"attributes":[
      {"id":"max_health","attribute":"minecraft:generic.max_health","amount":0.5,"operation":"add_value"},
      {"id":"armor","attribute":"minecraft:generic.armor","amount":0.5,"operation":"add_value"}
    ]},
    {"id":"mutuo:curio_5_3","rank":5,"name":"不朽","weight":15,"attributes":[
      {"id":"luck","attribute":"minecraft:generic.luck","amount":1.0,"operation":"add_value"},
      {"id":"movement_efficiency","attribute":"minecraft:generic.movement_efficiency","amount":0.125,"operation":"add_value"}
    ]},
    {"id":"mutuo:curio_5_4","rank":5,"name":"神话","weight":5,"attributes":[
      {"id":"critical_chance","attribute":"critical_strike:chance","amount":0.015,"operation":"add_value"},
      {"id":"critical_damage","attribute":"critical_strike:damage","amount":0.03,"operation":"add_value"}
    ]},

    {"id":"mutuo:curio_6_1","rank":6,"name":"运动员","weight":55,"attributes":[
      {"id":"max_health","attribute":"minecraft:generic.max_health","amount":1.0,"operation":"add_value"},
      {"id":"movement_efficiency","attribute":"minecraft:generic.movement_efficiency","amount":0.15,"operation":"add_value"}
    ]},
    {"id":"mutuo:curio_6_2","rank":6,"name":"防御师","weight":25,"attributes":[
      {"id":"armor","attribute":"minecraft:generic.armor","amount":1.0,"operation":"add_value"},
      {"id":"armor_toughness","attribute":"minecraft:generic.armor_toughness","amount":0.5,"operation":"add_value"}
    ]},
    {"id":"mutuo:curio_6_3","rank":6,"name":"缔造师","weight":15,"attributes":[
      {"id":"ranged_damage","attribute":"ranged_weapon:damage","amount":0.015,"operation":"add_multiplied_base"},
      {"id":"attack_damage","attribute":"minecraft:generic.attack_damage","amount":0.015,"operation":"add_multiplied_base"},
      {"id":"bullet_damage","attribute":"scguns:additional_bullet_damage","amount":0.015,"operation":"add_value"}
    ]},
    {"id":"mutuo:curio_6_4","rank":6,"name":"审判者","weight":5,"attributes":[
      {"id":"critical_chance","attribute":"critical_strike:chance","amount":0.025,"operation":"add_value"},
      {"id":"critical_damage","attribute":"critical_strike:damage","amount":0.04,"operation":"add_value"}
    ]},

    {"id":"mutuo:curio_7_1","rank":7,"name":"才华横溢","weight":55,"attributes":[
      {"id":"luck","attribute":"minecraft:generic.luck","amount":2.0,"operation":"add_value"},
      {"id":"movement_efficiency","attribute":"minecraft:generic.movement_efficiency","amount":0.25,"operation":"add_value"}
    ]},
    {"id":"mutuo:curio_7_2","rank":7,"name":"坚韧不拔","weight":25,"attributes":[
      {"id":"max_health","attribute":"minecraft:generic.max_health","amount":2.0,"operation":"add_value"},
      {"id":"armor_toughness","attribute":"minecraft:generic.armor_toughness","amount":0.5,"operation":"add_value"},
      {"id":"armor","attribute":"minecraft:generic.armor","amount":1.0,"operation":"add_value"}
    ]},
    {"id":"mutuo:curio_7_3","rank":7,"name":"勇往直前","weight":15,"attributes":[
      {"id":"attack_damage","attribute":"minecraft:generic.attack_damage","amount":0.025,"operation":"add_multiplied_base"},
      {"id":"ranged_damage","attribute":"ranged_weapon:damage","amount":0.025,"operation":"add_multiplied_base"},
      {"id":"bullet_damage","attribute":"scguns:additional_bullet_damage","amount":0.025,"operation":"add_value"}
    ]},
    {"id":"mutuo:curio_7_4","rank":7,"name":"致命一击","weight":5,"attributes":[
      {"id":"critical_chance","attribute":"critical_strike:chance","amount":0.025,"operation":"add_value"},
      {"id":"critical_damage","attribute":"critical_strike:damage","amount":0.05,"operation":"add_value"}
    ]},

    {"id":"mutuo:curio_8_1","rank":8,"name":"不朽的训练家","weight":55,"attributes":[
      {"id":"block_range","attribute":"minecraft:player.block_interaction_range","amount":2.0,"operation":"add_value"},
      {"id":"luck","attribute":"minecraft:generic.luck","amount":4.0,"operation":"add_value"},
      {"id":"mining_efficiency","attribute":"minecraft:player.mining_efficiency","amount":0.25,"operation":"add_value"},
      {"id":"step_height","attribute":"minecraft:generic.step_height","amount":1.0,"operation":"add_value"},
      {"id":"submerged_mining_speed","attribute":"minecraft:player.submerged_mining_speed","amount":0.25,"operation":"add_value"},
      {"id":"water_movement_efficiency","attribute":"minecraft:generic.water_movement_efficiency","amount":0.25,"operation":"add_value"},
      {"id":"movement_efficiency","attribute":"minecraft:generic.movement_efficiency","amount":0.25,"operation":"add_value"}
    ]},
    {"id":"mutuo:curio_8_2","rank":8,"name":"最后的守护者","weight":25,"attributes":[
      {"id":"armor","attribute":"minecraft:generic.armor","amount":5.0,"operation":"add_value"},
      {"id":"armor_toughness","attribute":"minecraft:generic.armor_toughness","amount":2.5,"operation":"add_value"},
      {"id":"safe_fall_distance","attribute":"minecraft:generic.safe_fall_distance","amount":2.0,"operation":"add_value"},
      {"id":"max_health","attribute":"minecraft:generic.max_health","amount":2.0,"operation":"add_value"}
    ]},
    {"id":"mutuo:curio_8_3","rank":8,"name":"绝世的战斗家","weight":15,"attributes":[
      {"id":"critical_chance","attribute":"critical_strike:chance","amount":0.025,"operation":"add_value"},
      {"id":"critical_damage","attribute":"critical_strike:damage","amount":0.05,"operation":"add_value"},
      {"id":"ranged_damage","attribute":"ranged_weapon:damage","amount":0.05,"operation":"add_multiplied_base"},
      {"id":"attack_damage","attribute":"minecraft:generic.attack_damage","amount":0.05,"operation":"add_multiplied_base"},
      {"id":"bullet_damage","attribute":"scguns:additional_bullet_damage","amount":0.05,"operation":"add_value"}
    ]},
    {"id":"mutuo:curio_8_4","rank":8,"name":"规则的缔造者","weight":5,"attributes":[
      {"id":"creative_flight","attribute":"neoforge:creative_flight","amount":1.0,"operation":"add_value"}
    ]}
  ],
  "catalysts": [
    {"id":"superreforge:common_reforge_stone","item":"superreforge:common_reforge_stone","levels":[[1,15],[2,25],[3,45],[4,10],[5,5]]},
    {"id":"superreforge:refined_reforge_stone","item":"superreforge:refined_reforge_stone","levels":[[3,25],[4,55],[5,15],[6,5]]},
    {"id":"superreforge:supreme_reforge_stone","item":"superreforge:supreme_reforge_stone","levels":[[5,55],[6,35],[7,10]]},
    {"id":"mutuo:enhancement_stone_4","item":"minecraft:netherite_ingot","levels":[[5,27],[6,55],[7,17],[8,1]]},
    {"id":"mutuo:enhancement_stone_5","item":"minecraft:nether_star","levels":[[7,95],[8,5]]},
    {"id":"mutuo:enhancement_stone_6","item":"minecraft:dragon_breath","levels":[[8,100]]}
  ]
}`)

// 只有 Curios 识别的任意非外观饰品槽会命中这个类型。
SuperReforge.addItemType('mutuo:curio_only', {
  include: [{ curios: 'any' }]
})

// 空气不可能作为重铸目标；内置词条改绑到这里后既不会进入候选，也不会继续提供旧效果。
SuperReforge.addItemType('mutuo:disabled_builtin', {
  include: [{ item: 'minecraft:air' }]
})

// 用稳定的原有等级 ID 覆盖显示，旧物品与媒介引用无需迁移。
SR_CURIO_SPEC.levels.forEach(level => SuperReforge.addLevel(level.id, {
  rank: level.rank,
  name: { text: level.name, color: level.color, bold: level.rank >= 7 }
}))

// KubeJS 不能删除较低优先级 datapack 定义，因此用 weight=0 与永不匹配类型停用 24 条内置词条。
const SR_BUILTIN_GROUPS = ['melee', 'ranged', 'curio']
SR_BUILTIN_GROUPS.forEach(group => SR_CURIO_SPEC.levels.forEach(level => {
  SuperReforge.addModifier(`superreforge:${group}_${level.rank}`, {
    level: level.id,
    item_types: ['mutuo:disabled_builtin'],
    name: { text: '已禁用', color: 'dark_gray' },
    weight: 0,
    attributes: []
  })
}))

// 每条新词条都只作用于当前实际佩戴它的 Curios 栏位；所有 amount 都是固定数值。
SR_CURIO_SPEC.modifiers.forEach(modifier => {
  const level = SR_CURIO_SPEC.levels[modifier.rank - 1]
  SuperReforge.addModifier(modifier.id, {
    level: level.id,
    item_types: ['mutuo:curio_only'],
    name: { text: modifier.name, color: level.color, bold: modifier.rank >= 7 },
    weight: modifier.weight,
    attributes: modifier.attributes.map(effect => ({
      id: effect.id,
      attribute: effect.attribute,
      amount: effect.amount,
      operation: effect.operation,
      slots: ['curios:any'],
      show_in_tooltip: true
    }))
  })
})

// 六种强化石都消耗 1 个、经验为 0；等级权重会由核心自动按 weight / 总和归一化。
SR_CURIO_SPEC.catalysts.forEach(catalyst => SuperReforge.addCatalyst(catalyst.id, {
  ingredient: { item: catalyst.item },
  count: 1,
  experience: 0,
  allow_same_modifier: false,
  allowed_item_types: ['mutuo:curio_only'],
  denied_item_types: [],
  levels: catalyst.levels.map(entry => ({
    level: SR_CURIO_SPEC.levels[entry[0] - 1].id,
    weight: entry[1]
  }))
}))
