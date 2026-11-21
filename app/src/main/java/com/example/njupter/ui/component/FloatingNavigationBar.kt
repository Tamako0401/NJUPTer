package com.example.njupter.ui.component

import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemColors
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import com.example.njupter.R

/**
 * 悬浮胶囊在屏幕底部占用的总高度（胶囊 64dp + 底部留白）。
 * 宿主内容让出这个高度，保证最后一个时段完整可见。
 * 有手势条时贴 inset 上方 8dp，无手势条时留 28dp 视觉呼吸。
 */
fun floatingNavBarBottomOccupancy(navInset: Dp): Dp {
    val breathing = if (navInset > 0.dp) 8.dp else 28.dp
    return 64.dp + breathing + navInset
}

/**
 * 悬浮底栏：结构参照 KernelSU 的 FloatingBottomBar（胶囊浮于内容之上），
 * 材质用 M3 原生组件——surfaceContainer 胶囊 + 透明容器色的 NavigationBarItem。
 * 宿主内容让出 [floatingNavBarBottomOccupancy] 的高度，最后一个时段不被胶囊压住。
 */
@Composable
fun FloatingNavigationBar(
    currentTab: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val navBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomPadding = floatingNavBarBottomOccupancy(navBarInset) - 64.dp

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 28.dp, end = 28.dp, bottom = bottomPadding),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        shadowElevation = 6.dp,
        tonalElevation = 0.dp
    ) {
        NavigationBar(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
            windowInsets = WindowInsets(0, 0, 0, 0),
            modifier = Modifier.height(64.dp)
        ) {
            NavigationBarItem(
                selected = currentTab == 0,
                onClick = { onSelect(0) },
                icon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                label = { Text(stringResource(R.string.timetable)) },
                colors = floatingItemColors()
            )
            NavigationBarItem(
                selected = currentTab == 1,
                onClick = { onSelect(1) },
                icon = { Icon(Icons.Default.Apps, contentDescription = null) },
                label = { Text(stringResource(R.string.services)) },
                colors = floatingItemColors()
            )
            NavigationBarItem(
                selected = currentTab == 2,
                onClick = { onSelect(2) },
                icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                label = { Text(stringResource(R.string.settings)) },
                colors = floatingItemColors()
            )
        }
    }
}

@Composable
private fun floatingItemColors(): NavigationBarItemColors {
    return NavigationBarItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
        selectedTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
        indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
