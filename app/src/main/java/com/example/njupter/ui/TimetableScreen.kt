package com.example.njupter.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.njupter.R
import com.example.njupter.data.CourseInfo
import com.example.njupter.data.CourseSession
import com.example.njupter.data.TimetableMetadata
import com.example.njupter.data.defaultSessionTimes
import com.example.njupter.ui.theme.getCourseColors
import com.example.njupter.ui.theme.NJUPTerTheme
import com.example.njupter.domain.getDateForWeekDay
import com.example.njupter.domain.getTodayDayOfWeek
import com.example.njupter.domain.getTodayWeekIndex
import java.util.Calendar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.text.font.FontWeight
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
    currentWeek: Int = 1,
    sessionTimes: List<String> = emptyList(),
    showWeekends: Boolean = true,
    isLoading: Boolean = false,
    onAddCourse: (CourseInfo) -> Unit = {},
    onAddSession: (CourseSession) -> Unit = {},
    onUpdateCourse: (CourseInfo) -> Unit = {},
    onUpdateSession: (CourseSession, CourseSession) -> Unit = { _, _ -> },
    onDeleteSession: (CourseSession) -> Unit = {},
    onSwitchTimetable: (String) -> Unit = {},
    onCurrentWeekChange: (Int) -> Unit = {},
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
    val initialPage = remember(currentTimetableId, currentTotalWeeks, currentWeek) {
        (currentWeek - 1).coerceIn(0, (currentTotalWeeks - 1).coerceAtLeast(0))
    }

    val pagerState = key(currentTimetableId, currentTotalWeeks) {
        rememberPagerState(initialPage = initialPage, pageCount = { currentTotalWeeks })
    }
    val todayWeekIndex = remember(currentStartDate, currentTotalWeeks) {
        getTodayWeekIndex(currentStartDate, currentTotalWeeks)
    }
    val todayDayOfWeek = remember { getTodayDayOfWeek() }
    val nowMillis = rememberCurrentTimeMillis()
    val nowMinuteOfDay = remember(nowMillis) {
        val calendar = Calendar.getInstance().apply { timeInMillis = nowMillis }
        calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
    }
    val currentSectionPosition = remember(sessionTimes, nowMinuteOfDay) {
        findCurrentSectionPosition(sessionTimes, nowMinuteOfDay)
    }

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

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
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

    LaunchedEffect(pagerState, currentTimetableId) {
        if (currentTimetableId == null) return@LaunchedEffect
        snapshotFlow { pagerState.settledPage }
            .collect { page ->
                onCurrentWeekChange(page + 1)
            }
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
                                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
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
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (todayWeekIndex != null && pagerState.currentPage != todayWeekIndex) {
                    FloatingActionButton(
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(todayWeekIndex)
                            }
                        },
                    ) {
                        Text(text = stringResource(R.string.today))
                    }
                }

                FloatingActionButton(onClick = {
                    showDialog = true
                    editingSession = null
                    editingCourse = null
                }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.cd_add_course))
                }
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
            val showCurrentTimeIndicator = todayDayOfWeek <= daysCount && currentSectionPosition != null
            val currentSectionIndex = currentSectionPosition?.first
            val currentSectionProgress = currentSectionPosition?.second ?: 0f
            val currentTimeLineOffset = if (showCurrentTimeIndicator && currentSectionIndex != null) {
                sectionHeight * (currentSectionIndex + currentSectionProgress)
            } else {
                0.dp
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(gridHeaderBg)
                ) {
                    Box(
                        modifier = Modifier
                            .width(sidebarWidth)
                            .height(45.dp)
                    )

                    dayLabels.forEachIndexed { index, dayLabel ->
                        val dateString = getDateForWeekDay(
                            currentStartDate,
                            currentWeek,
                            index + 1
                        )
                        val isToday = todayWeekIndex == page && todayDayOfWeek == index + 1

                        val cellContainerColor = if (isToday) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        } else {
                            Color.Transparent
                        }
                        val dayTextColor = if (isToday) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                        val dateTextColor = if (isToday) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        val cellBorderColor = if (isToday) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                        } else {
                            Color.Transparent
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(45.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 2.dp, vertical = 3.dp)
                                    .border(
                                        width = 1.dp,
                                        color = cellBorderColor,
                                        shape = MaterialTheme.shapes.small
                                    ),
                                shape = MaterialTheme.shapes.small,
                                color = cellContainerColor
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = dayLabel,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Medium,
                                        color = dayTextColor
                                    )
                                    Text(
                                        text = dateString,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = dateTextColor,
                                        fontWeight = FontWeight.Normal
                                    )
                                }
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
                            val isCurrentSection = showCurrentTimeIndicator && currentSectionIndex == section - 1
                            val sectionContainerColor = if (isCurrentSection) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                            } else {
                                Color.Transparent
                            }
                            val sectionBorderColor = if (isCurrentSection) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                            } else {
                                Color.Transparent
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(sectionHeight),
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 2.dp, vertical = 3.dp)
                                        .border(
                                            width = 1.dp,
                                            color = sectionBorderColor,
                                            shape = MaterialTheme.shapes.small
                                        ),
                                    shape = MaterialTheme.shapes.small,
                                    color = sectionContainerColor
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
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

                        // 4. Current time line
                        if (showCurrentTimeIndicator) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .offset(y = currentTimeLineOffset)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(3.dp)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .offset(x = (-4).dp, y = (-2.5).dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = CircleShape
                                        )
                                )
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

@Composable
private fun rememberCurrentTimeMillis(tickMs: Long = 30_000L): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(tickMs)
            now = System.currentTimeMillis()
        }
    }

    return now
}

private fun findCurrentSectionPosition(sessionTimes: List<String>, currentMinuteOfDay: Int): Pair<Int, Float>? {
    sessionTimes.forEachIndexed { index, timeStr ->
        val parts = timeStr.split("-")
        if (parts.size != 2) return@forEachIndexed

        val startMinute = parseMinuteOfDay(parts[0]) ?: return@forEachIndexed
        val endMinute = parseMinuteOfDay(parts[1]) ?: return@forEachIndexed
        if (endMinute <= startMinute) return@forEachIndexed

        if (currentMinuteOfDay in startMinute until endMinute) {
            val progress = (currentMinuteOfDay - startMinute).toFloat() / (endMinute - startMinute).toFloat()
            return index to progress.coerceIn(0f, 1f)
        }
    }
    return null
}

private fun parseMinuteOfDay(text: String): Int? {
    val parts = text.trim().split(":")
    if (parts.size != 2) return null
    val hour = parts[0].toIntOrNull() ?: return null
    val minute = parts[1].toIntOrNull() ?: return null
    return hour * 60 + minute
}

@Preview(showBackground = true, widthDp = 420, heightDp = 860)
@Composable
fun TimetableScreenPreview() {
    val sampleCourses = listOf(
        CourseInfo(
            id = "c1",
            name = "Data Structures",
            teacher = "Prof. Li",
            classroom = "A-203",
            colorIndex = 0
        ),
        CourseInfo(
            id = "c2",
            name = "Mobile Development",
            teacher = "Prof. Wang",
            classroom = "B-512",
            colorIndex = 2
        )
    )

    val sampleSessions = listOf(
        CourseSession(
            courseId = "c1",
            day = 1,
            startSection = 1,
            endSection = 2,
            weeks = (1..16).toList()
        ),
        CourseSession(
            courseId = "c2",
            day = 3,
            startSection = 5,
            endSection = 6,
            weeks = (1..16).toList()
        )
    )

    val sampleTimetables = listOf(
        TimetableMetadata(
            id = "preview",
            name = "2026 Spring",
            lastModified = System.currentTimeMillis(),
            totalWeeks = 16,
            sessionTimes = defaultSessionTimes,
            showWeekends = true
        )
    )

    NJUPTerTheme {
        TimetableScreen(
            courseInfos = sampleCourses,
            courseSessions = sampleSessions,
            timetables = sampleTimetables,
            currentTimetableName = "2026 Spring",
            currentTimetableId = "preview",
            currentStartDate = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000,
            currentTotalWeeks = 16,
            currentWeek = 2,
            sessionTimes = defaultSessionTimes,
            showWeekends = true,
            isLoading = false
        )
    }
}
