# Super Reforge Reforger Polish Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 完成可滚动且可悬停查看 Attribute 详情的“熔核锻造日志”GUI，修复动力锻锤穿模，并增加独立的 Super Reforge 创造模式页签和新的可安装 JAR。

**Architecture:** 服务端继续负责候选池与最终概率，预览负载只增加安全上限提示；客户端把预览扁平化为可测试的日志行模型，再由屏幕负责裁剪、滚动和提示框。动态锤子改为头部在下的专用 3D 模型，渲染器只施加方块朝向和垂直位移；创造页签通过独立 DeferredRegister 注册。

**Tech Stack:** Java 21、Minecraft 1.21.1、NeoForge 21.1.244、ModDevGradle 2.0.143、JUnit 5、Mojang Component/Codec、NeoForge DeferredRegister。

## Global Constraints

- 模组名保持 `Super Reforge`，mod ID 保持 `superreforge`，作者保持 `MUTUO`。
- Minecraft 固定为 `1.21.1`，NeoForge 最低版本固定为 `21.1.244`，Java 固定为 21。
- 不注册任何自定义 Attribute；提示框只展示原版或其他模组已经注册/同步的 Attribute ID。
- Curios 和 KubeJS 均保持可选依赖；不安装时核心 GUI、方块重铸与 datapack 功能必须运行。
- 服务端继续决定候选、成本和最终概率；客户端不得通过 GUI 重算或提交价格、概率和随机结果。
- 候选随机 Attribute 在重铸前只显示配置范围；实际值仍由 `词条 ID + 随机种子` 动态解析。
- 每个修改或新增的生产文件必须包含职责注释；坐标系、滚动边界和网络上限必须有解释性中文注释。
- 每项功能先写失败测试、确认失败，再写最小实现并运行回归测试。

## File Map

- `network/ReforgePreviewPayload.java`：在服务端报价中标注是否因网络安全上限而截断，概率本身仍由服务端生成。
- `client/ReforgerLogModel.java`：把嵌套等级/词条预览转换为完整有序行，并提供滚动边界与滑块换算的纯逻辑。
- `client/ModifierTooltipFormatter.java`：把同步词条定义格式化为 Attribute 详情 Component。
- `client/ReforgerScreen.java`：绘制精致双栏界面、裁剪日志、处理滚轮/拖动和悬停提示。
- `block/ReforgerMenu.java`：将两个机器槽和玩家背包槽移动到新界面坐标。
- `client/ReforgerRenderState.java`：输出静止、抬锤、接触、回弹和复位阶段的垂直位置与熔核亮度。
- `client/ReforgerHammerGeometry.java`：集中声明砧面、锤子缩放、模型边界、锚点和四向旋转。
- `client/ReforgerBlockEntityRenderer.java`：仅应用专用锤子几何与动画姿态，不再叠加手持式倾斜变换。
- `models/item/forge_hammer.json`：头部在下、锤柄在上的 3D 动力锻锤模型。
- `registry/ModCreativeTabs.java`：独立创造页签及固定内容顺序。
- `registry/ModRegistries.java`：把创造页签加入统一注册流程。
- `lang/en_us.json`、`lang/zh_cn.json`：GUI、Attribute 详情、滚动上限警告和创造页签翻译。
- `docs/FILE_REFERENCE.md`：逐文件说明新增/修改文件的用途与影响。

---

### Task 1: 服务端完整预览状态与日志纯模型

**Files:**
- Create: `src/main/java/com/mutuo/superreforge/client/ReforgerLogModel.java`
- Modify: `src/main/java/com/mutuo/superreforge/network/ReforgePreviewPayload.java`
- Test: `src/test/java/com/mutuo/superreforge/client/ReforgerLogModelTest.java`
- Test: `src/test/java/com/mutuo/superreforge/network/ReforgePreviewPayloadTest.java`

**Interfaces:**
- Consumes: `ReforgePreviewPayload.levels()`、`PreviewLevel`、`PreviewModifier`。
- Produces: `ReforgePreviewPayload.truncated()`；`ReforgerLogModel.from(ReforgePreviewPayload)`；`rows()`；`contentHeight()`；`clampScroll(int,int)`；`scrollBy(int,double,int)`；`thumbHeight(int,int)`；`scrollFromThumb(double,int,int,int)`。

- [ ] **Step 1: 写预览上限失败测试**

在 `ReforgePreviewPayloadTest` 创建 65 个有效 `CandidateLevel`，调用 `ReforgePreviewPayload.from(...)`，断言只同步 64 级且 `truncated()` 为 `true`；再创建单级 257 个词条并做相同断言。正常小预览断言 `truncated()` 为 `false`。网络 codec 往返后该标记必须保持。

```java
assertEquals(64, payload.levels().size());
assertTrue(payload.truncated());
assertEquals(payload, roundTrip(payload));
```

- [ ] **Step 2: 运行预览测试并确认失败**

Run: `./gradlew.bat test --tests '*ReforgePreviewPayloadTest' --console=plain`
Expected: FAIL，因为 `ReforgePreviewPayload` 还没有 `truncated` 字段。

- [ ] **Step 3: 最小实现网络标记**

在 record 的 `levels` 前加入 `boolean truncated`；`write`/`decode` 使用 `writeBoolean`/`readBoolean`。`from` 在原始等级数大于 64，或任一有效等级的归一化词条数大于 256 时设为 `true`。失败报价使用 `false`。

```java
boolean truncated = normalizedLevels.size() > MAX_LEVELS;
// 只要任一级别超过单级网络上限，就保留“预览已截断”事实。
truncated |= normalizedModifiers.size() > MAX_MODIFIERS_PER_LEVEL;
```

- [ ] **Step 4: 写日志模型失败测试**

用两级、每级三个词条构造预览，断言 `rows()` 依次包含 2 个 LEVEL 行和 6 个 MODIFIER 行，不因视口高度丢弃。以行高 `LEVEL_HEIGHT = 10`、`MODIFIER_HEIGHT = 9` 断言内容高度为 74；断言短内容滚动为 0、长内容钳制到 `contentHeight - viewportHeight`，滚轮和滑块换算均不会越界。

```java
ReforgerLogModel model = ReforgerLogModel.from(preview);
assertEquals(8, model.rows().size());
assertEquals(74, model.contentHeight());
assertEquals(34, model.clampScroll(999, 40));
assertEquals(0, model.clampScroll(-5, 40));
```

- [ ] **Step 5: 运行日志模型测试并确认失败**

Run: `./gradlew.bat test --tests '*ReforgerLogModelTest' --console=plain`
Expected: FAIL，因为 `ReforgerLogModel` 尚不存在。

- [ ] **Step 6: 实现日志模型并运行目标测试**

创建不可变 `ReforgerLogModel`，内部 `Row` 包含 `Kind kind`、`ResourceLocation id`、`Component name`、`double probability`、`int top` 和 `int height`。`from` 遍历所有 payload 行；滚轮每格移动 18 像素；滑块最小高度为 10 像素。

```java
public record Row(Kind kind, ResourceLocation id, Component name,
                  double probability, int top, int height) {}

public static ReforgerLogModel from(ReforgePreviewPayload preview) {
    List<Row> rows = new ArrayList<>();
    int top = 0;
    for (PreviewLevel level : preview.levels()) {
        rows.add(new Row(Kind.LEVEL, level.id(), level.name(), level.probability(), top, LEVEL_HEIGHT));
        top += LEVEL_HEIGHT;
        for (PreviewModifier modifier : level.modifiers()) {
            rows.add(new Row(Kind.MODIFIER, modifier.id(), modifier.name(), modifier.probability(), top, MODIFIER_HEIGHT));
            top += MODIFIER_HEIGHT;
        }
    }
    return new ReforgerLogModel(rows, top);
}
```

Run: `./gradlew.bat test --tests '*ReforgerLogModelTest' --tests '*ReforgePreviewPayloadTest' --console=plain`
Expected: PASS。

- [ ] **Step 7: 提交**

```powershell
git add src/main/java/com/mutuo/superreforge/client/ReforgerLogModel.java src/main/java/com/mutuo/superreforge/network/ReforgePreviewPayload.java src/test/java/com/mutuo/superreforge/client/ReforgerLogModelTest.java src/test/java/com/mutuo/superreforge/network/ReforgePreviewPayloadTest.java
git commit -m "feat: model complete scrollable reforge previews"
```

### Task 2: Attribute 悬停详情与“熔核锻造日志”界面

**Files:**
- Create: `src/main/java/com/mutuo/superreforge/client/ModifierTooltipFormatter.java`
- Modify: `src/main/java/com/mutuo/superreforge/client/ReforgerScreen.java`
- Modify: `src/main/java/com/mutuo/superreforge/block/ReforgerMenu.java`
- Modify: `src/main/resources/assets/superreforge/lang/en_us.json`
- Modify: `src/main/resources/assets/superreforge/lang/zh_cn.json`
- Test: `src/test/java/com/mutuo/superreforge/client/ModifierTooltipFormatterTest.java`
- Test: `src/test/java/com/mutuo/superreforge/client/ReforgerScreenContractTest.java`

**Interfaces:**
- Consumes: Task 1 的 `ReforgerLogModel`，以及 `DefinitionManager.clientModifiers()` 返回的 `Map<ResourceLocation, ModifierDisplayDefinition>`。
- Produces: `ModifierTooltipFormatter.format(ResourceLocation, ModifierDisplayDefinition): List<Component>`；宽 `286`、高 `218` 的容器界面和与之对齐的菜单槽位。

- [ ] **Step 1: 写提示格式失败测试**

测试一个词条包含固定 `ADD_VALUE` 主手效果和范围 `ADD_MULTIPLIED_BASE` 通用 Curios 效果。断言格式化文本包含词条 ID、Attribute ID、固定值、`2%–4%` 范围、加算/百分比运算名称及主手/任意 Curios 槽。传入 `null` 定义时必须返回“定义不可用”和原始 ID。

```java
List<Component> lines = ModifierTooltipFormatter.format(modifierId, definition);
String text = lines.stream().map(Component::getString).collect(Collectors.joining("\n"));
assertTrue(text.contains("superreforge:legendary"));
assertTrue(text.contains("minecraft:generic.attack_damage"));
assertTrue(text.contains("2%"));
assertTrue(text.contains("4%"));
```

- [ ] **Step 2: 运行提示测试并确认失败**

Run: `./gradlew.bat test --tests '*ModifierTooltipFormatterTest' --console=plain`
Expected: FAIL，因为格式化器尚不存在。

- [ ] **Step 3: 实现提示格式化器和翻译键**

固定 `ADD_VALUE` 使用原数值；`ADD_MULTIPLIED_BASE` 与 `ADD_MULTIPLIED_TOTAL` 乘以 100 后附加 `%`。范围显示最小值到最大值。每条 effect 独立输出 Attribute、数值、operation 和 slots；不访问客户端注册表，未知模组 Attribute 仍能显示资源 ID。

```java
public static List<Component> format(ResourceLocation id, ModifierDisplayDefinition definition) {
    List<Component> lines = new ArrayList<>();
    lines.add(definition == null ? Component.literal(id.toString()) : definition.name().copy());
    lines.add(Component.translatable("gui.superreforge.tooltip.modifier_id", id));
    if (definition == null) {
        lines.add(Component.translatable("gui.superreforge.tooltip.definition_unavailable"));
        return List.copyOf(lines);
    }
    definition.attributes().forEach(effect -> appendEffect(lines, effect));
    return List.copyOf(lines);
}
```

- [ ] **Step 4: 写屏幕契约失败测试**

`ReforgerScreenContractTest` 读取生产源码和翻译资源，断言 GUI 常量为 `286 × 218`，存在 `mouseScrolled`、`mouseDragged`、`enableScissor`、`ReforgerLogModel` 和 `ModifierTooltipFormatter` 接入；断言源码不再包含 `if (y > 93) break`。同时验证菜单槽位：目标 `(35,45)`、媒介 `(35,81)`、背包首槽 `(55,136)`、热栏首槽 `(55,194)`。

- [ ] **Step 5: 运行屏幕契约测试并确认失败**

Run: `./gradlew.bat test --tests '*ReforgerScreenContractTest' --console=plain`
Expected: FAIL，因为当前界面仍为 `256 × 198` 且直接截断日志。

- [ ] **Step 6: 实现新界面布局**

使用以下固定区域，所有坐标相对 `leftPos/topPos`：

```java
private static final int GUI_WIDTH = 286;
private static final int GUI_HEIGHT = 218;
private static final int LOG_LEFT = 154;
private static final int LOG_TOP = 36;
private static final int LOG_WIDTH = 116;
private static final int LOG_HEIGHT = 76;
private static final int SCROLLBAR_X = 273;
```

背景使用深色锻铁外框、铜色双线分隔、熔核橙色按钮焦点和紫灰日志底板。日志先 `enableScissor`，按 `row.top() - scrollOffset` 绘制，再 `disableScissor`。等级行为金橙色，词条行为浅灰紫色，概率右对齐；`truncated` 时在日志标题旁显示橙色警告符号。

- [ ] **Step 7: 实现滚轮、拖动和悬停**

`mouseScrolled` 只在日志或滚动条区域消费事件；`mouseClicked` 命中滑块后设置 `draggingScrollbar`；`mouseDragged` 调用 `scrollFromThumb`；`mouseReleased` 清除拖动状态。渲染时根据裁剪后的行矩形定位悬停词条，并在原版物品提示之后调用 `graphics.renderComponentTooltip(font, lines, mouseX, mouseY)`。

```java
@Override
public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
    if (!insideLog(mouseX, mouseY)) return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    scrollOffset = logModel.scrollBy(scrollOffset, deltaY, LOG_HEIGHT);
    return true;
}
```

- [ ] **Step 8: 对齐菜单和按钮**

机器槽改为 `(35,45)` 与 `(35,81)`；玩家背包改为 x 起点 55、y 起点 136，热栏 y 为 194；按钮保持左侧操作区并移至 `(66,91)`。`inventoryLabelY = 124`。按钮仍只在定义代次同步且非 pending 时启用。

```java
addSlot(new SlotItemHandler(handler, ReforgerBlockEntity.TARGET_SLOT, 35, 45));
addSlot(new SlotItemHandler(handler, ReforgerBlockEntity.CATALYST_SLOT, 35, 81));
// 玩家背包以 176 像素标准宽度居中放在 286 像素界面下方。
addSlot(new Slot(inventory, index, 55 + column * 18, 136 + row * 18));
```

- [ ] **Step 9: 运行目标测试和客户端编译**

Run: `./gradlew.bat test --tests '*ModifierTooltipFormatterTest' --tests '*ReforgerScreenContractTest' --console=plain`
Run: `./gradlew.bat compileJava --console=plain`
Expected: PASS。

- [ ] **Step 10: 提交**

```powershell
git add src/main/java/com/mutuo/superreforge/client/ModifierTooltipFormatter.java src/main/java/com/mutuo/superreforge/client/ReforgerScreen.java src/main/java/com/mutuo/superreforge/block/ReforgerMenu.java src/main/resources/assets/superreforge/lang/en_us.json src/main/resources/assets/superreforge/lang/zh_cn.json src/test/java/com/mutuo/superreforge/client/ModifierTooltipFormatterTest.java src/test/java/com/mutuo/superreforge/client/ReforgerScreenContractTest.java
git commit -m "feat: polish scrollable forge-log screen"
```

### Task 3: 竖直动力锻锤与无穿模动画

**Files:**
- Create: `src/main/java/com/mutuo/superreforge/client/ReforgerHammerGeometry.java`
- Modify: `src/main/java/com/mutuo/superreforge/client/ReforgerRenderState.java`
- Modify: `src/main/java/com/mutuo/superreforge/client/ReforgerBlockEntityRenderer.java`
- Modify: `src/main/resources/assets/superreforge/models/item/forge_hammer.json`
- Modify: `src/test/java/com/mutuo/superreforge/client/ReforgerRenderStateTest.java`
- Test: `src/test/java/com/mutuo/superreforge/client/ReforgerHammerGeometryTest.java`

**Interfaces:**
- Consumes: `ReforgerRenderState.hammerHeight()`，其语义明确为渲染模型中心的世界 Y 坐标。
- Produces: `ReforgerHammerGeometry.SCALE = 0.62F`；`headBottom(float originY)`；`yawDegrees(Direction)`；`ANVIL_TOP = 11.0F / 16.0F`；`CONTACT_ORIGIN_Y = 0.96F`；`REST_ORIGIN_Y = 1.03F`；`RAISED_ORIGIN_Y = 1.24F`。

- [ ] **Step 1: 写几何失败测试**

断言接触时锤头底面不低于砧面顶面且间隙小于 `1 / 16`；静止时比接触点高；缩放后的锤头横向范围位于中央砧面 `x=4/16..12/16` 内；四个方向 yaw 分别为北 0、东 90、南 180、西 270。

```java
assertTrue(ReforgerHammerGeometry.headBottom(CONTACT_ORIGIN_Y) >= ANVIL_TOP);
assertTrue(ReforgerHammerGeometry.headBottom(CONTACT_ORIGIN_Y) - ANVIL_TOP < 1.0F / 16.0F);
assertEquals(90.0F, ReforgerHammerGeometry.yawDegrees(Direction.EAST));
```

- [ ] **Step 2: 运行几何测试并确认失败**

Run: `./gradlew.bat test --tests '*ReforgerHammerGeometryTest' --console=plain`
Expected: FAIL，因为几何类尚不存在。

- [ ] **Step 3: 实现几何常量并重排 3D 模型**

将模型头部从 `y=11..15` 移到 `y=1..5`，锤柄改为 `y=4..16`，铜箍改为 `y=4..7`；保留锤头 x 范围 `2..14`、z 范围 `5..11`。`fixed` 显示变换旋转改为 `[0,0,0]`。按 Minecraft 模型以中心点渲染的规则，用模型头底 `1/16`、缩放 `0.62` 计算 `headBottom`。

```java
public static float headBottom(float originY) {
    float modelHeadBottomFromCenter = (1.0F / 16.0F) - 0.5F;
    return originY + modelHeadBottomFromCenter * SCALE;
}
```

```json
"fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]}
```

- [ ] **Step 4: 扩充动画失败测试**

测试 20 tick 的五个关键点：开始静止、5 tick 抬至最高、10 tick 接触、13 tick 回弹、20 tick 回到静止。断言接触时熔核亮度为 1，所有高度不低于接触点，最终不会从接触点瞬移到静止姿态；同时保留 `ReforgerBlockEntity` 在半程只触发一次熔岩火花和铁砧声音的现有回归断言。

```java
assertEquals(REST_ORIGIN_Y, stateAt(0).hammerHeight(), 0.001F);
assertEquals(RAISED_ORIGIN_Y, stateAt(5).hammerHeight(), 0.001F);
assertEquals(CONTACT_ORIGIN_Y, stateAt(10).hammerHeight(), 0.001F);
assertEquals(REST_ORIGIN_Y, stateAt(20).hammerHeight(), 0.001F);
```

- [ ] **Step 5: 运行动画测试并确认失败**

Run: `./gradlew.bat test --tests '*ReforgerRenderStateTest' --console=plain`
Expected: FAIL，因为现有动画使用 `1.15/0.38/0.92` 高度且结束时没有复位。

- [ ] **Step 6: 实现分段动画和渲染变换**

`ReforgerRenderState.fromTicks` 按进度 `[0,.25]` 从静止上升至 raised，`[.25,.5]` 二次缓入落至 contact，`[.5,.65]` 回弹至 `1.06F`，`[.65,1]` 平滑回到 rest。渲染器删除 X 轴 90°和 JSON -45°复合旋转，只保留方块 Y 朝向、`0.62` 等比缩放和几何中心 Y。

```java
poseStack.translate(0.5D, state.hammerHeight(), 0.5D);
poseStack.mulPose(Axis.YP.rotationDegrees(
        ReforgerHammerGeometry.yawDegrees(blockState.getValue(ReforgerBlock.FACING))));
poseStack.scale(ReforgerHammerGeometry.SCALE,
        ReforgerHammerGeometry.SCALE, ReforgerHammerGeometry.SCALE);
// 不再施加 X 轴 90°：锤子模型本身已经采用锤头朝下的工作姿态。
```

- [ ] **Step 7: 运行锤子测试和编译**

Run: `./gradlew.bat test --tests '*ReforgerHammerGeometryTest' --tests '*ReforgerRenderStateTest' --console=plain`
Run: `./gradlew.bat compileJava processResources --console=plain`
Expected: PASS，模型 JSON 可处理且 Java 编译成功。

- [ ] **Step 8: 游戏内四向视觉检查**

在开发客户端从正面、侧面和俯视观察朝北、东、南、西的重铸台；分别截取静止、接触和回弹画面。验收：锤头不进入中央砧面、后梁和立柱，锤柄竖直朝上，落点位于熔核/砧面中央。若烘焙模型实际枢轴与公式存在不超过 1 像素的偏差，只调整 `CONTACT_ORIGIN_Y`、`REST_ORIGIN_Y` 或 z 锚点，并同步更新测试常量。

- [ ] **Step 9: 提交**

```powershell
git add src/main/java/com/mutuo/superreforge/client/ReforgerHammerGeometry.java src/main/java/com/mutuo/superreforge/client/ReforgerRenderState.java src/main/java/com/mutuo/superreforge/client/ReforgerBlockEntityRenderer.java src/main/resources/assets/superreforge/models/item/forge_hammer.json src/test/java/com/mutuo/superreforge/client/ReforgerHammerGeometryTest.java src/test/java/com/mutuo/superreforge/client/ReforgerRenderStateTest.java
git commit -m "fix: align vertical reforger hammer animation"
```

### Task 4: 独立 Super Reforge 创造页签

**Files:**
- Create: `src/main/java/com/mutuo/superreforge/registry/ModCreativeTabs.java`
- Modify: `src/main/java/com/mutuo/superreforge/registry/ModRegistries.java`
- Modify: `src/main/resources/assets/superreforge/lang/en_us.json`
- Modify: `src/main/resources/assets/superreforge/lang/zh_cn.json`
- Test: `src/test/java/com/mutuo/superreforge/registry/ModCreativeTabsTest.java`

**Interfaces:**
- Consumes: `ModItems.REFORGER`、`COMMON_REFORGE_STONE`、`REFINED_REFORGE_STONE`、`SUPREME_REFORGE_STONE`。
- Produces: `ModCreativeTabs.SUPER_REFORGE`；`visibleItemIds(): List<ResourceLocation>`，顺序可被测试并被页签 displayItems 复用。

- [ ] **Step 1: 写页签失败测试**

断言 `visibleItemIds()` 精确等于重铸台、普通、精炼、终极媒介四个 ID，顺序固定；断言列表不包含 `superreforge:forge_hammer`，翻译资源包含 `itemGroup.superreforge`。

```java
assertEquals(List.of(
        id("reforger"), id("common_reforge_stone"),
        id("refined_reforge_stone"), id("supreme_reforge_stone")),
        ModCreativeTabs.visibleItemIds());
assertFalse(ModCreativeTabs.visibleItemIds().contains(id("forge_hammer")));
```

- [ ] **Step 2: 运行页签测试并确认失败**

Run: `./gradlew.bat test --tests '*ModCreativeTabsTest' --console=plain`
Expected: FAIL，因为独立页签注册类尚不存在。

- [ ] **Step 3: 实现页签注册**

使用 `DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SuperReforge.MOD_ID)` 注册 `super_reforge`。图标为重铸台，标题为 `Component.translatable("itemGroup.superreforge")`。`displayItems` 按 `visibleItemIds` 对应的四个 holder 顺序调用 `output.accept`；动画锤子不加入。

```java
public static final DeferredHolder<CreativeModeTab, CreativeModeTab> SUPER_REFORGE =
        TABS.register("super_reforge", () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.superreforge"))
                .icon(() -> ModItems.REFORGER.get().getDefaultInstance())
                .displayItems((parameters, output) -> visibleItems().forEach(output::accept))
                .build());
```

- [ ] **Step 4: 接入统一注册与翻译**

在 `ModRegistries.register` 中于物品注册之后调用 `ModCreativeTabs.register(bus)`。中文标题为“Super Reforge”，英文标题为“Super Reforge”；不把同一物品额外注入其他原版页签。

```java
ModBlocks.register(bus);
ModItems.register(bus);
ModCreativeTabs.register(bus);
ModBlockEntities.register(bus);
ModMenus.register(bus);
```

- [ ] **Step 5: 运行测试和编译**

Run: `./gradlew.bat test --tests '*ModCreativeTabsTest' --console=plain`
Run: `./gradlew.bat compileJava --console=plain`
Expected: PASS。

- [ ] **Step 6: 提交**

```powershell
git add src/main/java/com/mutuo/superreforge/registry/ModCreativeTabs.java src/main/java/com/mutuo/superreforge/registry/ModRegistries.java src/main/resources/assets/superreforge/lang/en_us.json src/main/resources/assets/superreforge/lang/zh_cn.json src/test/java/com/mutuo/superreforge/registry/ModCreativeTabsTest.java
git commit -m "feat: add Super Reforge creative tab"
```

### Task 5: 注释、文件说明、兼容矩阵与发布 JAR

**Files:**
- Modify: `docs/FILE_REFERENCE.md`
- Modify: all Java/JSON files touched in Tasks 1-4 where comments are missing or garbled
- Test: `src/test/java/com/mutuo/superreforge/DocumentationContractTest.java`

**Interfaces:**
- Consumes: Tasks 1-4 的最终文件集合。
- Produces: 注释完整的源代码、更新后的逐文件说明和 `build/libs/superreforge-0.1.0.jar`。

- [ ] **Step 1: 扩充文档契约失败测试**

断言 `docs/FILE_REFERENCE.md` 包含 `ReforgerLogModel.java`、`ModifierTooltipFormatter.java`、`ReforgerHammerGeometry.java`、`ModCreativeTabs.java`，并包含“影响”说明；扫描本次涉及 Java/JSON 文件，拒绝乱码替换符和常见未完成标记。

- [ ] **Step 2: 运行文档测试并确认失败**

Run: `./gradlew.bat test --tests '*DocumentationContractTest' --console=plain`
Expected: FAIL，因为新文件尚未加入文件参考文档。

- [ ] **Step 3: 更新注释与逐文件说明**

对每个新增类写类级职责注释；对 payload 上限、滚动坐标换算、Minecraft 模型中心坐标和创造页签隐藏锤子的原因写行内注释。`FILE_REFERENCE.md` 对每个新文件分别写“用途”和“修改会影响 GUI/动画/创造栏中的哪一部分”。

```markdown
- `client/ReforgerLogModel.java`：用途是把服务端嵌套预览变成完整日志行并计算滚动边界；修改它会影响日志顺序、行高、滚轮距离和滚动条比例，不影响服务端抽取概率。
```

- [ ] **Step 4: 运行完整干净测试和构建**

Run: `./gradlew.bat clean test build --console=plain`
Expected: `BUILD SUCCESSFUL`，JUnit 失败数为 0，生成 `build/libs/superreforge-0.1.0.jar`。

- [ ] **Step 5: 验证四种可选依赖服务端配置**

依次运行到日志出现服务器加载完成 `Done`，随后正常停止：

```powershell
./gradlew.bat runServer --console=plain
./gradlew.bat runServer -PwithCuriosRuntime=true --console=plain
./gradlew.bat runServer -PwithKubeJSRuntime=true --console=plain
./gradlew.bat runServer -PwithCuriosRuntime=true -PwithKubeJSRuntime=true --console=plain
```

Expected: 四种配置均无缺类、注册冲突、数据包解码或启动崩溃。

- [ ] **Step 6: 检查发布物**

使用 `jar tf build/libs/superreforge-0.1.0.jar` 确认包含创造页签类、GUI/锤子类、两种语言和锤子模型；确认没有 Curios/KubeJS 类被打包为依赖，也没有 `attributes/` 自定义注册资源。运行：

```powershell
Get-FileHash build/libs/superreforge-0.1.0.jar -Algorithm SHA256
Get-Item build/libs/superreforge-0.1.0.jar | Select-Object FullName,Length,LastWriteTime
```

- [ ] **Step 7: 提交发布收尾**

```powershell
git add docs/FILE_REFERENCE.md src/main src/test
git commit -m "docs: document reforger interface polish"
```

## Self-Review Results

- **规格覆盖：** Task 1 覆盖完整概率与网络截断告知；Task 2 覆盖精致 GUI、滚轮、滚动条和 Attribute 悬停；Task 3 覆盖穿模根因、竖直姿态、约一秒动画和四向检查；Task 4 覆盖独立页签及隐藏动画锤；Task 5 覆盖注释、文件说明、兼容矩阵和 JAR。
- **占位符扫描：** 每个实现步骤均给出具体 API、坐标、常量、命令和预期结果，不含未完成步骤或未指定测试。
- **类型一致性：** `truncated()` 由 payload 产生并由 screen 消费；`ReforgerLogModel.Row` 同时供绘制和悬停命中；`hammerHeight()` 的模型中心语义与几何类、动画类和 renderer 一致；创造页签显示顺序只有一个来源。
- **风险检查：** GUI 不重算服务端候选；未知模组 Attribute 只显示 ID；锤子视觉偏差被限制为只校正集中常量；Curios/KubeJS 仍是 compile-only/可选运行依赖。
