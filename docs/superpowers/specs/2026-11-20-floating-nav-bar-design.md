# 悬浮底栏 — 短设计

## 动机
三 tab 底栏（80dp 栏高）压住课程表第 12 节。悬浮胶囊占位远小于整栏，课表最后一节完整可见。

## 决策
- 双模式：经典（现 Scaffold+NavigationBar 原样保留）+ 悬浮（默认启用，设置可关）。
- 悬浮实现参照 KernelSU 的结构（Scaffold bottomBar 槽内放胶囊，miuix blur/lens 特效不引入）：
  M3 `Surface(shape=CircleShape 胶囊, color=surfaceContainer, shadowElevation)` 内嵌 3 个原生 `NavigationBarItem`（透明容器色），`padding(horizontal=28dp, bottom= 8dp+手势条 或 28dp)`。
- 悬浮时内容让出胶囊实际占位（`floatingNavBarBottomOccupancy()`：64dp 胶囊 + 8dp+手势条 或 28dp 呼吸），比经典整栏矮、且公式与胶囊组件单源锁定，第 12 节完整可见；胶囊浮于内容之上（KernelSU 式效果）。
- 开关位置：应用主题设置页（ThemeSettingsScreen）应用设置区 Toggle，en/zh 双语。
- 键 `floating_nav_bar`，默认 `true`。

## 非目标
- miuix blur/折射/重力光效、拖拽切换、SideRail 分栏——不引入。
- 不改 tab 数量与路由。
