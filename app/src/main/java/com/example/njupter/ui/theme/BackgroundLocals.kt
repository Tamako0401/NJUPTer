package com.example.njupter.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * 自定义背景是否已启用（背景图存在）。
 * 各屏 Scaffold 据此把容器色换成透明，让主题层里的背景图透出来；
 * 未启用时保持默认 surface，不改变无背景时的观感。
 */
val LocalHasCustomBackground = staticCompositionLocalOf { false }
