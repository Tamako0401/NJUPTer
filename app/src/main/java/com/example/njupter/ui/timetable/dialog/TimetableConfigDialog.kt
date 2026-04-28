package com.example.njupter.ui.timetable.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import com.example.njupter.ui.animation.pressScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.njupter.R
import com.example.njupter.data.defaultSessionTimes
import com.example.njupter.ui.theme.NJUPTerTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TimetableConfigDialog(
    initialName: String = "",
    initialStartDate: Long = System.currentTimeMillis(),
    initialTotalWeeks: Int = 20,
    initialShowWeekends: Boolean = true,
    initialSessionTimes: List<String> = defaultSessionTimes,
    isEditMode: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (String, Long, Int, Boolean, List<String>) -> Unit,
    onImportClick: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf(initialName) }
    var startDate by remember { mutableStateOf(initialStartDate) }
    var totalWeeks by remember { mutableFloatStateOf(initialTotalWeeks.toFloat()) }
    var showWeekends by remember { mutableStateOf(initialShowWeekends) }
    var sessionTimes by remember { mutableStateOf(initialSessionTimes) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showSessionTimeEditor by remember { mutableStateOf(false) }

    if (showDatePicker) {
        StartDateDialog(
            initialDateMillis = startDate,
            onDismiss = { showDatePicker = false },
            onConfirm = {
                startDate = it
                showDatePicker = false
            }
        )
    }

    if (showSessionTimeEditor) {
        SessionTimeEditorDialog(
            initialTimes = sessionTimes,
            onDismiss = { showSessionTimeEditor = false },
            onConfirm = {
                sessionTimes = it
                showSessionTimeEditor = false
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(if (isEditMode) stringResource(R.string.cur_timetable_settings) else stringResource(R.string.new_timetable)) 
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (!isEditMode && onImportClick != null) {
                    item {
                        FilledTonalButton(
                            onClick = onImportClick,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.import_from_jwxt))
                        }
                    }
                }

                // Name
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.timetable_name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Start Date
                item {
                    ConfigItemRow(
                        label = stringResource(R.string.start_date),
                        value = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(startDate)),
                        onClick = { showDatePicker = true }
                    )
                }

                // Total Weeks Slider
                item {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = stringResource(R.string.total_weeks), style = MaterialTheme.typography.bodyMedium)
                            Text(text = "${totalWeeks.toInt()}", style = MaterialTheme.typography.bodyMedium)
                        }
                        Slider(
                            value = totalWeeks,
                            onValueChange = { totalWeeks = it },
                            valueRange = 1f..30f,
                            steps = 28 // (30-1)-1 = 28 steps
                        )
                    }
                }

                // Show Weekends
                item {
                   Row(
                       modifier = Modifier.fillMaxWidth(),
                       horizontalArrangement = Arrangement.SpaceBetween,
                       verticalAlignment = Alignment.CenterVertically
                   ) {
                       Text(text = stringResource(R.string.show_weekends), style = MaterialTheme.typography.bodyMedium)
                       Switch(checked = showWeekends, onCheckedChange = { showWeekends = it })
                   }
                }

                // Session Times
                item {
                    ConfigItemRow(
                        label = stringResource(R.string.session_times_label),
                        value = stringResource(R.string.edit),
                        onClick = { showSessionTimeEditor = true }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { 
                    if (name.isNotBlank()) {
                        onConfirm(name, startDate, totalWeeks.toInt(), showWeekends, sessionTimes)
                    }
                },
                enabled = name.isNotBlank()
            ) {
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

@Composable
fun ConfigItemRow(
    label: String,
    value: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .pressScale(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                onClick = onClick
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.padding(start = 4.dp).height(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TimetableConfigDialogPreview() {
    NJUPTerTheme {
        TimetableConfigDialog(
            initialName = "2026 Spring",
            initialStartDate = System.currentTimeMillis(),
            initialTotalWeeks = 20,
            initialShowWeekends = false,
            onDismiss = {},
            onConfirm = { _, _, _, _, _ -> }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ConfigItemRowPreview() {
    NJUPTerTheme {
        ConfigItemRow(
            label = "Session Times",
            value = "Edit",
            onClick = {}
        )
    }
}
