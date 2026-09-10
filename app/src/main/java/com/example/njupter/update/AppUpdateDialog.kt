package com.example.njupter.update

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.njupter.R
import com.example.njupter.ui.theme.AppThemeMode
import com.example.njupter.ui.theme.NJUPTerTheme

@Composable
fun StartupUpdatePrompt(viewModel: AppUpdateViewModel) {
    val update by viewModel.update.collectAsState()
    val failed by viewModel.downloadFailed.collectAsState()
    val context = LocalContext.current
    update?.let { candidate ->
        AppUpdateDialog(candidate, failed, onIgnore = viewModel::ignore, onDownload = {
            if (viewModel.download()) Toast.makeText(context, R.string.update_download_started, Toast.LENGTH_LONG).show()
        })
    }
}

@Composable
fun AppUpdateDialog(update: AppUpdate, failed: Boolean, onIgnore: () -> Unit, onDownload: () -> Unit) {
    AlertDialog(
        onDismissRequest = onIgnore,
        icon = { Icon(Icons.Default.SystemUpdate, contentDescription = null) },
        title = { Text(stringResource(R.string.update_available, update.versionName)) },
        text = {
            Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.update_description))
                if (update.notes.isNotBlank()) Text(update.notes)
                if (failed) Text(stringResource(R.string.update_download_failed), color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = { TextButton(onClick = onDownload) { Text(stringResource(R.string.update_download)) } },
        dismissButton = { TextButton(onClick = onIgnore) { Text(stringResource(R.string.update_ignore)) } }
    )
}

@Preview(showBackground = true, locale = "zh")
@Composable
fun AppUpdateDialogPreview() {
    NJUPTerTheme {
        AppUpdateDialog(AppUpdate(100, "1.1.0", "新增课程下课倒计时，优化课表导入与桌面小组件。", ""), false, {}, {})
    }
}

@Preview(showBackground = true, locale = "zh")
@Composable
fun AppUpdateDialogDarkPreview() {
    NJUPTerTheme(themeMode = AppThemeMode.DARK) {
        AppUpdateDialog(AppUpdate(100, "1.1.0", "新增课程下课倒计时，优化课表导入与桌面小组件。", ""), true, {}, {})
    }
}
