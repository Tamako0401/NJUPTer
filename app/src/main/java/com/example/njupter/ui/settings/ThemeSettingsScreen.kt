package com.example.njupter.ui.settings

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.njupter.R
import com.example.njupter.ui.animation.predictiveback.PredictiveBackAnimation
import com.example.njupter.ui.animation.predictiveback.PredictiveBackExitDirection
import com.example.njupter.ui.settings.component.SettingsSectionCard
import com.example.njupter.ui.settings.model.SettingsIcon
import com.example.njupter.ui.settings.model.SettingsItem
import com.example.njupter.ui.settings.model.SettingsSection
import com.example.njupter.ui.theme.AppThemeMode
import com.example.njupter.ui.theme.LocalHasCustomBackground
import androidx.compose.ui.graphics.Color

private enum class ThemeChoiceDialog {
    THEME_MODE,
    LANGUAGE,
    APP_DPI
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSettingsScreen(
    themeMode: AppThemeMode,
    dynamicColorEnabled: Boolean,
    appLanguageTag: String,
    appDpi: Int,
    systemDpi: Int,
    customBackgroundEnabled: Boolean,
    backgroundDim: Float,
    predictiveBackAnimation: PredictiveBackAnimation,
    predictiveBackExitDirection: PredictiveBackExitDirection,
    onThemeModeChange: (AppThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onAppLanguageChange: (String) -> Unit,
    onAppDpiChange: (Int) -> Unit,
    onCustomBackgroundChange: (Boolean) -> Unit,
    onBackgroundDimChange: (Float) -> Unit,
    onPredictiveBackAnimationChange: (PredictiveBackAnimation) -> Unit,
    onPredictiveBackExitDirectionChange: (PredictiveBackExitDirection) -> Unit,
    onBack: () -> Unit
) {
    var choiceDialog by remember { mutableStateOf<ThemeChoiceDialog?>(null) }
    var predictiveAnimationDialog by remember { mutableStateOf(false) }
    var predictiveExitDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val pickBackground = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null && copyBackgroundToAppStorage(context, uri)) {
            onCustomBackgroundChange(true)
        }
    }

    val appearanceItems = listOf(
        SettingsItem.Navigation(
            icon = SettingsIcon.Vector(Icons.Default.Language),
            title = stringResource(R.string.app_language),
            description = stringResource(R.string.app_language_summary),
            value = appLanguageLabel(appLanguageTag),
            onClick = { choiceDialog = ThemeChoiceDialog.LANGUAGE }
        ),
        SettingsItem.Navigation(
            icon = SettingsIcon.Vector(Icons.Default.DarkMode),
            title = stringResource(R.string.theme_mode),
            description = stringResource(R.string.theme_mode_summary),
            value = themeMode.label(),
            onClick = { choiceDialog = ThemeChoiceDialog.THEME_MODE }
        ),
        SettingsItem.Toggle(
            icon = SettingsIcon.Vector(Icons.Default.ColorLens),
            title = stringResource(R.string.dynamic_color),
            description = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                stringResource(R.string.dynamic_color_summary)
            } else {
                stringResource(R.string.dynamic_color_unavailable)
            },
            checked = dynamicColorEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
            onToggle = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    onDynamicColorChange(!dynamicColorEnabled)
                }
            }
        ),
        SettingsItem.Navigation(
            icon = SettingsIcon.Vector(Icons.Default.AspectRatio),
            title = stringResource(R.string.app_dpi),
            description = stringResource(R.string.app_dpi_summary),
            value = if (appDpi > 0) "$appDpi" else stringResource(R.string.app_dpi_system),
            onClick = { choiceDialog = ThemeChoiceDialog.APP_DPI }
        ),
        SettingsItem.Toggle(
            icon = SettingsIcon.Vector(Icons.Default.Wallpaper),
            title = stringResource(R.string.custom_background),
            description = stringResource(R.string.custom_background_summary),
            checked = customBackgroundEnabled,
            onToggle = {
                if (!customBackgroundEnabled) pickBackground.launch("image/*")
                else onCustomBackgroundChange(false)
            }
        ),
    )

    val motionItems = buildList {
        add(
            SettingsItem.Navigation(
                icon = SettingsIcon.Vector(Icons.Default.Animation),
                title = stringResource(R.string.predictive_back_animation),
                description = stringResource(R.string.predictive_back_animation_summary),
                value = predictiveBackAnimation.label(),
                onClick = { predictiveAnimationDialog = true }
            )
        )
        if (predictiveBackAnimation != PredictiveBackAnimation.NONE) {
            add(
                SettingsItem.Navigation(
                    icon = SettingsIcon.Vector(Icons.Default.SwapHoriz),
                    title = stringResource(R.string.predictive_back_exit_direction),
                    description = stringResource(R.string.predictive_back_exit_direction_summary),
                    value = predictiveBackExitDirection.label(),
                    onClick = { predictiveExitDialog = true }
                )
            )
        }
    }

    Scaffold(
        containerColor = if (LocalHasCustomBackground.current) Color.Transparent
        else MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.theme_settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Top + WindowInsetsSides.Horizontal
        )
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = innerPadding.calculateTopPadding(),
                end = 16.dp,
                bottom = innerPadding.calculateBottomPadding() + 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                SettingsSectionCard(
                    section = SettingsSection(
                        title = stringResource(R.string.appearance_settings),
                        items = appearanceItems
                    )
                )
            }
            item {
                SettingsSectionCard(
                    section = SettingsSection(
                        title = stringResource(R.string.motion_settings),
                        items = motionItems
                    )
                )
            }
            if (customBackgroundEnabled) {
                item {
                    BackgroundDimSlider(
                        dim = backgroundDim,
                        onDimChange = onBackgroundDimChange
                    )
                }
            }
        }
    }

    when (choiceDialog) {
        ThemeChoiceDialog.THEME_MODE -> {
            SingleChoiceSettingsDialog(
                title = stringResource(R.string.theme_mode),
                labels = listOf(
                    stringResource(R.string.theme_mode_system),
                    stringResource(R.string.theme_mode_light),
                    stringResource(R.string.theme_mode_dark)
                ),
                selectedIndex = when (themeMode) {
                    AppThemeMode.SYSTEM -> 0
                    AppThemeMode.LIGHT -> 1
                    AppThemeMode.DARK -> 2
                },
                onDismiss = { choiceDialog = null },
                onConfirm = { index ->
                    onThemeModeChange(
                        when (index) {
                            0 -> AppThemeMode.SYSTEM
                            1 -> AppThemeMode.LIGHT
                            else -> AppThemeMode.DARK
                        }
                    )
                    choiceDialog = null
                }
            )
        }

        ThemeChoiceDialog.LANGUAGE -> {
            SingleChoiceSettingsDialog(
                title = stringResource(R.string.app_language),
                labels = listOf(
                    stringResource(R.string.language_system),
                    stringResource(R.string.language_zh),
                    stringResource(R.string.language_en)
                ),
                selectedIndex = when {
                    appLanguageTag.isEmpty() || appLanguageTag == "system" -> 0
                    appLanguageTag.startsWith("zh") -> 1
                    else -> 2
                },
                onDismiss = { choiceDialog = null },
                onConfirm = { index ->
                    onAppLanguageChange(
                        when (index) {
                            0 -> "system"
                            1 -> "zh"
                            else -> "en"
                        }
                    )
                    choiceDialog = null
                }
            )
        }

        ThemeChoiceDialog.APP_DPI -> {
            AppDpiDialog(
                currentDpi = if (appDpi > 0) appDpi else systemDpi,
                isCustom = appDpi > 0,
                onDismiss = { choiceDialog = null },
                onConfirm = { dpi ->
                    onAppDpiChange(dpi)
                    choiceDialog = null
                }
            )
        }

        null -> Unit
    }

    if (predictiveAnimationDialog) {
        SingleChoiceSettingsDialog(
            title = stringResource(R.string.predictive_back_animation),
            labels = listOf(
                stringResource(R.string.predictive_back_animation_none),
                stringResource(R.string.predictive_back_animation_aosp),
                stringResource(R.string.predictive_back_animation_miuix),
                stringResource(R.string.predictive_back_animation_scale),
                stringResource(R.string.predictive_back_animation_ksu_classic)
            ),
            selectedIndex = when (predictiveBackAnimation) {
                PredictiveBackAnimation.NONE -> 0
                PredictiveBackAnimation.AOSP -> 1
                PredictiveBackAnimation.MIUIX -> 2
                PredictiveBackAnimation.SCALE -> 3
                PredictiveBackAnimation.KSU_CLASSIC -> 4
            },
            onDismiss = { predictiveAnimationDialog = false },
            onConfirm = { index ->
                onPredictiveBackAnimationChange(
                    when (index) {
                        0 -> PredictiveBackAnimation.NONE
                        1 -> PredictiveBackAnimation.AOSP
                        2 -> PredictiveBackAnimation.MIUIX
                        3 -> PredictiveBackAnimation.SCALE
                        else -> PredictiveBackAnimation.KSU_CLASSIC
                    }
                )
                predictiveAnimationDialog = false
            }
        )
    }

    if (predictiveExitDialog) {
        SingleChoiceSettingsDialog(
            title = stringResource(R.string.predictive_back_exit_direction),
            labels = listOf(
                stringResource(R.string.predictive_back_exit_direction_follow_gesture),
                stringResource(R.string.predictive_back_exit_direction_always_right),
                stringResource(R.string.predictive_back_exit_direction_always_left)
            ),
            selectedIndex = when (predictiveBackExitDirection) {
                PredictiveBackExitDirection.FOLLOW_GESTURE -> 0
                PredictiveBackExitDirection.ALWAYS_RIGHT -> 1
                PredictiveBackExitDirection.ALWAYS_LEFT -> 2
            },
            onDismiss = { predictiveExitDialog = false },
            onConfirm = { index ->
                onPredictiveBackExitDirectionChange(
                    when (index) {
                        0 -> PredictiveBackExitDirection.FOLLOW_GESTURE
                        1 -> PredictiveBackExitDirection.ALWAYS_RIGHT
                        else -> PredictiveBackExitDirection.ALWAYS_LEFT
                    }
                )
                predictiveExitDialog = false
            }
        )
    }
}

@Composable
private fun BackgroundDimSlider(
    dim: Float,
    onDimChange: (Float) -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.background_dim),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.background_dim_summary),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Slider(
                    value = dim,
                    onValueChange = onDimChange,
                    valueRange = 0f..1f,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${(dim * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }
        }
    }
}

private fun copyBackgroundToAppStorage(
    context: android.content.Context,
    source: android.net.Uri
): Boolean {
    return try {
        val dst = java.io.File(context.filesDir, "custom_background.jpg")
        context.contentResolver.openInputStream(source)?.use { input ->
            dst.outputStream().use { output -> input.copyTo(output, 8 * 1024) }
        } != null
    } catch (e: Exception) {
        false
    }
}

@Composable
private fun appLanguageLabel(tag: String): String = when {
    tag.isEmpty() || tag == "system" -> stringResource(R.string.language_system)
    tag.startsWith("zh") -> stringResource(R.string.language_zh)
    else -> stringResource(R.string.language_en)
}

@Composable
private fun SingleChoiceSettingsDialog(
    title: String,
    labels: List<String>,
    selectedIndex: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                labels.forEachIndexed { index, label ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = index == selectedIndex,
                            onClick = {
                                onConfirm(index)
                            }
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
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

private const val DPI_MIN = 160
private const val DPI_MAX = 600

@Composable
private fun AppDpiDialog(
    currentDpi: Int,
    isCustom: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var tempDpi by remember { mutableIntStateOf(currentDpi.coerceIn(DPI_MIN, DPI_MAX)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.app_dpi)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.app_dpi_dialog_summary),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Slider(
                        value = tempDpi.toFloat(),
                        onValueChange = { tempDpi = it.toInt() },
                        valueRange = DPI_MIN.toFloat()..DPI_MAX.toFloat(),
                        steps = (DPI_MAX - DPI_MIN) / 40 - 1,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "$tempDpi",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    DpiPresetChip(
                        label = stringResource(R.string.app_dpi_small),
                        presetDpi = 240,
                        currentDpi = tempDpi,
                        onClick = { tempDpi = 240 }
                    )
                    DpiPresetChip(
                        label = stringResource(R.string.app_dpi_medium),
                        presetDpi = 320,
                        currentDpi = tempDpi,
                        onClick = { tempDpi = 320 }
                    )
                    DpiPresetChip(
                        label = stringResource(R.string.app_dpi_large),
                        presetDpi = 420,
                        currentDpi = tempDpi,
                        onClick = { tempDpi = 420 }
                    )
                    DpiPresetChip(
                        label = stringResource(R.string.app_dpi_extra_large),
                        presetDpi = 560,
                        currentDpi = tempDpi,
                        onClick = { tempDpi = 560 }
                    )
                }
                if (isCustom) {
                    TextButton(
                        onClick = {
                            onConfirm(0)
                            onDismiss()
                        },
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(stringResource(R.string.app_dpi_reset_system))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(tempDpi)
                    onDismiss()
                },
                enabled = tempDpi != currentDpi
            ) {
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

@Composable
private fun DpiPresetChip(
    label: String,
    presetDpi: Int,
    currentDpi: Int,
    onClick: () -> Unit
) {
    val selected = presetDpi == currentDpi
    Surface(
        shape = MaterialTheme.shapes.small,
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer
        else MaterialTheme.colorScheme.surfaceContainerHighest,
        onClick = onClick
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun AppThemeMode.label(): String = when (this) {
    AppThemeMode.SYSTEM -> stringResource(R.string.theme_mode_system)
    AppThemeMode.LIGHT -> stringResource(R.string.theme_mode_light)
    AppThemeMode.DARK -> stringResource(R.string.theme_mode_dark)
}

@Composable
private fun PredictiveBackAnimation.label(): String = when (this) {
    PredictiveBackAnimation.NONE -> stringResource(R.string.predictive_back_animation_none)
    PredictiveBackAnimation.AOSP -> stringResource(R.string.predictive_back_animation_aosp)
    PredictiveBackAnimation.MIUIX -> stringResource(R.string.predictive_back_animation_miuix)
    PredictiveBackAnimation.SCALE -> stringResource(R.string.predictive_back_animation_scale)
    PredictiveBackAnimation.KSU_CLASSIC -> stringResource(R.string.predictive_back_animation_ksu_classic)
}

@Composable
private fun PredictiveBackExitDirection.label(): String = when (this) {
    PredictiveBackExitDirection.FOLLOW_GESTURE -> stringResource(R.string.predictive_back_exit_direction_follow_gesture)
    PredictiveBackExitDirection.ALWAYS_RIGHT -> stringResource(R.string.predictive_back_exit_direction_always_right)
    PredictiveBackExitDirection.ALWAYS_LEFT -> stringResource(R.string.predictive_back_exit_direction_always_left)
}
