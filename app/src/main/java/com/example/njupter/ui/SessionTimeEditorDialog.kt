@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.njupter.ui

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.njupter.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionTimeEditorDialog(
    initialTimes: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit
) {
    var times by remember {
        mutableStateOf(
            if (initialTimes.size < 12) {
                initialTimes + List(12 - initialTimes.size) { "" }
            } else {
                initialTimes.take(12)
            }
        )
    }

    var editingIndex by remember { mutableStateOf<Int?>(null) }
    val validationResult = remember(times) { validateSessionTimes(times) }
    val invalidIndexes = validationResult.invalidIndexes
    val conflictIndexes = validationResult.overlapIndexes
    val hasValidationError = invalidIndexes.isNotEmpty() || conflictIndexes.isNotEmpty()

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val screenWidthDp = configuration.screenWidthDp

    val columnCount = when {
        isLandscape && screenWidthDp >= 1000 -> 3
        isLandscape -> 2
        else -> 1
    }

    if (editingIndex != null) {
        val index = editingIndex!!
        SessionTimePickerDialog(
            initialTimeStr = times[index],
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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(if (isLandscape) 0.82f else 0.92f)
                .wrapContentHeight(),
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = stringResource(R.string.edit_session_times),
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (invalidIndexes.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.session_invalid_range),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (conflictIndexes.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.session_overlap),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(columnCount),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = configuration.screenHeightDp.dp * 0.65f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(times) { index, time ->
                        val hasConflict = index in conflictIndexes || index in invalidIndexes

                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { editingIndex = index },
                            colors = CardDefaults.outlinedCardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            border = BorderStroke(
                                width = if (hasConflict) 2.dp else 1.dp,
                                color = if (hasConflict) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.session_section, index + 1),
                                    modifier = Modifier
                                        .width(68.dp)
                                        .padding(start = 8.dp),
                                    textAlign = TextAlign.Start,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = if (time.isNotBlank()) time else stringResource(R.string.set_time_placeholder),
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 8.dp),
                                    textAlign = TextAlign.End,
                                    maxLines = 1,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        hasConflict -> MaterialTheme.colorScheme.error
                                        time.isNotBlank() -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.outline
                                    }
                                )

                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = if (hasConflict) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }
                    TextButton(
                        onClick = { onConfirm(times) },
                        enabled = !hasValidationError
                    ) {
                        Text(stringResource(R.string.confirm))
                    }
                }
            }
        }
    }
}

private data class SessionTimeValidationResult(
    val invalidIndexes: Set<Int>,
    val overlapIndexes: Set<Int>
)

private fun validateSessionTimes(times: List<String>): SessionTimeValidationResult {
    val parsed = times.map { parseTimeRangeToMinutesOrNull(it) }
    val invalidIndexes = mutableSetOf<Int>()
    val overlapIndexes = mutableSetOf<Int>()

    for (i in parsed.indices) {
        val left = parsed[i] ?: continue
        if (left.first >= left.second) {
            invalidIndexes.add(i)
            continue
        }
        for (j in i + 1 until parsed.size) {
            val right = parsed[j] ?: continue
            if (right.first >= right.second) {
                invalidIndexes.add(j)
                continue
            }
            val overlap = left.first < right.second && right.first < left.second
            if (overlap) {
                overlapIndexes.add(i)
                overlapIndexes.add(j)
            }
        }
    }

    return SessionTimeValidationResult(
        invalidIndexes = invalidIndexes,
        overlapIndexes = overlapIndexes
    )
}

private fun parseTimeRangeToMinutesOrNull(timeStr: String): Pair<Int, Int>? {
    if (timeStr.isBlank()) return null
    val parts = timeStr.split("-").map { it.trim() }
    if (parts.size != 2) return null
    val start = parseSingleTimeToMinutes(parts[0]) ?: return null
    val end = parseSingleTimeToMinutes(parts[1]) ?: return null
    return start to end
}

private fun parseSingleTimeToMinutes(time: String): Int? {
    val hm = time.trim().split(":")
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

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

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
    val isRangeValid = remember(
        startTimeState.hour,
        startTimeState.minute,
        endTimeState.hour,
        endTimeState.minute
    ) {
        val startMinutes = startTimeState.hour * 60 + startTimeState.minute
        val endMinutes = endTimeState.hour * 60 + endTimeState.minute
        endMinutes > startMinutes
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(if (isLandscape) 0.75f else 0.9f)
                .wrapContentHeight(),
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.edit_session_time, sessionIndex),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text(stringResource(R.string.start_time)) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text(stringResource(R.string.end_time)) }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                val currentState = if (selectedTab == 0) startTimeState else endTimeState
                TimePicker(state = currentState)
                if (!isRangeValid) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.end_before_start),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }
                    TextButton(
                        onClick = {
                            val start =
                                "%02d:%02d".format(startTimeState.hour, startTimeState.minute)
                            val end = "%02d:%02d".format(endTimeState.hour, endTimeState.minute)
                            onConfirm("$start-$end")
                        },
                        enabled = isRangeValid
                    ) {
                        Text(stringResource(R.string.confirm))
                    }
                }
            }
        }
    }
}

fun parseTimeRange(timeStr: String): List<Int> {
    val default = listOf(8, 0, 8, 45) // Default 08:00 - 08:45
    if (timeStr.isBlank()) return default
    
    return try {
        val parts = timeStr.split("-").map { it.trim() }
        if (parts.size != 2) return default
        
        val startParts = parts[0].split(":").map { it.trim() }
        val endParts = parts[1].split(":").map { it.trim() }
        
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
