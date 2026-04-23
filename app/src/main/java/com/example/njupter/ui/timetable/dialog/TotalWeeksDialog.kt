package com.example.njupter.ui.timetable.dialog

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.example.njupter.R

@Composable
fun TotalWeeksDialog(
    initialWeeks: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var weeksInput by remember { mutableStateOf(initialWeeks.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.total_weeks)) },
        text = {
            OutlinedTextField(
                value = weeksInput,
                onValueChange = { weeksInput = it.filter { char -> char.isDigit() } },
                label = { Text(stringResource(R.string.weeks_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        },
        confirmButton = {
            TextButton(onClick = {
                weeksInput.toIntOrNull()?.let {
                    if (it > 0) {
                        onConfirm(it)
                    }
                }
            }) {
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

@Preview(showBackground = true)
@Composable
fun TotalWeeksDialogPreview() {
    MaterialTheme {
        TotalWeeksDialog(
            initialWeeks = 20,
            onDismiss = {},
            onConfirm = {}
        )
    }
}

