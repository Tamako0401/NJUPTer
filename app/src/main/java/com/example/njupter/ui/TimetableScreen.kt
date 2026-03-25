package com.example.njupter.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.njupter.R
import com.example.njupter.data.CourseInfo
import com.example.njupter.data.CourseSession
import com.example.njupter.data.TimetableMetadata
import com.example.njupter.ui.theme.getCourseColors
import com.example.njupter.domain.getDateForWeekDay
import kotlinx.coroutines.launch
import kotlin.math.abs
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableScreen(
    courseInfos: List<CourseInfo>,
    courseSessions: List<CourseSession>,
    timetables: List<TimetableMetadata> = emptyList(),
    currentTimetableName: String = "",
    currentTimetableId: String? = null,
    currentStartDate: Long = System.currentTimeMillis(),
    currentTotalWeeks: Int = 20,
    sessionTimes: List<String> = emptyList(),
    showWeekends: Boolean = true,
    onAddCourse: (CourseInfo) -> Unit = {},
    onAddSession: (CourseSession) -> Unit = {},
    onUpdateCourse: (CourseInfo) -> Unit = {},
    onUpdateSession: (CourseSession, CourseSession) -> Unit = { _, _ -> },
    onDeleteSession: (CourseSession) -> Unit = {},
    onSwitchTimetable: (String) -> Unit = {},
    onCreateTimetable: (String, Long, Int, Boolean, List<String>) -> Unit = { _, _, _, _, _ -> },
    onImportClick: (() -> Unit)? = null
) {
    val sectionHeight = 60.dp
    val sidebarWidth = 50.dp
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    val isDark = isSystemInDarkTheme()

    val currentCourseColors = getCourseColors()

    val gridBorderColor = if (isDark) Color(0xFF444444) else Color.LightGray
    val gridHeaderBg = MaterialTheme.colorScheme.surface
    val gridContentBg = MaterialTheme.colorScheme.background

    var showDialog by remember { mutableStateOf(false) }
    var editingSession by remember { mutableStateOf<CourseSession?>(null) }
    var editingCourse by remember { mutableStateOf<CourseInfo?>(null) }

    val daysCount = if (showWeekends) 7 else 5
    val dayLabels = if (showWeekends) {
        listOf(
            stringResource(R.string.day_mon),
            stringResource(R.string.day_tue),
            stringResource(R.string.day_wed),
            stringResource(R.string.day_thu),
            stringResource(R.string.day_fri),
            stringResource(R.string.day_sat),
            stringResource(R.string.day_sun)
        )
    } else {
        listOf(
            stringResource(R.string.day_mon),
            stringResource(R.string.day_tue),
            stringResource(R.string.day_wed),
            stringResource(R.string.day_thu),
            stringResource(R.string.day_fri)
        )
    }
    val maxSection = 12

    val pagerState = rememberPagerState(pageCount = { currentTotalWeeks })

    var showTimetableSelector by remember { mutableStateOf(false) }
    var showWeekSelector by remember { mutableStateOf(false) }

    // New state for NewTimetableDialog
    var showNewTimetableDialog by remember { mutableStateOf(false) }

    if (showNewTimetableDialog) {
        TimetableConfigDialog(
            onDismiss = { showNewTimetableDialog = false },
            onConfirm = { name, startDate, weeks, showWeekends, times ->
                onCreateTimetable(name, startDate, weeks, showWeekends, times)
                showNewTimetableDialog = false
            },
            onImportClick = {
                showNewTimetableDialog = false
                onImportClick?.invoke()
            }
        )
    }

    if (showTimetableSelector) {
        TimetableSelectorDialog(
            timetables = timetables,
            currentId = currentTimetableId,
            onDismiss = { showTimetableSelector = false },
            onSelect = onSwitchTimetable,
            onNewTimetable = { showNewTimetableDialog = true }
        )
    }

    // Show empty state if no timetables exist
    if (timetables.isEmpty()) {
        EmptyGuidePlaceholder(
            onCreateTimetable = { showNewTimetableDialog = true }
        )
        return
    }

    if (showWeekSelector) {
        WeekSelectorDialog(
            currentWeek = pagerState.currentPage + 1,
            totalWeeks = currentTotalWeeks,
            onDismiss = { showWeekSelector = false },
            onWeekSelected = { week ->
                scope.launch {
                    pagerState.animateScrollToPage(week - 1)
                }
                showWeekSelector = false
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .clickable { showTimetableSelector = true },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (currentTimetableName.isNotEmpty()) currentTimetableName else stringResource(
                                    R.string.timetable
                                ),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = stringResource(R.string.cd_switch_timetable),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        TextButton(
                            onClick = { showWeekSelector = true },
                            modifier = Modifier.align(Alignment.Center)
                        ) {
                            Text(
                                text = stringResource(R.string.week, pagerState.currentPage + 1),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        Row(
                            modifier = Modifier.align(Alignment.CenterEnd),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = {
                                scope.launch {
                                    val prev = (pagerState.currentPage - 1).coerceAtLeast(0)
                                    pagerState.animateScrollToPage(prev)
                                }
                            }) {
                                Icon(
                                    Icons.Default.KeyboardArrowLeft,
                                    contentDescription = stringResource(R.string.cd_previous_week)
                                )
                            }
                            IconButton(onClick = {
                                scope.launch {
                                    val next =
                                        (pagerState.currentPage + 1).coerceAtMost(currentTotalWeeks - 1)
                                    pagerState.animateScrollToPage(next)
                                }
                            }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = stringResource(R.string.cd_next_week)
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = Color.Transparent
                ),
                modifier = Modifier,
                windowInsets = WindowInsets(0.dp)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                showDialog = true
                editingSession = null
                editingCourse = null
            }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.cd_add_course))
            }
        }
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(top = 0.dp),
            verticalAlignment = Alignment.Top
        ) { page ->
            val currentWeek = page + 1
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(gridHeaderBg)
                        .border(0.5.dp, gridBorderColor)
                ) {
                    Box(
                        modifier = Modifier.width(sidebarWidth).height(45.dp)
                            .border(0.5.dp, gridBorderColor)
                    )

                    dayLabels.forEachIndexed { index, dayLabel ->
                        val dateString = getDateForWeekDay(
                            currentStartDate,
                            currentWeek,
                            index + 1
                        )
                        Box(
                            modifier = Modifier.weight(1f).height(45.dp)
                                .border(0.5.dp, gridBorderColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = dayLabel,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                )
                                Text(
                                    text = dateString,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Grid Body
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(sectionHeight * maxSection)
                        .background(gridContentBg)
                ) {
                    // Sidebar
                    Column(modifier = Modifier.width(sidebarWidth).fillMaxHeight()) {
                        (1..maxSection).forEach { section ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth().height(sectionHeight)
                                    .height(sectionHeight)
                                    .border(0.5.dp, gridBorderColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = section.toString(),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    if (section - 1 < sessionTimes.size && sessionTimes[section - 1].isNotEmpty()) {
                                        val timeStr = sessionTimes[section - 1]
                                        val parts = timeStr.split("-")
                                        if (parts.size == 2) {
                                            Text(
                                                text = parts[0],
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = androidx.compose.ui.text.font.FontWeight.Light,
                                                fontSize = 9.sp,
                                                lineHeight = 9.sp,
                                                textAlign = TextAlign.Center,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = parts[1],
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = androidx.compose.ui.text.font.FontWeight.Light,
                                                fontSize = 9.sp,
                                                lineHeight = 9.sp,
                                                textAlign = TextAlign.Center,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        } else {
                                            Text(
                                                text = timeStr,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = androidx.compose.ui.text.font.FontWeight.Light,
                                                fontSize = 9.sp,
                                                lineHeight = 9.sp,
                                                textAlign = TextAlign.Center,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Course content area
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        // 1. Grid lines layer
                        Column(modifier = Modifier.fillMaxSize()) {
                            (1..maxSection).forEach { _ ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(sectionHeight)
                                        .border(0.5.dp, gridBorderColor.copy(alpha = 0.5f))
                                )
                            }
                        }

                        // 2. Vertical lines
                        Row(modifier = Modifier.fillMaxSize()) {
                            (1..daysCount).forEach { _ ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .border(0.5.dp, gridBorderColor.copy(alpha = 0.5f))
                                )
                            }
                        }

                        // 3. Course Content
                        Row(modifier = Modifier.fillMaxSize()) {
                            (1..daysCount).forEach { day ->
                                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                    // 绘制该天的课程
                                    val sessionsForDay = courseSessions.filter {
                                        it.day == day && it.weeks.contains(currentWeek)
                                    }
                                    sessionsForDay.forEach { session ->
                                        val course = courseInfos.find { it.id == session.courseId }
                                        if (course != null) {
                                            CourseCard(
                                                course = course,
                                                session = session,
                                                sectionHeight = sectionHeight,
                                                colorsList = currentCourseColors,
                                                onClick = {
                                                    editingSession = session
                                                    editingCourse = course
                                                    showDialog = true
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

        if (showDialog) {
            CourseEditorDialog(
                initialSession = editingSession,
                initialCourse = editingCourse,
                existingCourses = courseInfos,
                existingSessions = courseSessions,
                colorsList = currentCourseColors,
                isDarkTheme = isDark,
                totalWeeks = currentTotalWeeks,
                onDismiss = { showDialog = false },
                onSave = { info, session, createNewCourse ->
                    if (createNewCourse) {
                        onAddCourse(info)
                        onAddSession(session)
                    } else {
                        if (editingCourse != null && editingCourse != info) onUpdateCourse(info)
                        if (editingSession != null && editingSession != session) onUpdateSession(
                            editingSession!!,
                            session
                        )
                    }
                    showDialog = false
                },
                onDelete = {
                    if (editingSession != null) {
                        onDeleteSession(editingSession!!)
                    }
                    showDialog = false
                }
            )
        }
    }
}
