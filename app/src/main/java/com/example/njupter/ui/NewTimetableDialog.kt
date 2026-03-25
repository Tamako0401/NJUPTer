package com.example.njupter.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.example.njupter.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NewTimetableDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Long, Int) -> Unit,
    onImportClick: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var totalWeeks by remember { mutableStateOf("20") }
    var showDatePicker by remember { mutableStateOf(false) }

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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.new_timetable)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (onImportClick != null) {
                    FilledTonalButton(
                        onClick = onImportClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("📥 从教务系统快捷导入")
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                OutlinedTextField(
                    value = dateFormat.format(Date(startDate)),
                    onValueChange = { },
                    label = { Text(stringResource(R.string.start_date)) },
                    readOnly = true,
                    enabled = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true },
                    colors = androidx.compose.material3.TextFieldDefaults.colors(
                        disabledContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                        disabledIndicatorColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )

                OutlinedTextField(
                    value = totalWeeks,
                    onValueChange = { totalWeeks = it.filter { char -> char.isDigit() } },
                    label = { Text(stringResource(R.string.weeks_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val weeks = totalWeeks.toIntOrNull() ?: 20
                    if (name.isNotBlank() && weeks > 0) {
                        onConfirm(name, startDate, weeks)
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text(stringResource(R.string.create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun NewTimetableDialogPreview() {
    MaterialTheme {
        NewTimetableDialog(
            onDismiss = {},
            onConfirm = { _, _, _ -> },
            onImportClick = {}
        )
    }
}
