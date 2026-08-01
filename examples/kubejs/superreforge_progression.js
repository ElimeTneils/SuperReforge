// 放置位置：kubejs/server_scripts/superreforge_progression.js
// 阶段定义随 server_scripts reload 发布；激活状态存入服务器世界并由全服共享。

// 成本公式：floor(基础成本 × multiplier) + addition；材料和经验分别计算。
SuperReforge.addStage(
  'example:dragon_defeated',
  100, // priority：同时激活多个阶段时，只采用最高优先级阶段。
  1.5, // materialMultiplier
  1,   // materialAddition
  2.0, // experienceMultiplier
  0    // experienceAddition
)

ServerEvents.loaded(event => {
  // 这里只演示 API。正式整合包可在击杀末影龙等事件中调用同一句来激活阶段。
  SuperReforge.setStageActive(event.server, 'example:dragon_defeated', true)

  // 返回空字符串表示当前没有已激活阶段，否则返回生效阶段 ID。
  console.info(`Super Reforge 当前阶段：${SuperReforge.getActiveStage(event.server)}`)
})
