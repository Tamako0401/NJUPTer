package com.example.njupter

import android.Manifest
import android.content.res.Configuration
import android.os.Bundle
import android.os.Build
import android.view.animation.DecelerateInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource

import com.example.njupter.data.FileTimetableRepository
import com.example.njupter.ui.timetable.TimetableScreen
import com.example.njupter.viewmodels.TimetableViewModel
import com.example.njupter.data.LocalFileDataSource
import com.example.njupter.data.SharedPreferencesSettingsRepository
import com.example.njupter.ui.settings.LanguageSelectScreen
import com.example.njupter.ui.settings.SettingsScreen
import com.example.njupter.ui.settings.TimetableSettingsScreen
import com.example.njupter.ui.theme.NJUPTerTheme
import com.example.njupter.ui.settings.JwxtImportScreen
import com.example.njupter.ui.settings.dialog.ImportPreviewDialog
import com.example.njupter.data.defaultSessionTimes
import com.example.njupter.notification.CourseReminderScheduler
import com.example.njupter.notification.ReminderBootstrapper
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale

private const val ANIM_DURATION = 300
private const val TAB_ANIM_DURATION = 220
private val animEasing = FastOutSlowInEasing

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
                val enableCurrentTimeIndicator by settingsRepository.getEnableCurrentTimeIndicator().collectAsState(initial = true)
                val scope = rememberCoroutineScope()
                val baseContext = LocalContext.current
                var currentTab by remember { mutableStateOf(0) }
                var showJwxtImport by remember { mutableStateOf(false) }
                var settingsSubPage by remember { mutableStateOf("main") }

                // Only keyed on languageTag — other config changes (dark mode, font scale)
                // don't affect string resolution from the context, so we avoid unnecessary
                // createConfigurationContext calls.
                val localizedContext = remember(baseContext, appLanguageTag) {
                    val locale = when {
                        appLanguageTag.startsWith("zh") -> Locale.SIMPLIFIED_CHINESE
                        appLanguageTag.startsWith("en") -> Locale.ENGLISH
                        else -> null
                    }
                    if (locale == null) {
                        baseContext
                    } else {
                        val config = Configuration(baseContext.resources.configuration)
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

                // Reschedule reminders when timetable identity changes.
                // courseInfos and sessions are NOT keys — the repository emits them
                // on every mutation, which would reschedule N times per import/add.
                // ReminderScheduler reads current repo state when it fires, so we only
                // need to trigger on structural changes.
                val reminderKey = uiState.isLoading to uiState.currentTimetableId
                LaunchedEffect(reminderKey) {
                    if (!uiState.isLoading && uiState.currentTimetableId != null) {
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

                    AnimatedContent(
                        targetState = showJwxtImport,
                        transitionSpec = {
                            slideInHorizontally(
                                initialOffsetX = { it / 10 },
                                animationSpec = tween(ANIM_DURATION, easing = animEasing)
                            ) + fadeIn(animationSpec = tween(ANIM_DURATION)) togetherWith
                                slideOutHorizontally(
                                    targetOffsetX = { -it / 10 },
                                    animationSpec = tween(ANIM_DURATION, easing = animEasing)
                                ) + fadeOut(animationSpec = tween(ANIM_DURATION))
                        },
                        label = "importTransition"
                    ) { showImport ->
                        if (showImport) {
                            JwxtImportScreen(
                                onBack = { showJwxtImport = false },
                                onCookiesObtained = { cookie, xh ->
                                    viewModel.fetchAndProcessImport(cookie, xh)
                                }
                            )
                        } else {
                            BackHandler(enabled = settingsSubPage != "main") {
                                settingsSubPage = "main"
                            }

                            Scaffold(
                                bottomBar = {
                                    NavigationBar {
                                        NavigationBarItem(
                                            selected = currentTab == 0,
                                            onClick = {
                                                currentTab = 0
                                                settingsSubPage = "main"
                                            },
                                            icon = { Icon(Icons.Default.Home, contentDescription = stringResource(R.string.cd_timetable)) },
                                            label = { Text(stringResource(R.string.timetable)) }
                                        )
                                        NavigationBarItem(
                                            selected = currentTab == 1 && settingsSubPage == "main",
                                            onClick = {
                                                currentTab = 1
                                            },
                                            icon = { Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.cd_settings)) },
                                            label = { Text(stringResource(R.string.settings)) }
                                        )
                                    }
                                }
                            ) { innerPadding ->
                                Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                                    AnimatedContent(
                                        targetState = currentTab to settingsSubPage,
                                        transitionSpec = {
                                            val (fromTab, fromSubPage) = initialState
                                            val (toTab, toSubPage) = targetState

                                            val isEnteringSubPage = fromSubPage == "main" && toSubPage != "main"
                                            val isLeavingSubPage = fromSubPage != "main" && toSubPage == "main"
                                            val isTabSwitch = fromTab != toTab

                                            if (isEnteringSubPage || isLeavingSubPage) {
                                                // Push: enter from right, exit to left
                                                // Pop:  enter from left, exit to right
                                                val enterOffset: (Int) -> Int = if (isEnteringSubPage) ({ it }) else ({ -it / 3 })
                                                val exitOffset: (Int) -> Int = if (isEnteringSubPage) ({ -it / 3 }) else ({ it })

                                                (slideInHorizontally(
                                                    initialOffsetX = enterOffset,
                                                    animationSpec = tween(ANIM_DURATION, easing = animEasing)
                                                ) + fadeIn(animationSpec = tween(ANIM_DURATION)))
                                                    .togetherWith(
                                                        slideOutHorizontally(
                                                            targetOffsetX = exitOffset,
                                                            animationSpec = tween(ANIM_DURATION, easing = animEasing)
                                                        ) + fadeOut(animationSpec = tween(200))
                                                    ).using(SizeTransform(clip = false))
                                            } else {
                                                (fadeIn(
                                                    animationSpec = tween(TAB_ANIM_DURATION, delayMillis = 90)
                                                ) + scaleIn(
                                                    initialScale = 0.92f,
                                                    animationSpec = tween(TAB_ANIM_DURATION, delayMillis = 90)
                                                )).togetherWith(
                                                    fadeOut(animationSpec = tween(90))
                                                )
                                            }
                                        },
                                        label = "contentTransition"
                                    ) { (tab, subPage) ->
                                        when {
                                            tab == 0 -> {
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
                                        enableCurrentTimeIndicator = enableCurrentTimeIndicator,
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
                                            }
                                            subPage == "language" -> {
                                                LanguageSelectScreen(
                                                    currentLanguageTag = appLanguageTag,
                                                    onBack = { settingsSubPage = "main" },  // {settingsSubPage = "main"}这个东西叫做无参lambda，表示被调用时要执行的语句
                                                    onSelectLanguage = { languageTag ->
                                                        scope.launch {
                                                            settingsRepository.setAppLanguageTag(languageTag)
                                                            // lambda 的写法是： { 参数列表 -> 函数体 }
                                                            // -> 左边把参数接住，右边是lambda被调用时要执行的代码
                                                        }
                                                    }
                                                )
                                            }
                                            subPage == "timetable" -> {
                                                TimetableSettingsScreen(
                                                    currentTimetableName = uiState.currentTimetableName,
                                                    currentStartDate = uiState.currentStartDate,
                                                    currentTotalWeeks = uiState.currentTotalWeeks,
                                                    currentShowWeekends = uiState.showWeekends,
                                                    currentSessionTimes = uiState.currentSessionTimes,
                                                    onBack = { settingsSubPage = "main" },
                                                    onSave = { name, startDate, weeks, showWeekends, sessionTimes ->
                                                        uiState.currentTimetableId?.let { timetableId ->
                                                            viewModel.updateTimetableMetadata(
                                                                timetableId,
                                                                name,
                                                                startDate,
                                                                weeks,
                                                                showWeekends,
                                                                sessionTimes
                                                            )
                                                        }
                                                    }
                                                )
                                            }
                                            else -> {
                                                SettingsScreen(
                                                    currentTimetableId = uiState.currentTimetableId,
                                                    currentTimetableName = uiState.currentTimetableName,
                                                    currentLanguageTag = appLanguageTag,
                                                    enableCurrentTimeIndicator = enableCurrentTimeIndicator,
                                                    onLanguageSelectClick = { settingsSubPage = "language" },
                                                    onTimetableSettingsClick = { settingsSubPage = "timetable" },
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
                        }
                    }
                }
            }
        }
    }
}
