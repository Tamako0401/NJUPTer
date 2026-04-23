package com.example.njupter.ui.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.njupter.R
import com.example.njupter.ui.settings.component.SettingsSectionHeader
import com.example.njupter.ui.settings.dialog.LanguageSelectDialog
import com.example.njupter.ui.timetable.dialog.TimetableConfigDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentTimetableId: String?,
    currentTimetableName: String,
    currentStartDate: Long,
    currentTotalWeeks: Int,
    currentSessionTimes: List<String>,
    currentShowWeekends: Boolean,
    currentLanguageTag: String,
    onLanguageChange: (String) -> Unit,
    onUpdateTimetableMetadata: (String, String, Long, Int, Boolean, List<String>) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var showConfigDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var notificationEnabled by remember { mutableStateOf(isNotificationPermissionGranted(context)) }
    var batteryWhitelistEnabled by remember { mutableStateOf(isIgnoringBatteryOptimizations(context)) }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationEnabled = isNotificationPermissionGranted(context)
                batteryWhitelistEnabled = isIgnoringBatteryOptimizations(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (showConfigDialog) {
        TimetableConfigDialog(
            initialName = currentTimetableName,
            initialStartDate = currentStartDate,
            initialTotalWeeks = currentTotalWeeks,
            initialShowWeekends = currentShowWeekends,
            initialSessionTimes = currentSessionTimes,
            isEditMode = true,
            onDismiss = { showConfigDialog = false },
            onConfirm = { name, startDate, weeks, showWeekends, sessionTimes ->
                if (currentTimetableId != null) {
                    onUpdateTimetableMetadata(
                        currentTimetableId,
                        name,
                        startDate,
                        weeks,
                        showWeekends,
                        sessionTimes
                    )
                }
                showConfigDialog = false
            }
        )
    }

    if (showLanguageDialog) {
        LanguageSelectDialog(
            currentLanguageTag = currentLanguageTag,
            onDismiss = { showLanguageDialog = false },
            onSelectLanguage = { languageTag ->
                onLanguageChange(languageTag)
                showLanguageDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            item {
                SettingsSectionHeader(title = stringResource(R.string.current_timetable_settings))
            }
            
            // If no timetable is selected/created, disable or hide specific settings
            if (currentTimetableId == null) {
                 item {
                     Text(
                         text = stringResource(R.string.no_timetable_desc),
                         modifier = Modifier.padding(16.dp),
                         style = MaterialTheme.typography.bodyMedium,
                         color = MaterialTheme.colorScheme.error
                     )
                 }
            } else {

                item {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.cur_timetable_settings)) },
                        supportingContent = { Text(currentTimetableName) },
                        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
                        modifier = Modifier.clickable { showConfigDialog = true }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }

            item {
                SettingsSectionHeader(title = stringResource(R.string.app_settings))
            }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.language)) },
                    supportingContent = {
                        Text(
                            if (currentLanguageTag.startsWith("zh")) "简体中文"
                            else "English"
                        )
                    },
                    trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
                    modifier = Modifier.clickable { showLanguageDialog = true }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.notification_permission)) },
                    supportingContent = { Text(stringResource(R.string.notification_permission_desc)) },
                    trailingContent = {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SuggestionChip(
                                onClick = { openNotificationSettings(context) },
                                label = {
                                    Text(
                                        if (notificationEnabled) stringResource(R.string.status_enabled)
                                        else stringResource(R.string.go_to_settings)
                                    )
                                }
                            )
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                        }
                    },
                    modifier = Modifier.clickable { openNotificationSettings(context) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.battery_optimization)) },
                    supportingContent = { Text(stringResource(R.string.battery_optimization_desc)) },
                    trailingContent = {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SuggestionChip(
                                onClick = { openBatteryOptimizationSettings(context) },
                                label = {
                                    Text(
                                        if (batteryWhitelistEnabled) stringResource(R.string.status_enabled)
                                        else stringResource(R.string.go_to_settings)
                                    )
                                }
                            )
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                        }
                    },
                    modifier = Modifier.clickable { openBatteryOptimizationSettings(context) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}

private fun isNotificationPermissionGranted(context: android.content.Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
}

private fun isIgnoringBatteryOptimizations(context: android.content.Context): Boolean {
    val powerManager = context.getSystemService(android.content.Context.POWER_SERVICE) as PowerManager
    return powerManager.isIgnoringBatteryOptimizations(context.packageName)
}

private fun openNotificationSettings(context: android.content.Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    }
    val fallbackIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.parse("package:${context.packageName}")
    }
    startActivitySafely(context, intent, fallbackIntent)
}

private fun openBatteryOptimizationSettings(context: android.content.Context) {
    val requestIntent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
        data = Uri.parse("package:${context.packageName}")
    }
    val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
    val finalFallbackIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.parse("package:${context.packageName}")
    }

    if (!startActivitySafely(context, requestIntent, fallbackIntent)) {
        startActivitySafely(context, finalFallbackIntent)
    }
}

private fun startActivitySafely(
    context: android.content.Context,
    primaryIntent: Intent,
    secondaryIntent: Intent? = null
): Boolean {
    return try {
        val launchIntent = when {
            primaryIntent.resolveActivity(context.packageManager) != null -> primaryIntent
            secondaryIntent != null && secondaryIntent.resolveActivity(context.packageManager) != null -> secondaryIntent
            else -> return false
        }
        context.startActivity(launchIntent)
        true
    } catch (_: Exception) {
        false
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    MaterialTheme {
        SettingsScreen(
            currentTimetableId = "1",
            currentTimetableName = "2026 春季学期",
            currentStartDate = System.currentTimeMillis(),
            currentTotalWeeks = 20,
            currentShowWeekends = false,
            currentSessionTimes = listOf("08:00-08:45"),
            currentLanguageTag = "zh",
            onLanguageChange = {},
            onUpdateTimetableMetadata = { _, _, _, _, _, _ -> },
        )
    }
}
