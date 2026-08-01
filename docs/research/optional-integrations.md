# Minecraft 1.21.1 / NeoForge 可选集成研究（Curios + KubeJS）

研究日期：2026-08-01。结论仅依据本工作树和本机参考克隆：

- Curios：`%TEMP%\\superreforge-reference-20260801\\Curios-1.21.1`（源码 `1.21.1` 分支，`9.5.1+1.21.1`）。
- KubeJS：`%TEMP%\\superreforge-reference-20260801\\KubeJS-2101`（`mod_version=2101.7.2` 源码）。
- 目标：Super Reforge，Minecraft `1.21.1`、NeoForge `21.1.244`、Java 21。

本文的“安全”含义是：缺少 Curios/KubeJS 时，**不解析、不链接、不初始化**任何引用其类型的类；核心、数据包和原版 Attribute 功能仍可启动。

## 结论摘要

1. Curios 的“任意 Curios 饰品”判断应使用 `CuriosApi.getCurio(stack).isPresent()`，它最终查询 `CuriosCapability.ITEM`；不要把“当前正被装备在 Curios 栏位”与“该 ItemStack 具备 Curio capability”混为一谈。
2. 对任意 Curios 功能栏位附加 Super Reforge 的动态 Attribute，最小且正确的入口是 Curios 的 `CurioAttributeModifierEvent`（NeoForge EVENT_BUS）。仅在 `SlotContext` 的真实功能槽位上（`!cosmetic()`）且效果包含 `SlotTarget.CURIOS_ANY` 时，以稳定 `ResourceLocation` 构造 `AttributeModifier` 并调用 `event.addModifier(...)`。该事件同时用于服务端穿脱装计算和客户端 tooltip，故不能只在一侧注册。
3. KubeJS 2101 的正式 addon 入口是实现 `dev.latvian.mods.kubejs.plugin.KubeJSPlugin`，并在 mod JAR 根资源写 `kubejs.plugins.txt`。它支持按条件 token 在 `Class.forName` **之前**检查 `ModList.isLoaded`；推荐行：`com.mutuo.superreforge.compat.kubejs.SuperReforgeKubeJSPlugin kubejs`。
4. 定义收集必须绑定到 `SERVER` script reload：`beforeScriptsLoaded(ScriptManager)` 清空当轮 collector，脚本经绑定调用 `addLevel/addItemType/addModifier/addCatalyst/addPredicate`，`afterScriptsLoaded(ScriptManager)` 一次性调用现有 `SuperReforgeApi.replaceScriptDefinitions(...)` 和 `SelectorHooks.replaceScriptPredicates(...)`。`startup_scripts` 不适合可 `/reload` 的定义。
5. KubeJS 本体没有一个通用的“配方查看器 catalyst 注册” API；其 `RecipeViewerEvents` 只处理条目、说明、分类和配方的增删，不提供 catalyst。JEI/REI/EMI catalyst 必须是独立 optional bridge，不能伪称为 KubeJS 2101 通用能力。

## 1. Curios：判断、属性与数据组件

### 1.1 “是 Curio”与“当前在 Curios 槽”

**任意 Curio ItemStack** 的最小判断：

```java
boolean isCurio = CuriosApi.getCurio(stack).isPresent();
```

- 公共签名：`top.theillusivec4.curios.api.CuriosApi#getCurio(ItemStack)`，参考源码 `neoforge/src/main/java/top/theillusivec4/curios/api/CuriosApi.java:218-227`。
- 实现：`stack.getCapability(CuriosCapability.ITEM)`，参考 `neoforge/src/main/java/top/theillusivec4/curios/mixin/CuriosImplMixinHooks.java:122-124`。
- capability 常量：`CuriosCapability.ITEM`，类型 `ItemCapability<ICurio, Void>`，参考 `neoforge/src/main/java/top/theillusivec4/curios/api/CuriosCapability.java:39-50`。

这判断的是“stack 有 Curios capability”，包括 `ICurioItem` 注册的 item；它**不证明**该 stack 正在某个玩家的 Curios inventory 中。若需要已装备位置，应遍历 `CuriosApi.getCuriosInventory(livingEntity)` 的 `ICuriosItemHandler`/栈处理器，或在 Curios 自己计算属性时直接使用事件给出的 `SlotContext`。

`CuriosApi.isStackValid(slotContext, stack)` 是“能否放入某个指定 slot”的校验，不能替代任意 Curio 判断。它会匹配已知槽、`curio` 泛槽、Curios item tags、registered predicates 和最后的 capability fallback；实现见 `CuriosImplMixinHooks.java:135-178`。

### 1.2 向所有 Curios 功能栏位动态附加 Attribute（推荐）

监听：

```java
NeoForge.EVENT_BUS.addListener(CuriosCompat::onCurioAttributeModifiers);

private static void onCurioAttributeModifiers(CurioAttributeModifierEvent event) {
    SlotContext context = event.getSlotContext();
    if (context.cosmetic()) return;
    // ModifierResolver.resolve(event.getItemStack(), DefinitionManager.snapshot()) ...
    // 对每个 CURIOS_ANY 的 effect：
    event.addModifier(attributeHolder,
        new AttributeModifier(stableId, amount, operation));
}
```

- 事件类：`top.theillusivec4.curios.api.event.CurioAttributeModifierEvent`，在 `NeoForge.EVENT_BUS` 发出，参考 `.../CurioAttributeModifierEvent.java:36-48`。
- 精确写入方法：`boolean addModifier(Holder<Attribute>, AttributeModifier)`，参考同文件 `:94-105`。
- 可得上下文：`getItemStack()`（`:143-146`）、`getSlotContext()`（`:135-140`）、slot-unique 的 `getId()`（`:149-153`）。`SlotContext` record 字段为 `(identifier, entity, index, cosmetic, visible)`，参考 `common/.../SlotContext.java:25-35`。
- Curios 计算顺序：先取 `curios:attribute_modifiers` data component 或 `ICurio#getAttributeModifiers`，再 post 该事件；参考 `CuriosImplMixinHooks.java:181-225`。所以 event 方案适用于不拥有 item 类的任意 Curio，并且可与现有 modifier 合并。

这正符合 Super Reforge 当前模型：`VanillaAttributeApplicator` 已刻意跳过 `CURIOS_ANY`（`src/main/java/com/mutuo/superreforge/item/VanillaAttributeApplicator.java:35-39`），且定义中的值可由 `ModifierResolver` 根据 stack 的 seed 稳定解析（`ModifierResolver.java:16-38`）。应复用其 `stableModifierId` 的规则（目前是 private，需抽为 core 纯函数或在 compat 侧完全同构），绝不可为每次查询生成随机 UUID/ID。

重要生命周期约束：

- Curios 文档直接说明该事件服务端在穿脱装、客户端在 tooltip 都会触发，且两侧必须一致（`CurioAttributeModifierEvent.java:36-46`）。bridge 应注册在 common NeoForge bus，不要仅 `Dist.CLIENT` 注册，也不要只处理 `event.getSlotContext().entity()` 非空的情况。
- cosmetic slot 不应施加实体属性。该字段已经在 `SlotContext` 中，优先 `if (context.cosmetic()) return;`。
- 同一 `AttributeModifier` 的 `ResourceLocation id` 必须固定；Curios 也明确要求该 ID 在穿/脱装间一致（事件类 `:94-101`）。
- Attribute holder 可用 `BuiltInRegistries.ATTRIBUTE.getHolder(effect.attribute())`；缺失 attribute 沿用当前原版 applicator 的警告与跳过策略。

### 1.3 写入 Curios data component（可用，但不适合本项目的动态定义）

Curios 将自身持久组件注册为 `curios:attribute_modifiers`：

- 注册点：`CuriosRegistry.CURIO_ATTRIBUTE_MODIFIERS`，`DataComponentType<CurioAttributeModifiers>`，参考 `neoforge/.../CuriosRegistry.java:80-86`。
- 值类型：`CurioAttributeModifiers(List<Entry>, boolean showInTooltip)`，每个 entry 包含 `(attribute ResourceLocation, AttributeModifier, slot String)`，参考 `neoforge/.../CurioAttributeModifiers.java:37-44, 125-142`。
- 高层写法：
  - `CuriosApi.addModifier(stack, Holder<Attribute>, id, amount, operation, slot)`；公共签名 `CuriosApi.java:320-333`，实现会追加 component entry，`CuriosImplMixinHooks.java:241-260`。
  - 仅修改 Curios 槽数量/slot attribute 时：`CuriosApi.addSlotModifier(...)`（`CuriosApi.java:271-300`）。这不是给攻击力/护甲等普通 Attribute 加词条的首选。

不建议 Super Reforge 采用 data component 写入：Reforge 的效果来自可热重载的 `DefinitionSnapshot` 与 stack seed；把已解析数值持久写入 Curios 组件会残留旧定义、与 `REFORGE_DATA` 两份事实来源冲突，并需要在重铸、数据包 reload、移除词条时精确清理。事件计算能天然保持动态刷新和 tooltip 一致性。

## 2. KubeJS 2101：插件、事件、脚本与 API 面

### 2.1 插件发现和无依赖安全性

KubeJS 在构造阶段遍历已加载 mod 的资源；每个 mod 根路径的 `kubejs.plugins.txt` 会被读取：

- 发现文件：`KubeJSPlugins#loadMod`，`plugin/KubeJSPlugins.java:37-51`。
- 每行格式为 `完整类名 [client] [requiredModId ...]`；loader 对每个 token 先执行 `ModList.get().isLoaded(...)`（`:56-77`），只在条件满足后才 `Class.forName(line[0])`（`:79-94`）。
- README 的官方用法与例子在 `README.md:68-86`。

因此资源文件应为：

```text
# src/main/resources/kubejs.plugins.txt
com.mutuo.superreforge.compat.kubejs.SuperReforgeKubeJSPlugin kubejs
```

即使 Super Reforge JAR 内含该 class，KubeJS 缺失时没有任何 loader 会读取此资源或加载该类；KubeJS 存在时 token 又在 `Class.forName` 前形成保护。不要让主 mod entry、mixin JSON、`@EventBusSubscriber` 扫描的常驻类、或 core public API 引用 `dev.latvian.mods.kubejs.*`。

插件接口及可用 hooks：

- `dev.latvian.mods.kubejs.plugin.KubeJSPlugin`，`plugin/KubeJSPlugin.java:47-174`。
- `registerEvents(EventGroupRegistry)`（`:63-67`）注册脚本可见的 EventGroup；`registerBindings(BindingRegistry)`（`:69-73`）提供 JS 全局对象。
- `beforeScriptsLoaded(ScriptManager)` / `afterScriptsLoaded(ScriptManager)`（`:163-167`）是一次 reload 的边界；`ScriptManager#reload` 的调用顺序已固定为 before → load scripts → after，见 `script/ScriptManager.java:46-70`。
- `BindingRegistry#add(String,Object)`（`script/BindingRegistry.java:5-14`）向当前 scope 暴露对象；每创建一个 KubeJS context 都会调用每个 plugin 的 `registerBindings`，见 `script/KubeJSContext.java:38-51`。因此 binding 本身不能“提交一次全局定义”；它必须写入当前 `ScriptManager` 对应的 reload collector。

### 2.2 建议的 SuperReforge API 形状和提交时机

在 KubeJS compat class 中实现如下最小面（名称可保留为用户希望的 JS API）：

```js
// kubejs/server_scripts/super_reforge.js
SuperReforge.addLevel('example:rare', { rank: 20, name: { text: 'Rare' } })
SuperReforge.addItemType('example:tools', { include: [{ tag: 'minecraft:tools' }] })
SuperReforge.addModifier('example:swift', { /* ModifierDefinition codec object */ })
SuperReforge.addCatalyst('example:stone', { /* CatalystDefinition codec object */ })
SuperReforge.addPredicate('example:durable', stack => stack.maxDamage > 100)

ServerEvents.loaded(event => {
  SuperReforge.setStageActive(event.server, 'hardmode', true)
})
```

Java 侧建议：

1. 对 `manager.scriptType == ScriptType.SERVER`，`beforeScriptsLoaded` 新建/清空一个 `Collector`（levels、itemTypes、modifiers、catalysts、predicates）。不要在 STARTUP/CLIENT collector 中修改服务器权威定义。
2. `registerBindings` 中通过 `bindings.context().kjsFactory.manager` 定位当前 manager；只给 SERVER scope 注入可变的定义 facade。它的 `addLevel` 等把 codec/显式 JS 参数转换为 core 的 `LevelDefinition`、`ItemTypeDefinition`、`ModifierDefinition`、`CatalystDefinition`，并写成 `DefinitionContribution` 或直接写临时 `DefinitionLayer.Builder`。
3. `afterScriptsLoaded` 且类型为 SERVER：将完整 collector 以**一次** `SuperReforgeApi.replaceScriptDefinitions(contributions)` 提交；成功后才 `SelectorHooks.replaceScriptPredicates(predicates)`。现有 API 已保证整个脚本层原子替换（`src/main/java/com/mutuo/superreforge/api/SuperReforgeApi.java:12-21`；`DefinitionManager.java:49-59`）。失败时保留前一份脚本层/谓词，不要出现半提交。
4. `addPredicate(id, callback)` 应登记为 `Predicate<ItemStack>` 并由 `SelectorHooks` 保存；核心现有 hook 已按 ID 查找（`src/main/java/com/mutuo/superreforge/reforge/SelectorHooks.java:8-32`），`ItemSelector.kubejsPredicate` 已有 codec 字段（`definition/ItemSelector.java:12-24`）。必须把 Rhino callback 的调用限制在该 server script context/服务器线程；不要把 callback 放到异步 reload/IO 线程执行。

`startup_scripts` 与 `server_scripts` 的含义不可混用：KubeJS 明确将 `startup_scripts` 定义为只在游戏启动加载、用于注册加载期内容；`server_scripts` 在每次 server resource reload 时加载、用于 recipes/tags/loot/server events（`KubeJS.java:77-94`）。路径常量也分别是 `kubejs/startup_scripts` 和 `kubejs/server_scripts`（`KubeJSPaths.java:34-40`）。本项目定义与 predicate 会随 `/reload` 生效，故应进入 server scripts + plugin reload hooks。

### 2.3 自定义 EventGroup（仅在确实需要事件名时）

若要暴露 `SuperReforgeEvents.*`，定义静态 group 并在 plugin 中注册：

```java
public interface SuperReforgeEvents {
    EventGroup GROUP = EventGroup.of("SuperReforgeEvents");
    EventHandler DEFINITIONS = GROUP.server("definitions", () -> SuperReforgeDefinitionsEvent.class);
}

@Override
public void registerEvents(EventGroupRegistry registry) {
    registry.register(SuperReforgeEvents.GROUP);
}
```

- `EventGroup.of/add/server/common`：`event/EventGroup.java:10-58`。
- `EventGroupRegistry#register`：`event/EventGroupRegistry.java:1-5`。
- KubeJS 仅把 plugin 注册的 groups 放进全局 `EventGroups.ALL`（`event/EventGroups.java:9-19`），并作为 `SuperReforgeEvents` binding 加到 scope（`plugin/builtin/BuiltinKubeJSPlugin.java:409-416`）。因此只声明常量而不覆写 `registerEvents` 时，JS 不可见。
- `EventHandler.listen` 只允许在 script loading 期间注册（`event/EventHandler.java:111-120`）。定义加载可直接用前述 plugin hooks，不需要多加一个 custom event；custom group 的价值在于给脚本作者明确的扩展事件，而不是作为提交事务的唯一保障。

### 2.4 服务端共享 progress stage API

权威状态应继续使用项目已有 `SavedData`，而不是 KubeJS `server.persistentData`：

- `ProgressService.setActive(MinecraftServer,String,boolean)` 与 `highestActive(MinecraftServer)` 是现成服务接口，`src/main/java/com/mutuo/superreforge/progress/ProgressService.java:28-48`。
- 数据实际保存在 Overworld `DataStorage` 的 `superreforge_progress`，并在变更时 `setDirty()`，`ServerProgressData.java:13-49`。这是跨维度、跨重启的服务器共享语义。
- KubeJS 自身也给 `MinecraftServer` 提供 `persistentData`，其 mixin 以 `kubejs_persistent_data.nbt` 读取/保存（`server/KubeJSServerEventHandler.java:44-46, 57-103, 122-160`）；它适合 KubeJS 自己的数据，但会让无 KubeJS 服务器无法再读取 progress，故不应成为 Super Reforge stage 的 source of truth。

推荐只在 SERVER binding 暴露无状态 facade：

```java
void setStageActive(MinecraftServer server, String id, boolean active) {
    ProgressService.setActive(server, id, active);
}
Optional<ProgressStage> highestActive(MinecraftServer server) {
    return ProgressService.highestActive(server);
}
```

若需要 JS 查询“某 stage 是否 active”，当前 core 没有 public `isActive`；应最小新增 `ProgressService.isActive(MinecraftServer,String)`（内部读 `data(server).activeStages()`），而不是让 compat 反射或触碰 `ServerProgressData` 私有字段。若希望 JS 添加 stage 定义，当前可调用 `replaceStages(Collection<ProgressStage>)`，但它会替换**全部**静态表（`ProgressService.java:17-26`），没有增量安全 API；应先设计一个与 DefinitionLayer 同样的原子、带重复 ID 校验的 stage collector，再公开 `addStage`。不能安全地把这件事作为 `addLevel` 的副作用，因为 `LevelDefinition` 与 `ProgressStage` 的字段/生命周期并不相同。

`ServerEvents.loaded` 是可用的服务器实例事件：`ServerEvents.LOADED` 定义于 `plugin/builtin/event/ServerEvents.java:29-46`，在 NeoForge `ServerStartingEvent` 时 post（`server/KubeJSServerEventHandler.java:100-108`），event 参数 `ServerKubeEvent#getServer()` 位于 `server/ServerKubeEvent.java:6-15`。它适合脚本激活 stage；不要在 startup scripts 触发 server-state 改写。

### 2.5 关于 addCatalyst / recipe viewer catalyst

`SuperReforge.addCatalyst(...)` 应仅指向本项目的 `CatalystDefinition`（字段见 `definition/CatalystDefinition.java:9-36`），并最终交给 `DefinitionLayer.Builder#catalyst`（`DefinitionLayer.java:29-57`）。它与 JEI/REI/EMI 中“一个配方类别的催化展示物”不是同一概念。

KubeJS 2101 的 `RecipeViewerEvents` 只有 `addEntries`、`removeEntries`、`groupEntries`、`addInformation`、`registerSubtypes`、`removeCategories`、`removeRecipes`，见 `plugin/builtin/event/RecipeViewerEvents.java:16-28`；没有 add-catalyst handler。故如果未来需要配方查看器显示 Reforger：

- 为 JEI、REI、EMI 各写独立 optional compat，并分别用它们自身 API；
- KubeJS bridge 不应承诺 `addRecipeViewerCatalyst`，除非同时明确它只支持某一 viewer 且其 mod ID 已经条件保护；
- 当前工作树没有任何 recipe viewer dependency，不能在本次“最小 KubeJS”集成中安全实现此能力。

## 3. Gradle：仓库、坐标和版本

### 3.1 当前目标项目基线

`build.gradle` 目前 `repositories {}` 为空（`:30-32`），dependencies 也尚未包含 Curios/KubeJS（`:124-156`）。目标版本为 MC `1.21.1` / NeoForge `21.1.244`（`gradle.properties:15-21`）。`neoforge.mods.toml` 已正确声明 optional dependency：Curios `[9,)`、KubeJS `[2101,)`。

建议补入的最小开发依赖形状（版本以锁定值变量管理）：

```groovy
repositories {
    maven { url = 'https://maven.theillusivec4.top/' } // Curios 发布仓库
    maven {
        url = 'https://maven.latvian.dev/releases'
        content { includeGroup 'dev.latvian.mods' }
    }
}

dependencies {
    compileOnly "top.theillusivec4.curios:curios-neoforge:${curios_version}"
    localRuntime "top.theillusivec4.curios:curios-neoforge:${curios_version}"

    compileOnly "dev.latvian.mods:kubejs-neoforge:${kubejs_version}"
    localRuntime "dev.latvian.mods:kubejs-neoforge:${kubejs_version}"
}
```

`compileOnly` 防止转递发布；`localRuntime` 是本项目 ModDevGradle 模板已定义并说明为“运行测试时存在、但不发布给使用者”的配置（`build.gradle:113-121, 124-156`）。不能用 `implementation` 或把 JAR 打进成品；这会破坏 optional 语义。

### 3.2 Curios（可由本地源码精确导出）

| 项 | 本地依据 | 结论 |
|---|---|---|
| group | `Curios/gradle.properties:5-6` | `top.theillusivec4.curios` |
| version | `Curios/gradle.properties:5` | `9.5.1+1.21.1` |
| NeoForge artifactId | `Curios/buildSrc/src/main/groovy/multiloader-common.gradle:6-8`（`"${mod_id}-${project.name}"`）且子项目为 `neoforge` | `curios-neoforge` |
| 坐标 | 上三项 | `top.theillusivec4.curios:curios-neoforge:9.5.1+1.21.1` |
| API classifier | `multiloader-common.gradle:16-26` 创建 classifier `api`；正常开发可直接使用主 artifact | 可选：`:api` |

参考 Curios 工程自己的 `repositories` 仅列 CurseMaven 和 JEI/EMI/REI 的开发依赖仓库（`neoforge/build.gradle:49-81`），其 publishing URL 来自 CI 环境变量（`buildSrc/.../multiloader-common.gradle:115-133`），**不能仅从该源码证明消费者应使用哪个公开 Maven URL**。上面的 `maven.theillusivec4.top` 是 Curios 通常的发布仓库建议；实施前必须运行 Gradle dependency resolution 进行一次实际验证。若该仓库/版本不可解析，不能以 CurseMaven 的 mod 文件替代 API 坐标来继续编译。

### 3.3 KubeJS（artifact 版本不能从本地源码唯一锁定）

| 项 | 本地依据 | 结论 |
|---|---|---|
| group | `KubeJS/gradle.properties:5-7` | `dev.latvian.mods` |
| artifactId | 同文件 `:5`，以及 `build.gradle:189-194` | `kubejs-neoforge` |
| Maven repo | `KubeJS/build.gradle:102-109` | `https://maven.latvian.dev/releases`（限制 group `dev.latvian.mods`） |
| 源码版本基础 | `KubeJS/gradle.properties:12-16` | `2101.7.2`，MC `1.21.1` |
| 发布 version | `KubeJS/build.gradle:12-20` | `2101.7.2-local.<epoch>`（本地）或 `2101.7.2-build.<BUILD_NUMBER>`（CI） |

所以坐标形状是：

```text
dev.latvian.mods:kubejs-neoforge:<已发布的 2101.7.2-build.N>
```

不能把裸 `2101.7.2` 写进 dependency 并声称已验证，因为源码构建脚本明确从不发布该裸版本。应从实际运行环境（或 Maven metadata）选择发布的 `2101.7.x-build.N`，然后以该精确版本写入 `kubejs_version` 并执行 Gradle resolution/游戏运行验证。项目的 metadata 下界 `[2101,)` 可保留，但编译 API 仍应锁定一个确切 build。

## 4. 缺依赖时防止类加载崩溃的规则

### Curios bridge

1. 只让 core（`com.mutuo.superreforge` 的非 compat 包）使用 Minecraft/NeoForge/项目自身类型；不得出现 Curios import、字段、方法参数、泛型实参、注解 class literal 或静态初始化引用。
2. 把所有 `top.theillusivec4.curios.*` import 放进单独的 `compat.curios.CuriosCompat`。该类不要使用 `@EventBusSubscriber`，避免 NeoForge 扫描期主动加载。
3. 主入口在检查 `ModList.get().isLoaded("curios")` 后再以字符串反射加载并调用一个无 Curios 类型的启动约定，例如 `Class.forName("...CuriosCompat").getMethod("initialize").invoke(null)`。仅成功加载后，bridge 自己才调用 `NeoForge.EVENT_BUS.addListener(CuriosCompat::onCurioAttributeModifiers)`。
4. 不能只把 `if (isLoaded)` 包在直接调用外层：如果主类的静态字段、method descriptor 或 lambda target 已含 Curios 类型，JVM 可能在判断之前链接失败。也不能把兼容层放入 core mixin 配置。

### KubeJS bridge

1. `compat.kubejs.SuperReforgeKubeJSPlugin` 可直接实现 `KubeJSPlugin`，但只通过 `kubejs.plugins.txt` 被 KubeJS loader 加载；加上 `kubejs` 条件 token。
2. KubeJS 未安装时，不要从 `SuperReforge` main constructor、`@EventBusSubscriber`、Mixin、服务注册表或 core API 主动触及该 plugin class。
3. 该 plugin 的 `registerBindings` 只能暴露 core 类/DTO；任何 KubeJS-specific 参数都留在 compat 包，且 server collector 的生命周期随 `ScriptManager` reload 清理。
4. 现有 `SelectorHooks` 的默认 Curios matcher 是 `false`，script predicates 是空 map（`SelectorHooks.java:8-32`），这正是缺依赖时应保持的降级状态。

### 不能安全实现/需要额外设计的点

- **不能用 Curios capability 判断“已装备”**：`getCurio(stack)` 仅说明 stack 支持 Curio 行为；若业务依赖实际佩戴者与栏位，必须使用 `CurioAttributeModifierEvent` 的 `SlotContext` 或 inventory handler 枚举。
- **不能把 Curios data component 当作动态 reforge 的唯一存储**：会与现有 `ReforgeData`、热重载 snapshot 和 deterministic seed 产生陈旧值冲突。
- **不能把 KubeJS `persistentData` 作为服务器共享 stage 的唯一真相**：无 KubeJS 时不可用，且已有 `SavedData` 正确解决此问题。
- **不能从 KubeJS 源码唯一确定发布 artifact 的 build suffix**：需解析仓库 metadata 或依据用户安装的 JAR 版本验证。
- **不能通过 base KubeJS 实现 JEI/REI/EMI catalyst**：没有对应通用接口；需另行对每个 viewer 加 compileOnly/localRuntime、条件插件入口和 API 兼容测试。
- **脚本异常时不可发布部分定义**：collector 必须在 `afterScriptsLoaded` 做一次完整校验/替换；目前 `replaceScriptDefinitions` 已在验证失败时保留旧 ACTIVE/SCRIPT 引用，应该利用而不是绕过。

## 实施顺序（最小风险）

1. 先把两套 compileOnly + localRuntime 坐标放入 Gradle，并只做 dependency resolution；KubeJS 版本取实际已发布 build。
2. 新建无扫描注解的 Curios bridge，按 `ModList` 后反射加载；以 `CurioAttributeModifierEvent` 驱动 `CURIOS_ANY`。
3. 新建 `kubejs.plugins.txt` 与 KubeJS plugin；先实现 SERVER reload collector + 四类定义 API + predicate 的原子发布。
4. 仅在 core 增加小而稳定的 `ProgressService.isActive`（如需要查询）和单独的 stage-definition collector；stage state 仍留在 `ServerProgressData`。
5. 最后在四种组合做启动验证：无依赖、仅 Curios、仅 KubeJS、两者都有；并验证 `/reload`、Curios tooltip/穿脱、服务器重启后的 stage 持久化。
