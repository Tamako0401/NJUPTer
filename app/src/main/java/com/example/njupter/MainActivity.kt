package com.example.njupter

import android.Manifest
import android.content.res.Configuration
import android.os.Bundle
import android.os.Build
import android.view.animation.DecelerateInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import android.content.pm.PackageManager
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.lifecycleScope
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.example.njupter.data.FileTimetableRepository
import com.example.njupter.ui.timetable.TimetableScreen
import com.example.njupter.viewmodels.TimetableViewModel
import com.example.njupter.data.LocalFileDataSource
import com.example.njupter.data.SharedPreferencesSettingsRepository
import com.example.njupter.ui.settings.LanguageSelectScreen
import com.example.njupter.ui.settings.SettingsScreen
import com.example.njupter.ui.theme.NJUPTerTheme
import com.example.njupter.ui.settings.JwxtImportScreen
import com.example.njupter.ui.settings.dialog.ImportPreviewDialog
import com.example.njupter.data.defaultSessionTimes
import com.example.njupter.notification.CourseReminderScheduler
import com.example.njupter.notification.ReminderBootstrapper
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * 初始化依赖关系，连接ViewModel与UI，设置应用主题
 */

class MainActivity : ComponentActivity() {
    private fun applyLocaleToActivityResources(languageTag: String) {
        val locale = when {
            languageTag.startsWith("zh") -> Locale.SIMPLIFIED_CHINESE
            languageTag.startsWith("en") -> Locale.ENGLISH
            else -> Locale.getDefault()
        }
        Locale.setDefault(locale)

        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)

        applicationContext.resources.updateConfiguration(
            Configuration(applicationContext.resources.configuration).apply { setLocale(locale) },
            applicationContext.resources.displayMetrics
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        var keepSplash = true
        splashScreen.setKeepOnScreenCondition { keepSplash }
        splashScreen.setOnExitAnimationListener { splashScreenViewProvider ->
            splashScreenViewProvider.view.animate()
                .alpha(0f)
                .setDuration(220L)
                .setInterpolator(DecelerateInterpolator())
                .withEndAction { splashScreenViewProvider.remove() }
                .start()
        }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()  // 全面屏适配

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }

        val dataSource = LocalFileDataSource(this)
        val settingsRepository = SharedPreferencesSettingsRepository(this)
        val repository = FileTimetableRepository(dataSource, settingsRepository)    // 实例化TimetableRepository，传入MainActivity的Context来读取assets下的JSON
        val reminderScheduler = CourseReminderScheduler(this)

        lifecycleScope.launch {
            ReminderBootstrapper.rescheduleCurrentTimetable(applicationContext)
        }

        val viewModel by viewModels<TimetableViewModel> {
            TimetableViewModel.provideFactory(repository, settingsRepository)
        }

        lifecycleScope.launch {
            viewModel.uiState.collectLatest { uiState ->
                keepSplash = uiState.isLoading
            }
        }

        setContent {
            NJUPTerTheme {  // 主题包装
                val uiState by viewModel.uiState.collectAsState()   // 观察状态，将StateFlow转换成Compose的State
                val importState by viewModel.importState.collectAsState()   // 同上
                val appLanguageTag by settingsRepository.getAppLanguageTag().collectAsState(initial = settingsRepository.peekAppLanguageTag())
                val scope = rememberCoroutineScope()
                val baseContext = LocalContext.current
                val currentConfig = LocalConfiguration.current
                var currentTab by remember { mutableStateOf(0) }
                var showJwxtImport by remember { mutableStateOf(false) }
                var showLanguageSelect by remember { mutableStateOf(false) }

                val localizedContext = remember(baseContext, currentConfig, appLanguageTag) {
                    val locale = when {
                        appLanguageTag.startsWith("zh") -> Locale.SIMPLIFIED_CHINESE
                        appLanguageTag.startsWith("en") -> Locale.ENGLISH
                        else -> null
                    }
                    if (locale == null) {
                        baseContext
                    } else {
                        val config = Configuration(currentConfig)
                        config.setLocale(locale)
                        baseContext.createConfigurationContext(config)
                    }
                }

                LaunchedEffect(appLanguageTag) {
                    applyLocaleToActivityResources(appLanguageTag)
                }

                CompositionLocalProvider(LocalContext provides localizedContext) {
                    // 导入预览对话框
                    importState.result?.let { result ->
                        ImportPreviewDialog(
                            importResult = result,
                            onConfirm = { name ->
                                viewModel.createAndImportTimetable(
                                    name = name,
                                    startDate = System.currentTimeMillis(),
                                    totalWeeks = 20,
                                    showWeekends = true,
                                    sessionTimes = defaultSessionTimes,
                                    newCourses = result.newCourses,
                                    newSessions = result.newSessions
                                )
                                viewModel.clearImportState()
                                showJwxtImport = false
                            },
                            onDismiss = {
                                viewModel.clearImportState()
                            }
                        )
                    }

                LaunchedEffect(
                    uiState.currentTimetableId,
                    uiState.currentStartDate,
                    uiState.currentTotalWeeks,
                    uiState.currentSessionTimes,
                    uiState.courseInfos,
                    uiState.sessions
                ) {
                    reminderScheduler.scheduleUpcomingReminders(
                        courseInfos = uiState.courseInfos,
                        sessions = uiState.sessions,
                        currentTimetableId = uiState.currentTimetableId,
                        startDate = uiState.currentStartDate,
                        totalWeeks = uiState.currentTotalWeeks,
                        sessionTimes = uiState.currentSessionTimes
                    )
                }

                    if (showJwxtImport) {
                        JwxtImportScreen(
                            onBack = { showJwxtImport = false },
                            onCookiesObtained = { cookie, xh ->
                                viewModel.fetchAndProcessImport(cookie, xh)
                            }
                        )
                    } else if (showLanguageSelect) {
                        LanguageSelectScreen(
                            currentLanguageTag = appLanguageTag,
                            onBack = { showLanguageSelect = false },
                            onSelectLanguage = { languageTag ->
                                scope.launch {
                                    settingsRepository.setAppLanguageTag(languageTag)
                                }
                                showLanguageSelect = false
                            }
                        )
                    } else {
                        Scaffold(
                            bottomBar = {
                                NavigationBar {
                                    NavigationBarItem(
                                        selected = currentTab == 0,
                                        onClick = { currentTab = 0 },
                                        icon = { Icon(Icons.Default.Home, contentDescription = stringResource(R.string.cd_timetable)) },
                                        label = { Text(stringResource(R.string.timetable)) }
                                    )
                                    NavigationBarItem(
                                        selected = currentTab == 1,
                                        onClick = { currentTab = 1 },
                                        icon = { Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.cd_settings)) },
                                        label = { Text(stringResource(R.string.settings)) }
                                    )
                                }
                            }
                        ) { innerPadding ->
                            Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                                if (currentTab == 0) {
                                    TimetableScreen(
                                        courseInfos = uiState.courseInfos,
                                        courseSessions = uiState.sessions,
                                        timetables = uiState.timetables,
                                        currentTimetableName = uiState.currentTimetableName,
                                        currentTimetableId = uiState.currentTimetableId,
                                        currentStartDate = uiState.currentStartDate,
                                        currentTotalWeeks = uiState.currentTotalWeeks,
                                        currentWeek = uiState.currentWeek,
                                        sessionTimes = uiState.currentSessionTimes,
                                        showWeekends = uiState.showWeekends,
                                        isLoading = uiState.isLoading,
                                        onAddCourse = viewModel::addCourse,
                                        onAddSession = viewModel::addSession,
                                        onUpdateCourse = viewModel::updateCourse,
                                        onUpdateSession = viewModel::updateSession,
                                        onDeleteSession = viewModel::deleteSession,
                                        onSwitchTimetable = viewModel::switchTimetable,
                                        onCurrentWeekChange = viewModel::setCurrentWeek,
                                        onCreateTimetable = viewModel::createTimetable,
                                        onImportClick = { showJwxtImport = true }
                                    )
                                } else {
                                    SettingsScreen(
                                        currentTimetableId = uiState.currentTimetableId,
                                        currentTimetableName = uiState.currentTimetableName,
                                        currentStartDate = uiState.currentStartDate,
                                        currentTotalWeeks = uiState.currentTotalWeeks,
                                        currentSessionTimes = uiState.currentSessionTimes,
                                        currentShowWeekends = uiState.showWeekends,
                                        currentLanguageTag = appLanguageTag,
                                        onLanguageSelectClick = { showLanguageSelect = true },
                                        onUpdateTimetableMetadata = viewModel::updateTimetableMetadata
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
