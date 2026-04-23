package com.example.njupter.ui.settings.model

import androidx.compose.ui.graphics.vector.ImageVector

data class SettingsSection(
    val title: String,
    val items: List<SettingsItem>
)

sealed interface SettingsItem {
    val icon: ImageVector
    val title: String

    data class Toggle(
        override val icon: ImageVector,
        override val title: String,
        val checked: Boolean,
        val onToggle: () -> Unit
    ) : SettingsItem

    data class Navigation(
        override val icon: ImageVector,
        override val title: String,
        val value: String? = null,
        val onClick: () -> Unit
    ) : SettingsItem
}

