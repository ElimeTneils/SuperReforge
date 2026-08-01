// Super Reforge：零基值百分比 Attribute 的客户端显示修正。
// 文件位置：kubejs/client_scripts/superreforge_percent_tooltips.js。
//
// movement_efficiency 等属性的真实有效范围是 0–1，但原版 ADD_VALUE Tooltip 会直接显示 0.15。
// 服务端仍必须保存 0.15 才能得到真正的 15%；本脚本只把提示文本改为 15%，不会重复增加属性。

const SR_DefinitionManager = Java.loadClass('com.mutuo.superreforge.definition.DefinitionManager')
const SR_ModDataComponents = Java.loadClass('com.mutuo.superreforge.registry.ModDataComponents')
const SR_DeterministicValue = Java.loadClass('com.mutuo.superreforge.reforge.DeterministicValue')
const SR_Component = Java.loadClass('net.minecraft.network.chat.Component')
const SR_Style = Java.loadClass('net.minecraft.network.chat.Style')
const SR_TextColor = Java.loadClass('net.minecraft.network.chat.TextColor')

// 给所有物品安装一个轻量动态处理器；没有 Super Reforge 词条的物品会立即返回。
ItemEvents.modifyTooltips(event => {
  event.modifyAll(builder => builder.dynamic('superreforge:percent_values'))
})

ItemEvents.dynamicTooltips('superreforge:percent_values', event => {
  // 直接读取物品保存的 modifier ID，并查询服务端同步到客户端的显示镜像。
  // 这样不会触发服务端物品类型选择器，也不会受整合服/远程客户端快照分支差异影响。
  const reforgeData = event.item.get(SR_ModDataComponents.REFORGE_DATA.get())
  if (reforgeData === null || !reforgeData.active()) {
    return
  }

  const definition = SR_DefinitionManager.clientModifiers().get(reforgeData.modifierId())
  if (definition === null) {
    return
  }

  // 由 Java List.forEach 把每条记录分别传入回调，避开 Rhino 对 get(index)/数组元素的包装对象复用。
  definition.attributes().forEach(effect => {
    const attributeId = String(effect.attribute())
    let label
    if (attributeId === 'minecraft:generic.movement_efficiency') {
      label = '移动效率'
    } else if (attributeId === 'minecraft:generic.water_movement_efficiency') {
      label = '水中移动效率'
    } else if (attributeId === 'minecraft:player.mining_efficiency') {
      label = '挖掘效率'
    }
    if (label === undefined) {
      return
    }

    const amount = Number(SR_DeterministicValue.resolve(
      effect.amount(),
      reforgeData.seed(),
      effect.id()
    ))
    const scaled = Math.round(Math.abs(amount) * 1000) / 10
    const value = Math.round(scaled) === scaled ? scaled.toFixed(0) : scaled.toFixed(1)
    const sign = amount >= 0 ? '+' : '-'
    const rgb = amount >= 0 ? 0x55FF55 : 0xFF5555
    // 用完整 Java 签名选择 TextColor 重载，避免 Rhino 在 ChatFormatting/TextColor 之间产生歧义。
    const style = SR_Style.EMPTY['withColor(net.minecraft.network.chat.TextColor)'](SR_TextColor.fromRgb(rgb))
    event.lines.add(SR_Component.literal(`${sign}${value}% ${label}`).setStyle(style))
  })
})
