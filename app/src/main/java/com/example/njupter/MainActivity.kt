package com.example.njupter

import android.Manifest
import android.content.res.Configuration
import android.os.Bundle
import android.os.Build
import android.app.Activity
import android.view.animation.DecelerateInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import com.example.njupter.ui.LocalActivityContext
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource

import com.example.njupter.data.FileTimetableRepository
import com.example.njupter.data.LocalFileDataSource
import com.example.njupter.ui.services.ServicesScreen
import com.example.njupter.ui.services.MorningRunScreen
import com.example.njupter.ui.services.CampusPassScreen
import com.example.njupter.ui.timetable.TimetableScreen
import com.example.njupter.viewmodels.TimetableViewModel
import com.example.njupter.viewmodels.MorningRunViewModel
import com.example.njupter.viewmodels.MorningRunUiState
import com.example.njupter.data.morningrun.MorningRunRepository
import com.example.njupter.viewmodels.CampusPassViewModel
import com.example.njupter.viewmodels.CampusPassUiState
import com.example.njupter.data.campuspass.CampusPassRepository
import com.example.njupter.data.SharedPreferencesSettingsRepository
import com.example.njupter.ui.settings.LanguageSelectScreen
import com.example.njupter.ui.settings.SettingsScreen
import com.example.njupter.ui.settings.TimetableSettingsScreen
import com.example.njupter.ui.settings.ThemeSettingsScreen
import com.example.njupter.ui.settings.WidgetSettingsScreen
import com.example.njupter.ui.theme.NJUPTerTheme
import com.example.njupter.ui.settings.JwxtImportScreen
import com.example.njupter.ui.settings.dialog.ImportPreviewDialog
import com.example.njupter.ui.animation.AppNavigationTransition
import com.example.njupter.ui.animation.AppPageTransition
import com.example.njupter.ui.animation.PredictiveBackSurface
import com.example.njupter.ui.animation.PredictiveBackOwner
import com.example.njupter.data.defaultSessionTimes
import com.example.njupter.notification.CourseReminderScheduler
import com.example.njupter.notification.ReminderBootstrapper
import com.example.njupter.widget.WidgetDataManager
import com.example.njupter.widget.WidgetUpdateScheduler
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale

/** Bottom-bar primary tabs. */
private const val TAB_TIMETABLE = 0
private const val TAB_SERVICES = 1
private const val TAB_SETTINGS = 2

/**
 * 初始化依赖关系，连接ViewModel与UI，设置应用主题
 */

class MainActivity : ComponentActivity() {
    private val updateViewModel by viewModels<com.example.njupter.update.AppUpdateViewModel>()
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
        requestPeakDisplayMode()  // 每 App 帧率策略下，显式要求窗口用最高刷新率模式

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
        val reminderScheduler = CourseReminderScheduler(this, settingsRepository)

        lifecycleScope.launch {
            ReminderBootstrapper.rescheduleCurrentTimetable(applicationContext)
        }

        WidgetUpdateScheduler.scheduleMidnightRefresh(this)

        val viewModel by viewModels<TimetableViewModel> {
            TimetableViewModel.provideFactory(repository, settingsRepository, this@MainActivity)
        }
        val morningRunViewModel by viewModels<MorningRunViewModel> {
            MorningRunViewModel.provideFactory(MorningRunRepository(), settingsRepository)
        }
        val campusPassViewModel by viewModels<CampusPassViewModel> {
            CampusPassViewModel.provideFactory(CampusPassRepository(), settingsRepository)
        }

        lifecycleScope.launch {
            viewModel.uiState.collectLatest { uiState ->
                keepSplash = uiState.isLoading
                if (!uiState.isLoading && uiState.currentTimetableId != null) {
                    WidgetDataManager.refreshWidget(this@MainActivity)
                }
            }
        }

        setContent {
            val appThemeMode by settingsRepository.getAppThemeMode().collectAsState(
                initial = settingsRepository.peekAppThemeMode()
            )
            val dynamicColorEnabled by settingsRepository.getDynamicColorEnabled().collectAsState(
                initial = settingsRepository.peekDynamicColorEnabled()
            )
            val predictiveBackAnimation by settingsRepository.getPredictiveBackAnimation()
                .collectAsState(initial = settingsRepository.peekPredictiveBackAnimation())
            val predictiveBackExitDirection by settingsRepository.getPredictiveBackExitDirection()
                .collectAsState(initial = settingsRepository.peekPredictiveBackExitDirection())

            NJUPTerTheme(
                themeMode = appThemeMode,
                dynamicColor = dynamicColorEnabled
            ) {
                val uiState by viewModel.uiState.collectAsState()   // 观察状态，将StateFlow转换成Compose的State
                val morningRunState by morningRunViewModel.uiState.collectAsState()
                val campusPassState by campusPassViewModel.uiState.collectAsState()
                val importState by viewModel.importState.collectAsState()   // 同上
                val appLanguageTag by settingsRepository.getAppLanguageTag().collectAsState(initial = settingsRepository.peekAppLanguageTag())
                val enableCurrentTimeIndicator by settingsRepository.getEnableCurrentTimeIndicator().collectAsState(initial = true)
                val scope = rememberCoroutineScope()
                val baseContext = LocalContext.current
                var currentTab by remember { mutableStateOf(TAB_TIMETABLE) }
                var showJwxtImport by remember { mutableStateOf(false) }
                var subPage by remember { mutableStateOf("main") }
                fun selectTab(tab: Int) {
                    if (currentTab == tab) subPage = "main" else { currentTab = tab; subPage = "main" }
                }

                // Only keyed on languageTag — other config changes (dark mode, font scale)
                // don't affect string resolution from the context, so we avoid unnecessary
                // createConfigurationContext calls.
                val configuration = LocalConfiguration.current
                val localizedContext = remember(baseContext, appLanguageTag, configuration) {
                    val locale = when {
                        appLanguageTag.startsWith("zh") -> Locale.SIMPLIFIED_CHINESE
                        appLanguageTag.startsWith("en") -> Locale.ENGLISH
                        else -> null
                    }
                    if (locale == null) {
                        baseContext
                    } else {
                        val config = Configuration(configuration)
                        config.setLocale(locale)
                        baseContext.createConfigurationContext(config)
                    }
                }

                LaunchedEffect(appLanguageTag) {
                    applyLocaleToActivityResources(appLanguageTag)
                }

                CompositionLocalProvider(
                    LocalContext provides localizedContext,
                    LocalActivityContext provides this@MainActivity,
                    LocalActivityResultRegistryOwner provides this@MainActivity
                ) {
                    if (!uiState.isLoading && importState.result == null && importState.error == null) {
                        com.example.njupter.update.StartupUpdatePrompt(updateViewModel)
                    }
                    // 导入预览对话框
                    importState.result?.let { result ->
                        ImportPreviewDialog(
                            importResult = result,
                            onConfirm = { name, startDate ->
                                viewModel.createAndImportTimetable(
                                    name = name,
                                    startDate = startDate,
                                    totalWeeks = 20,
                                    showWeekends = false,
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

                    importState.error?.let { error ->
                        AlertDialog(
                            onDismissRequest = viewModel::clearImportState,
                            title = { Text(stringResource(R.string.import_failed)) },
                            text = { Text(error) },
                            confirmButton = {
                                TextButton(onClick = viewModel::clearImportState) {
                                    Text(stringResource(R.string.confirm))
                                }
                            }
                        )
                    }

                // Rebuild alarms after course edits as well as timetable changes.
                // Coalesce the repository's course/session emissions during import.
                val reminderLeadMinutes by settingsRepository.getReminderLeadMinutes().collectAsState(initial = settingsRepository.peekReminderLeadMinutes())
                val reminderKey = listOf(uiState.isLoading, uiState.currentTimetableId, uiState.courseInfos, uiState.sessions, uiState.currentStartDate, uiState.currentTotalWeeks, uiState.currentSessionTimes, reminderLeadMinutes)
                LaunchedEffect(reminderKey) {
                    if (!uiState.isLoading) {
                        kotlinx.coroutines.delay(250)
                        reminderScheduler.scheduleUpcomingReminders(
                            courseInfos = uiState.courseInfos,
                            sessions = uiState.sessions,
                            currentTimetableId = uiState.currentTimetableId,
                            startDate = uiState.currentStartDate,
                            totalWeeks = uiState.currentTotalWeeks,
                            sessionTimes = uiState.currentSessionTimes
                        )
                    }
                }

                    PredictiveBackSurface(
                        enabled = showJwxtImport || subPage != "main" || currentTab != TAB_TIMETABLE,
                        owner = when {
                            showJwxtImport -> PredictiveBackOwner.IMPORT_PAGE
                            subPage != "main" || currentTab != TAB_TIMETABLE -> PredictiveBackOwner.SETTINGS_PAGE
                            else -> null
                        },
                        animation = predictiveBackAnimation,
                        exitDirection = predictiveBackExitDirection,
                        onBack = {
                            when {
                                showJwxtImport -> showJwxtImport = false
                                subPage != "main" -> subPage = "main"
                                else -> currentTab = TAB_TIMETABLE
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        AppPageTransition(
                            showImport = showJwxtImport,
                            modifier = Modifier.fillMaxSize(),
                            importContent = {
                                JwxtImportScreen(
                                    isActive = showJwxtImport,
                                    onBack = { showJwxtImport = false },
                                    onTimetableHtmlObtained = { html ->
                                        viewModel.processTimetableImport(html)
                                    }
                                )
                            },
                            mainContent = {
                                Scaffold(
                                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                                    bottomBar = {
                                        NavigationBar {
                                            NavigationBarItem(
                                                selected = currentTab == TAB_TIMETABLE,
                                                onClick = { selectTab(TAB_TIMETABLE) },
                                                icon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                                                label = { Text(stringResource(R.string.timetable)) }
                                            )
                                            NavigationBarItem(
                                                selected = currentTab == TAB_SERVICES,
                                                onClick = { selectTab(TAB_SERVICES) },
                                                icon = { Icon(Icons.Default.Apps, contentDescription = null) },
                                                label = { Text(stringResource(R.string.services)) }
                                            )
                                            NavigationBarItem(
                                                selected = currentTab == TAB_SETTINGS,
                                                onClick = { selectTab(TAB_SETTINGS) },
                                                icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                                label = { Text(stringResource(R.string.settings)) }
                                            )
                                        }
                                    }
                                ) { inner ->
                                    AppNavigationTransition(
                                        currentTab = currentTab,
                                        subPage = subPage,
                                        modifier = Modifier.fillMaxSize().padding(inner)
                                    ) { tab, page ->
                                        when {
                                            tab == TAB_TIMETABLE -> {
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
                                                    showNonCurrentWeekCourses = uiState.showNonCurrentWeekCourses,
                                                    enableCurrentTimeIndicator = enableCurrentTimeIndicator,
                                                    onAddCourse = viewModel::addCourse,
                                                    onAddSession = viewModel::addSession,
                                                    onUpdateCourse = viewModel::updateCourse,
                                                    onUpdateSession = viewModel::updateSession,
                                                    onDeleteSession = viewModel::deleteSession,
                                                    onSwitchTimetable = viewModel::switchTimetable,
                                                    onDeleteTimetable = viewModel::deleteTimetable,
                                                    onCurrentWeekChange = viewModel::setCurrentWeek,
                                                    onCreateTimetable = viewModel::createTimetable,
                                                    onImportClick = { showJwxtImport = true }
                                                )
                                            }
                                            tab == TAB_SERVICES && page == "main" -> {
                                                ServicesScreen(
                                                    boundName = when (val state = morningRunState) {
                                                        is MorningRunUiState.Ready -> state.token.name
                                                        else -> null
                                                    },
                                                    count = when (val state = morningRunState) {
                                                        is MorningRunUiState.Ready -> state.snapshot.count
                                                        else -> null
                                                    },
                                                    campusPassSummary = when (val state = campusPassState) {
                                                        is CampusPassUiState.Ready -> stringResource(
                                                            if (state.snapshot.onCampus == true) R.string.campus_pass_on_campus
                                                            else R.string.campus_pass_off_campus
                                                        )
                                                        else -> null
                                                    },
                                                    onMorningRunClick = { subPage = "morning_run" },
                                                    onCampusPassClick = { subPage = "campus_pass" }
                                                )
                                            }
                                            tab == TAB_SERVICES && page == "morning_run" -> {
                                                MorningRunScreen(
                                                    uiState = morningRunState,
                                                    onBack = { subPage = "main" },
                                                    onRefresh = morningRunViewModel::refresh,
                                                    onBind = morningRunViewModel::bind,
                                                    onClear = morningRunViewModel::clearToken
                                                )
                                            }
                                            tab == TAB_SERVICES && page == "campus_pass" -> {
                                                CampusPassScreen(
                                                    uiState = campusPassState,
                                                    onBack = { subPage = "main" },
                                                    onRefresh = campusPassViewModel::refresh,
                                                    onBind = campusPassViewModel::bind,
                                                    onClear = campusPassViewModel::clearToken
                                                )
                                            }
                                            page == "theme" -> {
                                                ThemeSettingsScreen(
                                                    themeMode = appThemeMode,
                                                    dynamicColorEnabled = dynamicColorEnabled,
                                                    predictiveBackAnimation = predictiveBackAnimation,
                                                    predictiveBackExitDirection = predictiveBackExitDirection,
                                                    onThemeModeChange = { mode ->
                                                        scope.launch {
                                                            settingsRepository.setAppThemeMode(mode)
                                                        }
                                                    },
                                                    onDynamicColorChange = { enabled ->
                                                        scope.launch {
                                                            settingsRepository.setDynamicColorEnabled(enabled)
                                                        }
                                                    },
                                                    onPredictiveBackAnimationChange = { animation ->
                                                        scope.launch {
                                                            settingsRepository.setPredictiveBackAnimation(animation)
                                                        }
                                                    },
                                                    onPredictiveBackExitDirectionChange = { direction ->
                                                        scope.launch {
                                                            settingsRepository.setPredictiveBackExitDirection(direction)
                                                        }
                                                    },
                                                    onBack = { subPage = "main" }
                                                )
                                            }
                                            page == "language" -> {
                                                LanguageSelectScreen(
                                                    currentLanguageTag = appLanguageTag,
                                                    onBack = { subPage = "main" },
                                                    onSelectLanguage = { languageTag ->
                                                        scope.launch {
                                                            settingsRepository.setAppLanguageTag(languageTag)
                                                        }
                                                    }
                                                )
                                            }
                                            page == "timetable" -> {
                                                TimetableSettingsScreen(
                                                    currentTimetableName = uiState.currentTimetableName,
                                                    currentStartDate = uiState.currentStartDate,
                                                    currentTotalWeeks = uiState.currentTotalWeeks,
                                                    currentShowWeekends = uiState.showWeekends,
                                                    currentShowNonCurrentWeekCourses = uiState.showNonCurrentWeekCourses,
                                                    currentSessionTimes = uiState.currentSessionTimes,
                                                    onBack = { subPage = "main" },
                                                    onSave = { name, startDate, weeks, showWeekends, showNonCurrentWeekCourses, sessionTimes ->
                                                        uiState.currentTimetableId?.let { timetableId ->
                                                            viewModel.updateTimetableMetadata(
                                                                timetableId,
                                                                name,
                                                                startDate,
                                                                weeks,
                                                                showWeekends,
                                                                showNonCurrentWeekCourses,
                                                                sessionTimes
                                                            )
                                                        }
                                                    },
                                                    onDelete = {
                                                        uiState.currentTimetableId?.let(viewModel::deleteTimetable)
                                                    }
                                                )
                                            }
                                            page == "widget" -> {
                                                WidgetSettingsScreen(
                                                    onBack = { subPage = "main" }
                                                )
                                            }
                                            else -> {
                                                SettingsScreen(
                                                    currentTimetableId = uiState.currentTimetableId,
                                                    currentTimetableName = uiState.currentTimetableName,
                                                    currentLanguageTag = appLanguageTag,
                                                    currentThemeMode = appThemeMode,
                                                    onBack = { currentTab = TAB_TIMETABLE },
                                                    reminderLeadMinutes = reminderLeadMinutes,
                                                    onReminderLeadMinutesChange = { minutes -> scope.launch { settingsRepository.setReminderLeadMinutes(minutes) } },
                                                    enableCurrentTimeIndicator = enableCurrentTimeIndicator,
                                                    onThemeSettingsClick = { subPage = "theme" },
                                                    onLanguageSelectClick = { subPage = "language" },
                                                    onTimetableSettingsClick = { subPage = "timetable" },
                                                    onWidgetSettingsClick = { subPage = "widget" },
                                                    onToggleCurrentTimeIndicator = { enabled ->
                                                        scope.launch {
                                                            settingsRepository.setEnableCurrentTimeIndicator(enabled)
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 请求本窗口使用机型支持的最高刷新率。
 * 部分 OEM 会用自身的每 App 帧率策略覆盖 AOSP 的 ARR（View 投票、touch boost），
 * 所以除 preferredRefreshRate 之外还要显式给出显示模式 id。
 */
private fun Activity.requestPeakDisplayMode() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
    val currentDisplay = display ?: return
    val currentMode = currentDisplay.mode
    val peak = currentDisplay.supportedModes
        .filter { it.physicalWidth == currentMode.physicalWidth &&
            it.physicalHeight == currentMode.physicalHeight && it.refreshRate > 60f }
        ?.maxByOrNull { it.refreshRate }
        ?: return
    window.attributes = window.attributes.apply {
        preferredRefreshRate = peak.refreshRate
        preferredDisplayModeId = peak.modeId
    }
}
