# Critical Strike KubeJS Compat 设计规格

日期：2026-08-01

## 1. 目标与范围

为 Minecraft 1.21.1、NeoForge 21.1.x、KubeJS 2101 与 Critical Strike 1.21.1 构建一个独立附属模组。凡是进入 KubeJS `EntityEvents.beforeHurt` 所依赖的 `LivingDamageEvent.Pre` 管线，并且能够追踪到玩家攻击者的伤害，都可以使用 Critical Strike 的暴击率、暴击倍率、伤害标记与表现效果。

模组安装后零脚本自动生效，同时向整合包作者提供专用 KubeJS 事件，用于按攻击者、受击者和伤害来源过滤暴击，调整本次概率与倍率，强制结果，以及监听成功暴击。

项目目录为 `D:\Codex\critical-strike-kubejs-compat`，Mod ID 为 `critical_strike_kubejs_compat`，显示名为 `Critical Strike KubeJS Compat`。

## 2. 平台与依赖

- Java 21
- Minecraft 1.21.1
- NeoForge 21.1.x
- KubeJS 2101
- Critical Strike 的 1.21.1 版本
- 服务端负责全部暴击判定与伤害修改
- Critical Strike 与 KubeJS 均为必需依赖

本项目不复制或修改上游源码。编译时使用正式依赖，运行测试时加载真实的 Critical Strike 与 KubeJS。模组声明为双端可安装，但所有业务判定只在逻辑服务端执行。

## 3. 非目标

- 不为没有玩家攻击者的环境伤害创建虚拟攻击者。
- 不让普通生物读取玩家专属的 Critical Strike Attribute。
- 不替换 Critical Strike 原有近战与 `PersistentProjectileEntity` 判定。
- 不修改或持久化覆盖 Critical Strike 的服务端配置。
- 不保证绕过 NeoForge `LivingDamageEvent.Pre` 的自定义伤害系统能够兼容。
- 不提供针对单个枪械或魔法模组的硬编码适配。

## 4. 核心架构

### 4.1 事件桥

兼容桥以最低事件优先级监听 `LivingDamageEvent.Pre`。KubeJS 自带的 `EntityEvents.beforeHurt` 先运行并可能修改 `DamageContainer.newDamage`，兼容桥随后读取这个最新伤害值并应用暴击倍率。

仅在逻辑服务端执行以下流程：

1. 检查是否位于 Critical Strike 已原生处理的调用范围；若是则直接返回。
2. 从 `DamageSource.getEntity()` 解析负责伤害的实体。
3. 若负责实体不是玩家，则检查直接实体是否为标准弹射物，并从其拥有者解析玩家。
4. 若仍没有玩家攻击者，则直接返回。
5. 读取当前伤害、目标、直接实体、伤害来源以及玩家的 Critical Strike Attribute。
6. 构造可修改的 KubeJS 暴击上下文并发布 `modify` 事件。
7. 校验脚本修改后的概率、倍率和决策。
8. 执行暴击判定；成功后修改事件中的当前伤害，写入 Critical Strike 的暴击倍率标记并播放原有表现效果。
9. 发布只读的 `critical` 成功事件。

### 4.2 玩家攻击者解析

攻击者解析按以下顺序执行：

1. `DamageSource.getEntity()` 是玩家时直接使用。
2. `DamageSource.getDirectEntity()` 是标准 `Projectile` 且其拥有者是玩家时使用该拥有者。
3. 其他情况视为没有可追踪的玩家攻击者。

自定义模组若完全不把玩家或标准弹射物拥有者写入 `DamageSource`，兼容桥无法安全推断攻击者，因此不会触发暴击。

默认拒绝攻击者与受击者为同一玩家的自伤。自伤仍会进入专用 `modify` 事件，并以 `allowed = false` 开始；脚本作者可以显式调用 `allow()`。

### 4.3 原生判定防重

Critical Strike 已在玩家攻击与持久弹射物命中流程中执行暴击判定。兼容模组通过 Mixin 为 NeoForge/Mojmap 下的 `Player.attack` 与 `AbstractArrow.onHitEntity` 建立线程局部、可嵌套的原生判定范围，并使用 `try/finally` 保证退出时清理范围；后者对应 Critical Strike 通用源码中的 `PersistentProjectileEntity.onEntityHit`。

当 `LivingDamageEvent.Pre` 在该范围内触发时，兼容桥无条件跳过。这样可以同时覆盖以下情形：

- Critical Strike 原生判定成功；
- 原生随机判定失败；
- 原生近战或远程暴击被配置关闭；
- 近战武器要求不满足；
- `PersistentProjectileEntity` 的子类沿用或调用原生命中方法。

因此兼容模组不会进行第二次随机判定，也不会改变 Critical Strike 原生配置语义。自定义弹丸若覆盖命中逻辑且不调用原生 `onEntityHit`，其伤害不在原生范围内，会由通用事件桥处理。

## 5. 暴击数据与判定

默认值直接读取攻击玩家当前的：

- `critical_strike:chance`
- `critical_strike:damage`

兼容层使用 Critical Strike 公开的 Attribute 入口或 `CriticalStriker` 接口取得规范化概率与倍率，不复制常量。未被脚本覆盖时，概率和倍率与 Critical Strike 本体完全一致。

判定顺序如下：

1. `deny()` 或 `forceNormal()`：不暴击。
2. `forceCritical()`：跳过随机数并暴击。
3. 默认：使用最终概率进行一次服务端随机判定。

当脚本未覆盖概率且没有强制结果时，兼容层优先复用 Critical Strike 的原生概率判定入口，以保留其批处理配置语义。脚本覆盖概率后，按覆盖后的概率单独抽取一次随机数。

成功时：

- `criticalDamage = currentDamage * damageMultiplier`
- 将 `DamageContainer.newDamage` 设置为 `criticalDamage`
- 通过 Critical Strike 的 `CriticalDamageSource` 写入本次倍率
- 复用 Critical Strike 的服务端暴击粒子/音效入口；所有由兼容桥处理的非原生伤害使用其远程暴击音量配置

事件监听器读取的是普通 KubeJS `beforeHurt` 已修改后的 `currentDamage`，因此脚本的基础伤害调整会先于暴击乘算。

## 6. KubeJS API

注册全局事件组 `CriticalStrikeCompatEvents`。

### 6.1 `modify`

示例：

```js
CriticalStrikeCompatEvents.modify(event => {
  if (event.sourceType == 'mymod:bleeding') {
    event.deny()
    return
  }

  if (event.sourceType == 'mymod:arcane_bolt') {
    event.chance = 0.35
    event.damageMultiplier = 2.25
  }

  // event.allow()
  // event.forceCritical()
  // event.forceNormal()
})
```

可读字段：

- `attacker`：负责伤害的玩家
- `target`：受击生物
- `source`：原始 `DamageSource`
- `sourceType`：伤害类型注册表 ID 字符串，例如 `mymod:arcane_bolt`
- `directEntity`：子弹、魔法弹等直接实体，可能为空
- `damage`：普通 KubeJS `beforeHurt` 处理后的当前伤害
- `chance`：当前暴击率
- `damageMultiplier`：当前暴击倍率
- `allowed`：当前是否允许暴击
- `forcedResult`：`DEFAULT`、`CRITICAL` 或 `NORMAL`

可修改内容与方法：

- 设置 `chance`
- 设置 `damageMultiplier`
- `allow()`
- `deny()`
- `forceCritical()`
- `forceNormal()`
- `clearForcedResult()`

多个监听器按 KubeJS 的正常注册顺序处理同一上下文；后执行的监听器可以覆盖前一个监听器的字段或强制结果。

### 6.2 `critical`

示例：

```js
CriticalStrikeCompatEvents.critical(event => {
  console.info(`${event.attacker.username} 使用 ${event.sourceType} 造成暴击`)
  console.info(`伤害：${event.baseDamage} -> ${event.criticalDamage}`)
})
```

该事件只读，并提供：

- `attacker`
- `target`
- `source`
- `sourceType`
- `directEntity`
- `baseDamage`
- `criticalDamage`
- `chance`
- `damageMultiplier`
- `forced`

`critical` 表示暴击判定成功且倍率已经写入 `LivingDamageEvent.Pre`，不承诺目标最终损失生命。后续其他事件监听器仍可能修改或阻止伤害。此定义保证脚本可稳定用于日志、自定义粒子、音效、任务与统计联动，而无需建立跨事件生命周期状态。

## 7. 数值校验与错误处理

- 暴击率最终限制在 `0.0` 到 `1.0`。
- 暴击倍率最低限制为 `1.0`。
- `NaN`、正负无穷和负数不会写入伤害事件。
- 脚本提交非法概率或倍率时，兼容层记录包含字段名和来源的错误，并回退到该字段进入 KubeJS 事件前的有效值。
- 乘算结果不是有限非负数时，本次暴击不修改伤害，并记录错误。
- 无玩家攻击者、客户端事件、已在原生判定范围内的事件均安静跳过。
- KubeJS 事件没有监听器时不创建多余脚本调用对象以外的持久状态。
- 不捕获并吞掉 KubeJS 框架本身的脚本异常；遵循 KubeJS 的标准错误报告，同时保证伤害上下文保留最后一个通过校验的值。

## 8. 组件边界

- `CriticalStrikeKubeJSCompat`：NeoForge 入口与依赖检查，不承载判定逻辑。
- `CriticalDamageBridge`：`LivingDamageEvent.Pre` 监听、流程编排和成功效果。
- `PlayerDamageSourceResolver`：只负责从 `DamageSource` 安全解析玩家。
- `NativeCriticalScope`：线程局部、可嵌套的原生处理范围。
- 两个范围 Mixin：只负责包裹 `Player.attack` 和 NeoForge/Mojmap 的 `AbstractArrow.onHitEntity`。
- `CriticalAttemptContext`：一次判定的可修改 Java 数据模型及数值校验。
- `CriticalResultContext`：成功事件的不可修改快照。
- `CriticalStrikeCompatEvents`：KubeJS 事件组定义。
- `ModifyCriticalKubeEvent` 与 `CriticalSuccessKubeEvent`：脚本可见包装层。

核心解析、校验和决策逻辑不直接依赖全局事件总线，便于纯单元测试。KubeJS 专用类与核心桥分包，但由于 KubeJS 是必需依赖，不需要可选依赖反射或条件加载层。

## 9. 测试设计

### 9.1 单元测试

- `DamageSource` 的负责实体是玩家时解析成功。
- 标准弹射物拥有者是玩家时回退解析成功。
- 非玩家攻击者和无来源环境伤害解析失败。
- 自伤初始为拒绝，脚本调用 `allow()` 后可继续。
- 概率正常、越界、`NaN` 和无穷值校验。
- 倍率正常、低于一、负数、`NaN` 和无穷值校验。
- 默认、强制暴击、强制不暴击和拒绝的决策优先级。
- 嵌套原生范围正确计数并在退出后清理。
- 乘算溢出时不写入非法伤害。

### 9.2 NeoForge 集成测试与 GameTest

- 原版玩家近战只由 Critical Strike 判定一次。
- 原版箭矢与 `PersistentProjectileEntity` 子类只判定一次。
- 玩家归属的自定义法术伤害、实体弹丸、Hitscan 与爆炸进入兼容桥。
- 无玩家来源、环境伤害与默认自伤不会暴击。
- 实际读取 `critical_strike:chance` 与 `critical_strike:damage`。
- 普通 `EntityEvents.beforeHurt` 修改先于兼容暴击乘算。
- `modify` 的允许、拒绝、概率、倍率及强制结果生效。
- `critical` 仅在成功判定时发布一次。
- 暴击伤害来源标记和原有粒子/音效入口被调用。
- 真实 Critical Strike 与 KubeJS 同时加载后，专用事件完成注册且服务端启动成功。

随机相关测试使用可注入的判定器或固定随机源，不依赖概率碰运气。

### 9.3 构建验证

- `gradlew test`
- `gradlew classes`
- NeoForge 测试运行或服务器启动冒烟测试
- 最终 `gradlew build`
- 对生成 JAR 检查 mod 元数据、Mixin 配置、KubeJS 插件服务声明和示例文档

## 10. 文档与交付物

最终交付：

- 可安装的 NeoForge JAR
- 完整 Gradle 源码工程
- 中文 README
- 构建与安装说明
- KubeJS API 字段和事件时机说明
- 包含过滤伤害来源、修改概率/倍率、强制结果和成功监听的示例脚本
- 兼容范围说明，明确只有携带玩家归属信息并进入 `LivingDamageEvent.Pre` 的伤害能够自动兼容

## 11. 验收标准

在 Minecraft 1.21.1、NeoForge 21.1.x、KubeJS 2101 与 Critical Strike 1.21.1 环境中：

1. 不编写脚本时，所有进入 KubeJS 同一受伤事件管线且可追踪到玩家的非原生伤害自动使用 Critical Strike 暴击属性。
2. Critical Strike 原生近战与持久弹射物保持原行为，且每次伤害最多进行一次暴击随机判定。
3. 环境伤害、无玩家来源伤害和默认自伤不触发兼容暴击。
4. KubeJS 作者能过滤来源、修改概率与倍率、强制结果并监听一次成功暴击。
5. 普通 KubeJS `beforeHurt` 的基础伤害修改发生在暴击乘算之前。
6. 非法脚本数值不会产生负数、非有限伤害或服务端崩溃。
7. 单元测试、集成测试、启动冒烟测试和发布构建全部通过。
8. 交付 JAR、源码、中文文档和示例脚本齐全。
