# 设计参考与资产边界

Super Reforge 由 MUTUO 制作，代码采用 `LGPL-3.0-or-later`。

## 参考项目

- [MCTeamPotato/Remodifier](https://github.com/MCTeamPotato/Remodifier)：参考“物品获得可重铸 modifier”的玩法方向。
- [CursedFlames/Modifiers](https://github.com/CursedFlames/Modifiers)：参考数据化 modifier 与属性组合的设计思路。
- [CursedFlames/BountifulBaubles](https://github.com/CursedFlames/BountifulBaubles/)：参考独立重铸设施、铁砧/工作台/熔岩的视觉语言。

这些仓库仅作为玩法和视觉语汇研究对象。Super Reforge 没有复制它们的 Java 源码、模型 JSON、纹理像素或其他二进制资产。

## 本项目资产

- 熔核锻台的 22 部件几何布局和 3D 锻锤模型为本项目原创组合。
- 当前模型引用 Minecraft 原版的深板岩砖、磨制黑石砖、切制铜、铁块和岩浆块材质；JAR 不重新分发这些纹理文件。
- 普通、精炼、至高媒介的物品模型引用原版燧石、紫水晶碎片和回响碎片纹理。
- 内置图像生成服务在制作阶段不可用，因此没有把任何 AI 生成位图放入发布 JAR；这不会影响模型或游戏功能。

Minecraft 及其原版资产属于 Mojang Studios/Microsoft。本项目不隶属于或受其认可。
