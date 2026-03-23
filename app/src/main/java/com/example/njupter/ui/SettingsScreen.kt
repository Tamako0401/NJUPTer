package com.example.njupter.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.njupter.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentTimetableId: String?,
    currentTimetableName: String,
    currentStartDate: Long,
    currentTotalWeeks: Int,
    showWeekends: Boolean,
    onUpdateTimetableMetadata: (String, String, Long, Int) -> Unit,
    onToggleShowWeekends: (Boolean) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showWeeksDialog by remember { mutableStateOf(false) }
    var showNameDialog by remember { mutableStateOf(false) }

    if (showDatePicker) {
        StartDateDialog(
            initialDateMillis = currentStartDate,
            onDismiss = { showDatePicker = false },
            onConfirm = {
                if (currentTimetableId != null) {
                    onUpdateTimetableMetadata(currentTimetableId, currentTimetableName, it, currentTotalWeeks)
                }
                showDatePicker = false
            }
        )
    }

    if (showWeeksDialog) {
        TotalWeeksDialog(
            initialWeeks = currentTotalWeeks,
            onDismiss = { showWeeksDialog = false },
            onConfirm = {
                if (currentTimetableId != null) {
                    onUpdateTimetableMetadata(currentTimetableId, currentTimetableName, currentStartDate, it)
                }
                showWeeksDialog = false
            }
        )
    }

    if (showNameDialog) {
        var nameInput by remember { mutableStateOf(currentTimetableName) }
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text(stringResource(R.string.timetable_name)) },
            text = {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text(stringResource(R.string.name)) },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (nameInput.isNotBlank()) {
                         if (currentTimetableId != null) {
                             onUpdateTimetableMetadata(currentTimetableId, nameInput, currentStartDate, currentTotalWeeks)
                         }
                    }
                    showNameDialog = false
                }) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
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
                Text(
                    text = stringResource(R.string.current_timetable_settings),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(16.dp)
                )
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
                        headlineContent = { Text(stringResource(R.string.timetable_name)) },
                        supportingContent = { Text(currentTimetableName) },
                        modifier = Modifier.clickable { showNameDialog = true }
                    )
                }

                item {
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.start_date)) },
                        supportingContent = { Text(dateFormat.format(Date(currentStartDate))) },
                        modifier = Modifier.clickable { showDatePicker = true }
                    )
                }

                item {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.total_weeks)) },
                        supportingContent = { Text(currentTotalWeeks.toString()) },
                        modifier = Modifier.clickable { showWeeksDialog = true }
                    )
                }
            }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.show_weekends)) },
                    supportingContent = { Text(stringResource(R.string.show_weekends_desc)) },
                    trailingContent = {
                        Switch(
                            checked = showWeekends,
                            onCheckedChange = onToggleShowWeekends
                        )
                    }
                )
            }

            item {
                Text(
                    text = stringResource(R.string.app_settings),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(16.dp)
                )
            }
            
            // Empty for now
        }
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
            showWeekends = false,
            onUpdateTimetableMetadata = { _, _, _, _ -> },
            onToggleShowWeekends = {}
        )
    }
}
