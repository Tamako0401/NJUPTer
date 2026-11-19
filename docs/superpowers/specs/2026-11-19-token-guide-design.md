# 晨跑 token 获取指南弹窗 — 短设计

## 背景
晨跑绑定需要粘贴约 2 小时时效的 JWT，只有从微信小程序运行时抓包才能取得。当前绑定弹窗只有一句 placeholder，用户不知道 token 从哪来。

## 决策（会话中已确认）
- 位置：`MorningRunTokenSheet` 内加「如何获取 token？」文字入口 → 可滚动 AlertDialog 全文；不做 toast。
- 文案：不提 root；写「任意 HTTPS 抓包工具」，推荐 Reqable；中英双语。
- 指南步骤：小程序入口（企业微信工作台/微信）→ 抓包过滤 wechat.njupt.edu.cn → 找 login/bind 或 login/refreshUserInfo 响应 → 复制 data.token（整段 JSON 也可）→ 有效期 ~2h。

## 实现
- `MorningRunTokenSheet.kt`：标题行右侧 `TextButton`（`morning_run_how_to_get`）→ `showGuide` 状态 → `AlertDialog`（scrollable Column，`morning_run_guide_*` 文案，确定关闭）。
- strings en/zh：`morning_run_how_to_get` + `morning_run_guide_title` + 6 条步骤 + `morning_run_guide_steps` 占位符拼装（`%1$s` 逐条列出）。

## 非目标
- 不做 deeplink/抓包自动化（本会话已判死/搁置）。
