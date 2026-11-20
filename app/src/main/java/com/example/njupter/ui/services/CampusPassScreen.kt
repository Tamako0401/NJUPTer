package com.example.njupter.ui.services

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NightShelter
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import com.example.njupter.viewmodels.CampusPassUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 校园通行页：绑定 0trust cdr token（约 3h 有效）后展示在校/在寝状态、
 * 最后出行记录与宿舍位置。门禁权限不在本页范围。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampusPassScreen(
    uiState: CampusPassUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onBind: (String) -> Boolean,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showBindSheet by remember { mutableStateOf(false) }

    if (showBindSheet) {
        CampusPassTokenSheet(
            onDismiss = { showBindSheet = false },
            onBind = onBind
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.campus_pass)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                actions = {
                    if (uiState is CampusPassUiState.Ready) {
                        TextButton(onClick = onClear) {
                            Text(stringResource(R.string.campus_pass_unbind))
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
            is CampusPassUiState.Unbound -> {
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
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(120.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = stringResource(R.string.campus_pass_no_token_title),
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.campus_pass_no_token_desc),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(32.dp))
                    Button(
                        onClick = { showBindSheet = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.campus_pass_bind))
                    }
                }
            }
            is CampusPassUiState.Loading -> {
                PullToRefreshBox(
                    isRefreshing = true,
                    onRefresh = onRefresh,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    CircularProgressIndicator()
                }
            }
            is CampusPassUiState.Error -> {
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
                        text = if (state.tokenExpired) {
                            stringResource(R.string.campus_pass_expired)
                        } else {
                            state.message
                        },
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = onRefresh) {
                        Text(stringResource(R.string.retry))
                    }
                    if (state.tokenExpired) {
                        TextButton(onClick = { showBindSheet = true }) {
                            Text(stringResource(R.string.campus_pass_rebind))
                        }
                    }
                }
            }
            is CampusPassUiState.Ready -> {
                PullToRefreshBox(
                    isRefreshing = uiState is CampusPassUiState.Loading,
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
private fun ReadyContent(state: CampusPassUiState.Ready) {
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
        // 状态卡
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatusRow(
                    icon = { Icon(Icons.Default.School, contentDescription = null) },
                    label = when (snapshot.onCampus) {
                        true -> stringResource(R.string.campus_pass_on_campus)
                        false -> stringResource(R.string.campus_pass_off_campus)
                        null -> stringResource(R.string.campus_pass_unknown)
                    },
                    positive = snapshot.onCampus == true
                )
                StatusRow(
                    icon = { Icon(Icons.Default.NightShelter, contentDescription = null) },
                    label = when (snapshot.inDorm) {
                        true -> stringResource(R.string.campus_pass_in_dorm)
                        false -> stringResource(R.string.campus_pass_not_in_dorm)
                        null -> stringResource(R.string.campus_pass_not_returned)
                    },
                    positive = snapshot.inDorm == true
                )
                Text(
                    text = stringResource(R.string.campus_pass_expires, expiresAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 最后出行记录
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                SettingsSectionHeader(
                    title = stringResource(R.string.campus_pass_last_access),
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Text(
                    text = if (snapshot.lastAccessTime.isNotEmpty()) {
                        "%1\$s · %2\$s".format(snapshot.lastAccessTime, snapshot.lastAccessPlace)
                    } else {
                        stringResource(R.string.campus_pass_not_returned)
                    },
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        // 宿舍位置（全空隐藏）
        if (snapshot.dormBuilding.isNotEmpty() || snapshot.dormRoom.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    SettingsSectionHeader(
                        title = stringResource(R.string.campus_pass_dorm_location),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Text(
                        text = "%1\$s %2\$s".format(snapshot.dormBuilding, snapshot.dormRoom),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusRow(
    icon: @Composable () -> Unit,
    label: String,
    positive: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            icon()
            Spacer(Modifier.width(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleLarge,
                color = if (positive) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = if (positive) "✓" else "—",
            style = MaterialTheme.typography.titleLarge,
            color = if (positive) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
