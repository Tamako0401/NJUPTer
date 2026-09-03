package com.example.njupter.ui.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.njupter.R

@Composable
fun AppBottomBar(
    currentTab: Int,
    onTimetableClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        NavigationBarItem(
            selected = currentTab == 0,
            onClick = onTimetableClick,
            icon = {
                Icon(
                    Icons.Default.Home,
                    contentDescription = stringResource(R.string.cd_timetable)
                )
            },
            label = { Text(stringResource(R.string.timetable)) }
        )
        NavigationBarItem(
            selected = currentTab == 1,
            onClick = onSettingsClick,
            icon = {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = stringResource(R.string.cd_settings)
                )
            },
            label = { Text(stringResource(R.string.settings)) }
        )
    }
}
