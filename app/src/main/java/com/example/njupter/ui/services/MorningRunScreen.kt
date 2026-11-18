package com.example.njupter.ui.services

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.njupter.R
import com.example.njupter.ui.settings.component.SettingsSectionHeader
import com.example.njupter.viewmodels.MorningRunUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 晨跑次数查询页。绑定 token 后展示本学期次数、今日打卡、学期统计与最近打卡记录。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MorningRunScreen(
    uiState: MorningRunUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onBind: (String) -> Boolean,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showBindSheet by remember { mutableStateOf(false) }

    if (showBindSheet) {
        MorningRunTokenSheet(
            onDismiss = { showBindSheet = false },
            onBind = onBind
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.morning_run)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                actions = {
                    if (uiState is MorningRunUiState.Ready) {
                        TextButton(onClick = onClear) {
                            Text(stringResource(R.string.morning_run_unbind))
                        }
                    }
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Top + WindowInsetsSides.Horizontal
        )
    ) { padding ->
        when (val state = uiState) {
            is MorningRunUiState.Unbound -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsRun,
                        contentDescription = null,
                        modifier = Modifier.size(120.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = stringResource(R.string.morning_run_no_token_title),
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.morning_run_no_token_desc),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(32.dp))
                    Button(
                        onClick = { showBindSheet = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.morning_run_bind))
                    }
                }
            }
            is MorningRunUiState.Loading -> {
                BoxInside(padding) {
                    CircularProgressIndicator()
                }
            }
            is MorningRunUiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.error
                    )
                    if (state.tokenExpired) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.morning_run_expired),
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = onRefresh) {
                        Text(stringResource(R.string.retry))
                    }
                    if (state.tokenExpired) {
                        TextButton(onClick = { showBindSheet = true }) {
                            Text(stringResource(R.string.morning_run_rebind))
                        }
                    }
                }
            }
            is MorningRunUiState.Ready -> {
                PullToRefreshBox(
                    isRefreshing = false,
                    onRefresh = onRefresh,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    ReadyContent(state)
                }
            }
        }
    }
}

@Composable
private fun BoxInside(padding: PaddingValues, content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.align(Alignment.Center)
        ) {
            content()
        }
    }
}

@Composable
private fun ReadyContent(state: MorningRunUiState.Ready) {
    val snapshot = state.snapshot
    val expiresAt = SimpleDateFormat("HH:mm", Locale.getDefault())
        .format(Date(state.token.expiresAtEpochSeconds * 1000))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 身份卡
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors()
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    text = state.token.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = state.token.studentId,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.morning_run_expires, expiresAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 次数卡
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = snapshot.count.toString(),
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.morning_run_count_label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                val today = snapshot.today
                Text(
                    text = if (today != null) {
                        "%1\$s · %2\$s".format(today.time, today.location)
                    } else {
                        stringResource(R.string.morning_run_no_checkin)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 学期统计
        if (snapshot.history.isNotEmpty()) {
            Column {
                SettingsSectionHeader(
                    title = stringResource(R.string.morning_run_history),
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 6.dp)
                )
                snapshot.history.forEach { semester ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "%1\$s %2\$s".format(semester.year, semester.term),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = stringResource(
                                R.string.morning_run_semester_value,
                                semester.count,
                                semester.differ
                            ),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 最近打卡
        if (snapshot.records.isNotEmpty()) {
            Column {
                SettingsSectionHeader(
                    title = stringResource(R.string.morning_run_records),
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 6.dp)
                )
                snapshot.records.forEach { record ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = record.date,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "%1\$s - %2\$s".format(record.start, record.end),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
