package com.example.njupter.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.res.stringResource
import com.example.njupter.R
import com.example.njupter.data.TimetableMetadata
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableSelectorDialog(
    timetables: List<TimetableMetadata>,
    currentId: String?,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
    onCreate: (String, Long) -> Unit
) {
    var showCreate by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }

    if (showCreate) {
        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(initialSelectedDateMillis = startDate)
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        startDate = datePickerState.selectedDateMillis ?: startDate
                        showDatePicker = false
                    }) {
                        Text(stringResource(R.string.confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text(stringResource(R.string.new_timetable)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text(stringResource(R.string.name)) },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(stringResource(R.string.start_date), style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(dateFormat.format(Date(startDate)))
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (newName.isNotBlank()) {
                        onCreate(newName, startDate)
                        showCreate = false
                    }
                }) {
                    Text(stringResource(R.string.create))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreate = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    } else {
        Dialog(onDismissRequest = onDismiss) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.padding(16.dp).fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(stringResource(R.string.select_timetable), style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 300.dp)
                    ) {
                        items(timetables) { meta ->
                            ListItem(
                                headlineContent = { Text(meta.name) },
                                supportingContent = { 
                                    val date = Date(meta.lastModified)
                                    val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                                    Text(stringResource(R.string.last_modified, format.format(date)))
                                },
                                trailingContent = {
                                    if (meta.id == currentId) {
                                        Icon(Icons.Default.Check, contentDescription = stringResource(R.string.cd_selected))
                                    }
                                },
                                modifier = Modifier.clickable {
                                    onSelect(meta.id)
                                    onDismiss()
                                }
                            )
                            HorizontalDivider()
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { showCreate = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.new_timetable))
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TimetableSelectorDialogPreview() {
    val sampleTimetables = listOf(
        TimetableMetadata("1", "2026春季学期", System.currentTimeMillis()),
        TimetableMetadata("2", "2026秋季学期", System.currentTimeMillis() - 86400000)
    )
    MaterialTheme {
        TimetableSelectorDialog(
            timetables = sampleTimetables,
            currentId = "1",
            onDismiss = {},
            onSelect = {},
            onCreate = { _, _ -> }
        )
    }
}
