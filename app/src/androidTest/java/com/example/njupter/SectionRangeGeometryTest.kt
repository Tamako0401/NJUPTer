package com.example.njupter

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.RangeSlider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.example.njupter.ui.theme.NJUPTerTheme
import com.example.njupter.ui.timetable.dialog.SectionRangeTrack
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalMaterial3Api::class)
class SectionRangeGeometryTest {
    @get:Rule val compose = createComposeRule()

    @Test fun endpointsAndThumbGapsMatchTheDefaultTrack() {
        val range = mutableStateOf(1f..5f)
        compose.setContent {
            NJUPTerTheme {
                Column {
                    RangeSlider(range.value, {}, Modifier.width(300.dp).testTag("default"),
                        valueRange = 1f..12f, steps = 10)
                    RangeSlider(range.value, {}, Modifier.width(300.dp).testTag("custom"),
                        valueRange = 1f..12f, steps = 10,
                        track = { SectionRangeTrack(it, emptySet()) })
                }
            }
        }
        for (selection in listOf(1f..5f, 10f..12f, 1f..12f)) {
            compose.runOnIdle { range.value = selection }
            val expected = compose.onNodeWithTag("default").captureToImage().asAndroidBitmap()
            val actual = compose.onNodeWithTag("custom").captureToImage().asAndroidBitmap()
            assertTrue("Track geometry differs at $selection", expected.sameAs(actual))
        }
    }
}
