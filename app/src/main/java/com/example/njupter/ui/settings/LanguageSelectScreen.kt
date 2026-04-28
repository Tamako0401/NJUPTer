package com.example.njupter.ui.settings

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.njupter.ui.animation.pressScale
import com.example.njupter.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSelectScreen(
    currentLanguageTag: String,
    onBack: () -> Unit,
    onSelectLanguage: (String) -> Unit
) {
    val normalizedLanguageTag = if (currentLanguageTag.startsWith("zh")) "zh" else "en"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.language)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .animateContentSize(animationSpec = spring())
                .padding(innerPadding)
        ) {
            item {
                LanguageOptionRow(
                    title = "跟随系统",
                    subtitle = "Follow System",
                    selected = currentLanguageTag.isEmpty() || currentLanguageTag == "system",
                    onClick = { onSelectLanguage("system") }
                )
                HorizontalDivider()
                LanguageOptionRow(
                    title = "简体中文",
                    subtitle = "简体中文",
                    selected = normalizedLanguageTag == "zh" && currentLanguageTag != "system" && currentLanguageTag.isNotEmpty(),
                    onClick = { onSelectLanguage("zh") }
                )
                HorizontalDivider()
                LanguageOptionRow(
                    title = "英语",
                    subtitle = "English",
                    selected = normalizedLanguageTag == "en" && currentLanguageTag != "system" && currentLanguageTag.isNotEmpty(),
                    onClick = { onSelectLanguage("en") }
                )
            }
        }
    }
}

@Composable
private fun LanguageOptionRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = spring())
            .pressScale(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}
