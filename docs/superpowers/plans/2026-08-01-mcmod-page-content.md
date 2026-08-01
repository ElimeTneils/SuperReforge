# Super Reforge MC 百科文案 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 生成一份供模组作者核对和改写的 MC 百科主页底稿与独立教程，准确覆盖 Super Reforge 的主要功能、基础玩法及添加、覆盖、删除定义案例。

**Architecture:** 主页底稿与教程分成两个独立 Markdown 文件：主页只承担快速说明和主要功能展示，教程承担操作、概率、配置和扩展案例。所有事实先由仓库元数据、内置资源、示例和测试交叉核对；文案完成后按 MC 百科规则做措辞、版本和功能状态审计。

**Tech Stack:** Markdown、Minecraft 1.21.1、NeoForge 21.1.244、Super Reforge 0.1.0、JSON datapack、KubeJS 2101。

## Global Constraints

- 中文名称使用“超级重铸”，原名使用“Super Reforge”。
- 只写 Minecraft 1.21.1、NeoForge 21.1.244 或同 Minecraft 版本后续兼容构建。
- Curios 9.x 与 KubeJS 2101 均为可选依赖，不得写成必需前置。
- 不宣称兼容所有模组；只说明可引用已注册 Attribute，并可按 ID、标签、Curios 或 KubeJS 谓词接入物品。
- 不把正在开发但尚未完成验证的 GUI 滚动、悬停详情、锤子校正或独立创造页签写成已发布功能。
- 使用中立、简明、准确的第三人称表述；不使用主观强度评价、广告式措辞或不确定说法。
- 最终文本是作者核对和改写用底稿，不直接代替作者对 MC 百科投稿内容的人工确认。

---

### Task 1: 建立当前发布状态事实清单

**Files:**
- Read: `gradle.properties`
- Read: `src/main/templates/META-INF/neoforge.mods.toml`
- Read: `README.md`
- Read: `CHANGELOG.md`
- Read: `docs/CONFIGURATION.md`
- Read: `docs/DATAPACK_API.md`
- Read: `docs/KUBEJS_API.md`
- Read: `src/main/resources/data/superreforge/superreforge/**`
- Read: `examples/datapack/**`
- Read: `examples/kubejs/**`
- Create: `docs/MCMOD_FACT_CHECK.md`

**Interfaces:**
- Consumes: 仓库当前 HEAD、工作树状态、内置定义和公开配置/API 文档。
- Produces: 只含已实现事实、版本、依赖、默认内容数量、待验证功能和禁写声明的核对表，供后续两篇文案引用。

- [ ] **Step 1: 记录工作树与当前提交，区分已提交功能和并发开发内容**

Run:

```powershell
git status --short
git log -8 --oneline
```

Expected: 输出当前提交以及所有未提交文件；任何只出现在未提交文件中的功能标记为“不得写入成稿”。

- [ ] **Step 2: 核对版本、依赖与发布元数据**

Run:

```powershell
rg -n "^(minecraft_version|neo_version|mod_version|mod_name|mod_authors|curios_version|kubejs_version)=" gradle.properties
rg -n "modId=|type=|versionRange=|side=" src/main/templates/META-INF/neoforge.mods.toml
```

Expected: 确认 Minecraft 1.21.1、NeoForge 21.1.244、模组版本 0.1.0、作者 MUTUO，以及 Curios/KubeJS 为 optional、BOTH。

- [ ] **Step 3: 核对默认内容数量与名称**

Run:

```powershell
(rg --files src/main/resources/data/superreforge/superreforge/levels | Measure-Object).Count
(rg --files src/main/resources/data/superreforge/superreforge/item_types | Measure-Object).Count
(rg --files src/main/resources/data/superreforge/superreforge/modifiers | Measure-Object).Count
(rg --files src/main/resources/data/superreforge/superreforge/catalysts | Measure-Object).Count
```

Expected: 8 个等级、7 个物品类型、24 个词条、3 个媒介。

- [ ] **Step 4: 创建事实清单**

Create `docs/MCMOD_FACT_CHECK.md` with these exact sections:

```markdown
# Super Reforge MC 百科事实核对表

## 发布元数据
## 已实现的玩家功能
## 默认内容
## 可选兼容
## 数据包与 KubeJS 能力
## 服务器配置
## 尚未完成或尚未验证，禁止写入成稿
## 投稿前需由作者人工确认
```

Each statement must cite a local file path or verification command in parentheses.

- [ ] **Step 5: 自检事实清单并提交**

Run:

```powershell
rg -n "待补充|占位符|稍后完成|最新版|旧版|兼容所有|支持所有" docs/MCMOD_FACT_CHECK.md
git diff --check
```

Expected: `rg` 无输出，`git diff --check` 无输出。

Commit only the fact-check file:

```powershell
git add -- docs/MCMOD_FACT_CHECK.md
git commit -m "docs: add MC百科 fact checklist"
```

### Task 2: 编写模组主页底稿和主要功能

**Files:**
- Read: `docs/MCMOD_FACT_CHECK.md`
- Create: `docs/MCMOD_PAGE_COPY.md`

**Interfaces:**
- Consumes: Task 1 的已实现事实与禁写边界。
- Produces: 可供作者改写的模组基本信息、主页正文、主要功能、依赖与兼容说明、投稿字段建议。

- [ ] **Step 1: 写入投稿字段建议**

Create the opening metadata block:

```markdown
# 超级重铸（Super Reforge）MC 百科主页底稿

> 本文是供模组作者核对和改写的参考底稿，请勿未经人工修改直接提交。

## 投稿字段建议

- MOD 名称：超级重铸
- MOD 原名：Super Reforge
- 作者/团队：MUTUO
- 支持的 Minecraft 版本：1.21.1
- 加载方式：NeoForge
- 运行方式：客户端与服务端均需安装
- 所属分类建议：综合类
- 推荐标签：重铸、词条、数据驱动、KubeJS、Curios
```

- [ ] **Step 2: 编写主页介绍**

The introduction must contain four compact paragraphs in this order:

1. 模组主题与适用对象。
2. 熔核锻台的基础操作与成本预览。
3. 数据驱动、动态前缀和 Attribute 的工作方式。
4. Curios/KubeJS 可选兼容与未安装时的核心行为。

Do not include code blocks in the introduction.

- [ ] **Step 3: 编写主要功能列表**

Use exact headings and cover each fact once:

```markdown
## 主要功能

### 熔核锻台与服务器判定
### 品质、词条与相对权重
### 动态前缀与 Attribute
### 多物品类型合并
### 数据包与 KubeJS 自定义
### Curios 可选兼容
### 服务器配置与进度阶段
```

- [ ] **Step 4: 加入依赖、版本与教程入口**

Add `## 安装与依赖` and `## 使用教程` sections. The dependency section must distinguish required NeoForge from optional Curios/KubeJS. The tutorial section must summarize the six-step basic flow and point to `MCMOD_TUTORIAL.md` without replacing the summary with a bare link.

- [ ] **Step 5: 审核主页底稿并提交**

Run:

```powershell
rg -n "我觉得|笔者|小编|非常强大|必装|貌似|据说|最新版|旧版|未完待续|欢迎补充|兼容所有|支持所有|！！！|？？？|\.\.\." docs/MCMOD_PAGE_COPY.md
rg -n "Minecraft 1\.21\.1|NeoForge 21\.1\.244|Curios|KubeJS|MUTUO" docs/MCMOD_PAGE_COPY.md
git diff --check
```

Expected: 第一条命令无输出；第二条命令包含所有五项事实；`git diff --check` 无输出。

Commit only the page copy:

```powershell
git add -- docs/MCMOD_PAGE_COPY.md
git commit -m "docs: draft MC百科 mod page"
```

### Task 3: 编写独立教程与添加、覆盖、删除案例

**Files:**
- Read: `docs/MCMOD_FACT_CHECK.md`
- Read: `docs/CONFIGURATION.md`
- Read: `docs/DATAPACK_API.md`
- Read: `docs/KUBEJS_API.md`
- Read: `examples/datapack/data/example/superreforge/**`
- Read: `examples/kubejs/superreforge_definitions.js`
- Create: `docs/MCMOD_TUTORIAL.md`

**Interfaces:**
- Consumes: Task 1 的事实清单、生产 API 文档和仓库内可运行示例。
- Produces: 可单独发布的完整教程，包含玩家操作、概率、配置、数据包、KubeJS 覆盖、删除/恢复和常见问题。

- [ ] **Step 1: 创建教程骨架**

Create these exact sections:

```markdown
# 超级重铸完整使用与自定义教程

## 适用版本与安装
## 基础重铸流程
## 默认品质、物品类型与媒介
## 概率如何计算
## 服务器配置
## 使用数据包添加自定义内容
## 使用 KubeJS 覆盖同 ID 定义
## 删除定义与处理已有物品
## 常见失败提示
## 投稿前作者核对
```

- [ ] **Step 2: 编写玩家操作、成本和概率章节**

Document target count `1`, catalyst matching, material count, experience payment, server-side preview, default animation `20 tick`, two-stage weighted selection, and the joint probability formula:

```text
某词条最终概率 = 该品质归一化概率 × 该品质内该词条归一化概率
```

Include one numeric example using level weights `20, 30, 30`, yielding `25%, 37.5%, 37.5%` after normalization.

- [ ] **Step 3: 编写服务器配置章节**

Describe every key from `serverconfig/superreforge-server.toml`:

```text
experienceEnabled
experienceMode
automaticInitialModifier
automaticCatalyst
showAttributeLines
animationTicks
creativePlayersPay
missingDefinitionPolicy
```

State the exact `LEVELS`/`POINTS` difference and `REMOVE`/`KEEP_INACTIVE` behavior.

- [ ] **Step 4: 编写一组连续的数据包案例**

Use namespace `example` and resource IDs already present under `examples/datapack`. Include `pack.mcmeta` plus one level, item type, modifier, and catalyst JSON. Copy the production JSON shapes exactly; do not convert the readable `.jsonc` files into installable examples without removing comments.

- [ ] **Step 5: 编写同 ID KubeJS 覆盖案例**

Use `kubejs/server_scripts/super_reforge.js` and call:

```js
SuperReforge.addModifier('example:legendary_blade', {
  level: 'example:legendary',
  item_types: ['example:custom_weapons'],
  name: { text: '熔铸传说', color: 'gold' },
  weight: 10,
  attributes: [{
    id: 'damage',
    attribute: 'minecraft:generic.attack_damage',
    amount: { min: 0.06, max: 0.10 },
    operation: 'add_multiplied_base',
    slots: ['mainhand'],
    show_in_tooltip: true
  }]
})
```

Explain that a successful `server_scripts` reload makes the script definition replace the datapack definition with the same ID as one atomic publication.

- [ ] **Step 6: 编写删除与恢复案例**

Document these exact state transitions:

```text
删除 KubeJS 同 ID 定义并成功 reload
→ 数据包中的 example:legendary_blade 重新生效

再删除数据包中的 example:legendary_blade.json 并执行 /reload
→ 活动定义中不再存在该 ID

missingDefinitionPolicy = REMOVE
→ 已有物品上的失效词条数据会被移除

missingDefinitionPolicy = KEEP_INACTIVE
→ 保留词条 ID 与随机种子，但暂不显示名称或提供 Attribute；恢复同 ID 定义后重新生效
```

- [ ] **Step 7: 验证 JSON 与 API 名称**

Run:

```powershell
Get-ChildItem examples/datapack/data -Recurse -Filter *.json | ForEach-Object { Get-Content -Raw -Encoding UTF8 $_.FullName | ConvertFrom-Json | Out-Null }
rg -n "addLevel|addItemType|addModifier|addCatalyst|addPredicate|addStage|setStageActive|getActiveStage" src/main/java/com/mutuo/superreforge/compat/kubejs docs/KUBEJS_API.md
```

Expected: all JSON parses without error; every documented KubeJS API name appears in production bindings and documentation.

- [ ] **Step 8: 审核教程并提交**

Run:

```powershell
rg -n "待补充|占位符|稍后完成|最新版|旧版|非常强大|必装|兼容所有|支持所有|\.jsonc.*直接" docs/MCMOD_TUTORIAL.md
rg -n "REMOVE|KEEP_INACTIVE|server_scripts|/reload|weight|20 tick" docs/MCMOD_TUTORIAL.md
git diff --check
```

Expected: 第一条命令无输出；第二条命令包含所有六项关键语义；`git diff --check` 无输出。

Commit only the tutorial:

```powershell
git add -- docs/MCMOD_TUTORIAL.md
git commit -m "docs: add Super Reforge usage tutorial"
```

### Task 4: 最终一致性与 MC 百科投稿检查

**Files:**
- Read: `docs/MCMOD_FACT_CHECK.md`
- Modify: `docs/MCMOD_PAGE_COPY.md`
- Modify: `docs/MCMOD_TUTORIAL.md`
- Read: `docs/superpowers/specs/2026-08-01-mcmod-page-content-design.md`

**Interfaces:**
- Consumes: 前三项任务的事实清单、主页底稿和教程。
- Produces: 事实一致、没有占位符、没有未完成功能承诺、便于作者逐段人工改写的最终底稿。

- [ ] **Step 1: 对照规格逐项检查覆盖范围**

Confirm the page copy covers introduction, main functions, dependencies and tutorial summary. Confirm the tutorial covers installation, operation, probability, configuration, datapack, KubeJS override, deletion, existing-item behavior and common failures.

- [ ] **Step 2: 检查未完成功能是否误入成稿**

Run:

```powershell
rg -n "滚动条|拖动滚动|悬停详情|独立创造|锤子穿模|竖直动力锻锤" docs/MCMOD_PAGE_COPY.md docs/MCMOD_TUTORIAL.md
```

Expected: 无输出，除非执行时这些功能已经提交、通过测试并被重新加入事实清单。

- [ ] **Step 3: 运行完整文档与项目验证**

Run:

```powershell
git diff --check
.\gradlew.bat test classes --console=plain
```

Expected: `git diff --check` 无输出；Gradle build successful，全部测试通过。

- [ ] **Step 4: 记录作者投稿前人工核对项**

Ensure both deliverables end with a non-postable checklist covering:

```text
实际启动版本
客户端/服务端安装方式
Curios 与 KubeJS 可选关系
配方和游戏截图
外部链接有效性
作者亲自改写与事实确认
```

- [ ] **Step 5: 提交最终修订**

Run:

```powershell
git add -- docs/MCMOD_PAGE_COPY.md docs/MCMOD_TUTORIAL.md
git diff --cached --check
git commit -m "docs: finalize MC百科 submission draft"
```

Expected: only the two deliverable files are staged; commit succeeds without including concurrent mod implementation files.
