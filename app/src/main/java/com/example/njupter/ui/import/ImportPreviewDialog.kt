package com.example.njupter.ui.import

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.njupter.domain.import.TimetableImportMatcher

@Composable
fun ImportPreviewDialog(
    importResult: TimetableImportMatcher.ImportResult,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("导入的课表") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "导入预览")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = importResult.summary)
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("新课表名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(text = "将为你自动创建一个全新的课表。")
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank()
            ) {
                Text("创建并导入")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
