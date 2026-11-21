package com.example.njupter.ui.component

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 悬浮胶囊占位公式的契约测试：MainActivity 内容让位高度必须与
 * FloatingNavigationBar 胶囊实际占位一致，否则最后一节会被压住
 * （本功能要解决的核心 bug）。
 */
class FloatingNavigationBarTest {

    @Test
    fun `无手势条时占位为胶囊加呼吸留白`() {
        assertEquals(92.dp, floatingNavBarBottomOccupancy(0.dp))
    }

    @Test
    fun `有手势条时占位为胶囊加8dp加inset`() {
        assertEquals(96.dp, floatingNavBarBottomOccupancy(24.dp))
        assertEquals(104.dp, floatingNavBarBottomOccupancy(32.dp))
    }
}
