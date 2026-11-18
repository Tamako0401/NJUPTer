package com.example.njupter.ui.services

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.njupter.R

/**
 * 绑定小程序 token 的底部弹窗：粘贴 token（或包含 token 的文本），解析失败显示错误文案。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MorningRunTokenSheet(
    onDismiss: () -> Unit,
    onBind: (String) -> Boolean,
    modifier: Modifier = Modifier
) {
    var input by remember { mutableStateOf("") }
    var showInvalid by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = stringResource(R.string.morning_run_bind),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = input,
                onValueChange = {
                    input = it
                    showInvalid = false
                },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                placeholder = { Text(stringResource(R.string.morning_run_token_hint)) },
                isError = showInvalid,
                supportingText = if (showInvalid) {
                    {
                        Text(stringResource(R.string.morning_run_token_invalid))
                    }
                } else {
                    null
                }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
            ) {
                TextButton(
                    onClick = {
                        val pasted = clipboard.getText()?.text.orEmpty()
                        if (pasted.isNotEmpty()) {
                            input = pasted
                            showInvalid = false
                        }
                    }
                ) {
                    Text(stringResource(R.string.morning_run_paste))
                }
                Button(
                    onClick = {
                        if (onBind(input)) {
                            onDismiss()
                        } else {
                            showInvalid = true
                        }
                    }
                ) {
                    Text(stringResource(R.string.confirm))
                }
            }
        }
    }
}
