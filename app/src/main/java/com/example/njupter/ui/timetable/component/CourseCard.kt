package com.example.njupter.ui.timetable.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.njupter.data.CourseInfo
import com.example.njupter.data.CourseSession
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun CourseCard(
    course: CourseInfo,
    session: CourseSession,
    sectionHeight: Dp,
    colorsList: List<Color>,
    onClick: () -> Unit
) {
    val height = sectionHeight * (session.endSection - session.startSection + 1)
    val topMargin = sectionHeight * (session.startSection - 1)

    val colorIndex = if (course.colorIndex in colorsList.indices) {
        course.colorIndex
    } else {
        if (colorsList.isNotEmpty()) (course.name.hashCode() and Int.MAX_VALUE) % colorsList.size else 0
    }
    
    val backgroundColor = if (colorsList.isNotEmpty()) colorsList[colorIndex] else MaterialTheme.colorScheme.primaryContainer
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = topMargin)
            .height(height)
            .padding(1.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier.padding(4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = course.name,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            if (course.classroom.isNotEmpty()) {
                Text(
                    text = "@${course.classroom}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (course.teacher.isNotEmpty()) {
                Text(
                    text = "${course.teacher}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CourseCardPreview() {
    val sampleColors = listOf(
        Color(0xFFE3F2FD),
        Color(0xFFE8F5E9),
        Color(0xFFFFF3E0),
        Color(0xFFF3E5F5),
        Color(0xFFE0F7FA)
    )
    MaterialTheme {
        CourseCard(
            course = CourseInfo("1", "高等数学", "张老师", "教 1-101", 0),
            session = CourseSession("1", 1, 1, 2, listOf(1, 2, 3, 4, 5)),
            sectionHeight = 60.dp,
            colorsList = sampleColors,
            onClick = {}
        )
    }
}

