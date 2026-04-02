package com.example.njupter.domain.import

import com.example.njupter.data.CourseInfo
import com.example.njupter.data.CourseSession
import com.example.njupter.data.import.RemoteCourse
import java.util.UUID

/**
 * 将从远程爬取的数据映射到本地域领域模型，并处理重名/匹配
 */
class TimetableImportMatcher {
    
    data class ImportResult(
        val newCourses: List<CourseInfo>,
        val newSessions: List<CourseSession>,
        // 比如想提供更新差异报告可以用这个
        val summary: String
    )
    
    fun matchAndConvert(
        remoteCourses: List<RemoteCourse>,
        existingCourses: List<CourseInfo>,
        existingSessions: List<CourseSession>,
        summaryTemplate: (Int, Int) -> String = { courses, sessions -> "Found $courses new courses, $sessions new sessions." }
    ): ImportResult {
        
        val newInfos = mutableListOf<CourseInfo>()
        val newSessions = mutableListOf<CourseSession>()
        var importedCount = 0

        // 去重策略示例：相同的课名、教师、地点视为同一门课 (复用同一个 Id)
        // 使用一个 Map 帮助快速查找有没有已经转换过的 CourseInfo
        val courseMap = mutableMapOf<String, CourseInfo>()
        
        // 先把现有的课放进字典（如果需要合并现有和新导入）
        existingCourses.forEach { 
            val key = "${it.name}|${it.teacher}|${it.classroom}"
            courseMap[key] = it
        }

        remoteCourses.forEach { remote ->
            val key = "${remote.name}|${remote.teacher}|${remote.classroom}"
            
            // 如果不存在，创建一个新的 CourseInfo
            val courseInfo = courseMap.getOrPut(key) {
                // 新课程，需要入库
                val newCourse = CourseInfo(
                    id = UUID.randomUUID().toString(),
                    name = remote.name,
                    teacher = remote.teacher,
                    classroom = remote.classroom,
                    colorIndex = -1 // Auto 颜色
                )
                newInfos.add(newCourse)
                newCourse
            }

            // session 生成
            val session = CourseSession(
                courseId = courseInfo.id,
                day = remote.dayOfWeek,
                startSection = remote.startSection,
                endSection = remote.endSection,
                weeks = remote.weeks
            )
            
            // 做简单的冲突检测：如果当前导入的 session 和现有的完全一致，就不重复添加
            val isDuplicate = existingSessions.any { 
                it.courseId == session.courseId && 
                it.day == session.day && 
                it.startSection == session.startSection && 
                it.endSection == session.endSection 
                // 可以加上 weeks 的比对，视逻辑严格程度
            } || newSessions.any {
                it.courseId == session.courseId && 
                it.day == session.day && 
                it.startSection == session.startSection && 
                it.endSection == session.endSection
            }
            
            if (!isDuplicate) {
                newSessions.add(session)
                importedCount++
            }
        }
        
        val totalNewCourses = newInfos.size
        return ImportResult(
            newCourses = newInfos,
            newSessions = newSessions,
            summary = summaryTemplate(totalNewCourses, importedCount)
        )
    }
}

