@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.njupter.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.example.njupter.R
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionTimeEditorDialog(
    initialTimes: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit
) {
    // Ensure we have 12 items to edit
    var times by remember {
        mutableStateOf(
            if (initialTimes.size < 12) {
                initialTimes + List(12 - initialTimes.size) { "" }
            } else {
                initialTimes.take(12)
            }
        )
    }

    // State for the currently editing index
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    val conflictIndexes = remember(times) { findConflictIndexes(times) }

    if (editingIndex != null) {
        val index = editingIndex!!
        val currentTimeStr = times[index]
        
        SessionTimePickerDialog(
            initialTimeStr = currentTimeStr,
            sessionIndex = index + 1,
            onDismiss = { editingIndex = null },
            onConfirm = { newTimeStr ->
                val newList = times.toMutableList()
                newList[index] = newTimeStr
                times = newList
                editingIndex = null
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Session Times") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (conflictIndexes.isNotEmpty()) {
                    Text(
                        text = "Some sessions overlap. Conflicting cards are highlighted in red.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                LazyColumn(
                    modifier = Modifier.heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(times) { index, time ->
                        val hasConflict = index in conflictIndexes
                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { editingIndex = index },
                            colors = CardDefaults.outlinedCardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            border = BorderStroke(
                                width = if (hasConflict) 2.dp else 1.dp,
                                color = if (hasConflict) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    modifier = Modifier.width(36.dp),
                                    style = MaterialTheme.typography.titleMedium
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Text(
                                    text = if (time.isNotBlank()) time else "Set time...",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (hasConflict) {
                                        MaterialTheme.colorScheme.error
                                    } else if (time.isNotBlank()) {
                                        MaterialTheme.colorScheme.onSurface
                                    } else {
                                        MaterialTheme.colorScheme.outline
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(times) }) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

private fun findConflictIndexes(times: List<String>): Set<Int> {
    val parsed = times.map { parseTimeRangeToMinutesOrNull(it) }
    val conflicts = mutableSetOf<Int>()

    for (i in parsed.indices) {
        val left = parsed[i] ?: continue
        if (left.first >= left.second) {
            conflicts.add(i)
            continue
        }
        for (j in i + 1 until parsed.size) {
            val right = parsed[j] ?: continue
            if (right.first >= right.second) {
                conflicts.add(j)
                continue
            }
            val overlap = left.first < right.second && right.first < left.second
            if (overlap) {
                conflicts.add(i)
                conflicts.add(j)
            }
        }
    }

    return conflicts
}

private fun parseTimeRangeToMinutesOrNull(timeStr: String): Pair<Int, Int>? {
    if (timeStr.isBlank()) return null
    val parts = timeStr.split("-")
    if (parts.size != 2) return null
    val start = parseSingleTimeToMinutes(parts[0]) ?: return null
    val end = parseSingleTimeToMinutes(parts[1]) ?: return null
    return start to end
}

private fun parseSingleTimeToMinutes(time: String): Int? {
    val hm = time.split(":")
    if (hm.size != 2) return null
    val h = hm[0].toIntOrNull() ?: return null
    val m = hm[1].toIntOrNull() ?: return null
    if (h !in 0..23 || m !in 0..59) return null
    return h * 60 + m
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionTimePickerDialog(
    initialTimeStr: String,
    sessionIndex: Int,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    // Parse initial string "HH:mm-HH:mm"
    // If invalid, default to current time for start, +45m for end
    val (startHour, startMinute, endHour, endMinute) = remember(initialTimeStr) {
        parseTimeRange(initialTimeStr)
    }

    val startTimeState = rememberTimePickerState(
        initialHour = startHour,
        initialMinute = startMinute,
        is24Hour = true
    )
    val endTimeState = rememberTimePickerState(
        initialHour = endHour,
        initialMinute = endMinute,
        is24Hour = true
    )

    // 0 = Start, 1 = End
    var selectedTab by remember { mutableStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Session $sessionIndex Time") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Start Time") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("End Time") }
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                if (selectedTab == 0) {
                    TimePicker(state = startTimeState)
                } else {
                    TimePicker(state = endTimeState)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val start = String.format("%02d:%02d", startTimeState.hour, startTimeState.minute)
                val end = String.format("%02d:%02d", endTimeState.hour, endTimeState.minute)
                onConfirm("$start-$end")
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

fun parseTimeRange(timeStr: String): List<Int> {
    val default = listOf(8, 0, 8, 45) // Default 08:00 - 08:45
    if (timeStr.isBlank()) return default
    
    return try {
        val parts = timeStr.split("-")
        if (parts.size != 2) return default
        
        val startParts = parts[0].split(":")
        val endParts = parts[1].split(":")
        
        if (startParts.size != 2 || endParts.size != 2) return default
        
        listOf(
            startParts[0].toInt(), startParts[1].toInt(),
            endParts[0].toInt(), endParts[1].toInt()
        )
    } catch (e: Exception) {
        default
    }
}

@Preview(showBackground = true)
@Composable
fun SessionTimeEditorDialogPreview() {
    MaterialTheme {
        SessionTimeEditorDialog(
            initialTimes = listOf(
                "08:00-08:45",
                "08:55-09:40",
                "10:00-10:45",
                "10:55-11:40",
                "14:00-14:45",
                "14:55-15:40",
                "16:00-16:45",
                "16:55-17:40",
                "18:30-19:15",
                "19:25-20:10",
                "20:20-21:05",
                ""
            ),
            onDismiss = {},
            onConfirm = {}
        )
    }
}
