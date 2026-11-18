package com.example.njupter.ui.services

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.njupter.R
import com.example.njupter.ui.settings.component.SettingsSectionCard
import com.example.njupter.ui.settings.model.SettingsIcon
import com.example.njupter.ui.settings.model.SettingsItem
import com.example.njupter.ui.settings.model.SettingsSection

/**
 * 服务页：查询类功能的入口卡片列表。后续查询功能在 [queryItems] 追加条目即可。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServicesScreen(
    boundName: String?,
    count: Int?,
    onMorningRunClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val morningRunSummary = when {
        boundName == null -> stringResource(R.string.morning_run_unbound)
        count != null -> stringResource(R.string.morning_run_count_value, count)
        else -> boundName
    }
    val queryItems = listOf(
        SettingsItem.Navigation(
            icon = SettingsIcon.Vector(Icons.Default.DirectionsRun),
            title = stringResource(R.string.morning_run),
            description = stringResource(R.string.morning_run_summary),
            value = morningRunSummary,
            emphasized = true,
            onClick = onMorningRunClick
        )
    )

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.services)) },
                colors = TopAppBarDefaults.topAppBarColors()
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Top + WindowInsetsSides.Horizontal
        )
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 24.dp
            )
        ) {
            item {
                SettingsSectionCard(
                    section = SettingsSection(
                        title = stringResource(R.string.services),
                        items = queryItems
                    )
                )
            }
        }
    }
}
