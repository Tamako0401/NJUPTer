package com.example.njupter.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.njupter.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeScreen(
    showWeekends: Boolean,
    onToggleShowWeekends: (Boolean) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.me)) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.show_weekends)) },
                supportingContent = { Text(stringResource(R.string.show_weekends_desc)) },
                trailingContent = {
                    Switch(
                        checked = showWeekends,
                        onCheckedChange = onToggleShowWeekends
                    )
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MeScreenPreview() {
    MaterialTheme {
        MeScreen(
            showWeekends = false,
            onToggleShowWeekends = {}
        )
    }
}

