package com.example.njupter.ui

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
import kotlin.math.abs

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
            .padding(1.dp) // Small spacing between cards
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
        }
    }
}

