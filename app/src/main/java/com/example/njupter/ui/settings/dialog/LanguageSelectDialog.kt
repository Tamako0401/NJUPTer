package com.example.njupter.ui.settings.dialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.njupter.R

@Composable
fun LanguageSelectDialog(
    currentLanguageTag: String,
    onDismiss: () -> Unit,
    onSelectLanguage: (String) -> Unit
) {
    val normalizedLanguageTag = if (currentLanguageTag.startsWith("zh")) "zh" else "en"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.language)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectLanguage("en") },
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.language_en))
                    RadioButton(selected = normalizedLanguageTag == "en", onClick = { onSelectLanguage("en") })
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectLanguage("zh") },
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.language_zh))
                    RadioButton(selected = normalizedLanguageTag == "zh", onClick = { onSelectLanguage("zh") })
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        }
    )
}
