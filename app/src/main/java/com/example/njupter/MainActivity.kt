package com.example.njupter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
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
import androidx.compose.ui.res.stringResource
import com.example.njupter.data.CourseInfo
import com.example.njupter.data.CourseSession
import com.example.njupter.data.FileTimetableRepository
import com.example.njupter.ui.TimetableScreen
import com.example.njupter.ui.TimetableViewModel
import com.example.njupter.data.LocalFileDataSource
import com.example.njupter.data.SharedPreferencesSettingsRepository
import com.example.njupter.ui.SettingsScreen
import com.example.njupter.ui.theme.NJUPTerTheme
import com.example.njupter.ui.import.JwxtImportScreen
import com.example.njupter.ui.import.ImportPreviewDialog
import com.example.njupter.data.defaultSessionTimes

/**
 * 初始化依赖关系，连接ViewModel与UI，设置应用主题
 */

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()  // 全面屏适配

        val dataSource = LocalFileDataSource(this)
        val settingsRepository = SharedPreferencesSettingsRepository(this)
        val repository = FileTimetableRepository(dataSource, settingsRepository)    // 实例化TimetableRepository，传入MainActivity的Context来读取assets下的JSON

        val viewModel by viewModels<TimetableViewModel> {
            TimetableViewModel.provideFactory(repository, settingsRepository)
        }

        setContent {
            NJUPTerTheme {  // 主题包装
                val uiState by viewModel.uiState.collectAsState()   // 观察状态，将StateFlow转换成Compose的State
                val importState by viewModel.importState.collectAsState()
                var currentTab by remember { mutableStateOf(0) }
                var showJwxtImport by remember { mutableStateOf(false) }

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

                if (showJwxtImport) {
                    JwxtImportScreen(
                        onBack = { showJwxtImport = false },
                        onCookiesObtained = { cookie, xh ->
                            viewModel.fetchAndProcessImport(cookie, xh)
                        }
                    )
                } else {
                    Scaffold(
                        bottomBar = {
                            NavigationBar {
                                NavigationBarItem(
                                    selected = currentTab == 0,
                                    onClick = { currentTab = 0 },
                                    icon = { Icon(Icons.Default.Home, contentDescription = "Timetable") },
                                    label = { Text("Timetable") }
                                )
                                NavigationBarItem(
                                    selected = currentTab == 1,
                                    onClick = { currentTab = 1 },
                                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
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
                                    sessionTimes = uiState.currentSessionTimes,
                                    showWeekends = uiState.showWeekends,
                                    onAddCourse = viewModel::addCourse,
                                    onAddSession = viewModel::addSession,
                                    onUpdateCourse = viewModel::updateCourse,
                                    onUpdateSession = viewModel::updateSession,
                                    onDeleteSession = viewModel::deleteSession,
                                    onSwitchTimetable = viewModel::switchTimetable,
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

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    NJUPTerTheme {
        // ... (Preview data setup)
        val courseInfos = listOf(
            CourseInfo("math", "高等数学", "李", "教1-145"),
            CourseInfo("physics", "大学物理", "田", "教1-4191"),
            CourseInfo("cs", "计算机导论", "所", "教9-810")
        )
        val sessions = listOf(
            CourseSession("math", 1, 1, 2),
            CourseSession("math", 3, 3, 4),
            CourseSession("physics", 2, 2, 4),
            CourseSession("cs", 5, 1, 1)
        )
        TimetableScreen(courseInfos, sessions)
    }
}
