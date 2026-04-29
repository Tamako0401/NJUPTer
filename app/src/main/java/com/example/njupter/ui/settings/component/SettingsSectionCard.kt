/*
    视图层：决定条目怎么画
 */
package com.example.njupter.ui.settings.component

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.ui.res.painterResource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.njupter.ui.settings.model.SettingsIcon
import androidx.compose.ui.text.style.TextOverflow
import com.example.njupter.ui.animation.pressScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.njupter.R
import com.example.njupter.ui.settings.model.SettingsItem
import com.example.njupter.ui.settings.model.SettingsSection
import com.example.njupter.ui.theme.NJUPTerTheme

@Composable
fun SettingsSectionCard(
    section: SettingsSection,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        SettingsSectionHeader(
            title = section.title,
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .offset(x = (-8).dp)
        )
        Surface(
            modifier = Modifier.animateContentSize(
                animationSpec = tween(200)
            ),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                section.items.forEachIndexed { index, item ->
                    when (item) {
                        is SettingsItem.Navigation -> NavigableSettingsRow(item = item)
                        is SettingsItem.Toggle -> ToggleSettingsRow(item = item)
                    }
                    if (index < section.items.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NavigableSettingsRow(item: SettingsItem.Navigation) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                onClick = item.onClick
            )
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (val icon = item.icon) {
            is SettingsIcon.Vector -> Icon(
                imageVector = icon.imageVector,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            is SettingsIcon.Drawable -> Icon(
                painter = painterResource(id = icon.resId),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = item.title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp)
        )
        if (!item.value.isNullOrBlank()) {
            Text(
                text = item.value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun ToggleSettingsRow(item: SettingsItem.Toggle) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                onClick = item.onToggle
            )
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (val icon = item.icon) {
                is SettingsIcon.Vector -> Icon(
                    imageVector = icon.imageVector,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                is SettingsIcon.Drawable -> Icon(
                    painter = painterResource(id = icon.resId),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
        Switch(
            checked = item.checked,
            onCheckedChange = { item.onToggle() }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsSectionCardPreview() {
    NJUPTerTheme {
        SettingsSectionCard(
            section = SettingsSection(
                title = "General Settings",
                items = listOf(
                    SettingsItem.Navigation(
                        icon = SettingsIcon.Vector(Icons.Default.Settings),
                        title = "Language",
                        value = "English",
                        onClick = {}
                    ),
                    SettingsItem.Toggle(
                        icon = SettingsIcon.Vector(Icons.Default.Notifications),
                        title = "Enable Notifications",
                        checked = true,
                        onToggle = {}
                    ),
                    SettingsItem.Navigation(
                        icon = SettingsIcon.Vector(Icons.Default.Info),
                        title = "About",
                        onClick = {}
                    )
                )
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LanguageSelectScreenPreview(){
    NJUPTerTheme {
        SettingsSectionCard(
            section = SettingsSection(
                title = stringResource(R.string.language),
                items = listOf(
                    SettingsItem.Navigation(
                        icon = SettingsIcon.Vector(Icons.Default.Language),
                        title = stringResource(R.string.language_system),
                        value = null,
                        onClick = {}
                    ),
                    SettingsItem.Navigation(
                        icon = SettingsIcon.Vector(Icons.Default.Language),
                        title = stringResource(R.string.language_zh),
                        value = "\u2713",
                        onClick = {}
                    ),
                    SettingsItem.Navigation(
                        icon = SettingsIcon.Vector(Icons.Default.Language),
                        title = stringResource(R.string.language_en),
                        value = null,
                        onClick = {}
                    )
                )
            )
        )
    }
}