package com.example.njupter.data.import

import org.jsoup.Jsoup
import org.jsoup.nodes.Document

/**
 * 远程课程数据模型，临时存放解析来的字段
 */
data class RemoteCourse(
    val name: String,
    val teacher: String,
    val classroom: String,
    val dayOfWeek: Int, // 1~7
    val startSection: Int,
    val endSection: Int,
    val weeks: List<Int>
)

/**
 * HTML 解析层：分离解析逻辑
 */
class JwxtParser {
    
    fun parseHtml(html: String): List<RemoteCourse> {
        val document: Document = Jsoup.parse(html)
        val courses = mutableListOf<RemoteCourse>()
        
        // 课表通常是一个 id 为 "Table1" 的表格
        val table = document.getElementById("Table1") ?: return emptyList()
        val tds = table.select("td[align=Center]")
        
        for (td in tds) {
            val htmlContent = td.html()
            // 过滤空单元格或不含 {} (包含上课周数) 的部分
            if (htmlContent.isBlank() || htmlContent.contains("&nbsp;")) continue
            if (!htmlContent.contains("{") && !htmlContent.contains("}")) continue
            
            // 有的 td 内包含多门课程，<br><br> 分隔这些共占用一个单元格的课
            val courseBlocks = htmlContent.split("<br><br>", "<br><br/>", "<br/><br/>")
            
            for (block in courseBlocks) {
                // 每门课程的各个属性由单个 <br> 分隔
                val lines = block.split("<br>", "<br/>").map { Jsoup.parse(it).text().trim() }.filter { it.isNotEmpty() }
                
                if (lines.size >= 3) {
                    val name = lines[0]
                    val timeWeekStr = lines[1] // 例: "周四第3,4节{第1-17周|单周}"
                    val teacher = lines[2]
                    val classroom = if (lines.size >= 4) lines[3] else "" // 第四行是上课地点，有些可能缺失
                    
                    if (timeWeekStr.length >= 2) {
                        val dayOfWeek = parseDay(timeWeekStr.substring(0, 2))
                        val (startSection, endSection) = parseSections(timeWeekStr)
                        val weeks = parseWeeks(timeWeekStr)
                        
                        if (dayOfWeek != -1 && startSection != -1) {
                            courses.add(
                                RemoteCourse(
                                    name = name,
                                    teacher = teacher,
                                    classroom = classroom,
                                    dayOfWeek = dayOfWeek,
                                    startSection = startSection,
                                    endSection = endSection,
                                    weeks = weeks
                                )
                            )
                        }
                    }
                }
            }
        }
        
        return courses
    }
    
    private fun parseDay(dayStr: String): Int {
        return when (dayStr) {
            "周一" -> 1
            "周二" -> 2
            "周三" -> 3
            "周四" -> 4
            "周五" -> 5
            "周六" -> 6
            "周日", "周天" -> 7
            else -> -1
        }
    }
    
    private fun parseSections(timeWeekStr: String): Pair<Int, Int> {
        val sectionRegex = Regex("第([\\d,]+)节")
        val match = sectionRegex.find(timeWeekStr)
        if (match != null) {
            val sections = match.groupValues[1].split(",").mapNotNull { it.toIntOrNull() }.sorted()
            if (sections.isNotEmpty()) {
                return Pair(sections.first(), sections.last())
            }
        }
        return Pair(-1, -1)
    }
    
    private fun parseWeeks(timeWeekStr: String): List<Int> {
        // 解析 "{第1-17周|单周}" 或 "{第12-14周}"
        val weekRegex = Regex("\\{第(.*?)(周|节)(\\|(.*))?\\}")
        val match = weekRegex.find(timeWeekStr)
        if (match != null) {
            val rangesStr = match.groupValues[1]
            val oddEvenType = match.groupValues[4]
            
            val allWeeks = mutableListOf<Int>()
            val ranges = rangesStr.split(",")
            for (range in ranges) {
                val bounds = range.split("-")
                if (bounds.size == 2) {
                    val start = bounds[0].toIntOrNull() ?: continue
                    val end = bounds[1].toIntOrNull() ?: continue
                    allWeeks.addAll((start..end).toList())
                } else {
                    bounds.firstOrNull()?.toIntOrNull()?.let { allWeeks.add(it) }
                }
            }
            
            return when {
                oddEvenType.contains("单") -> allWeeks.filter { it % 2 != 0 }
                oddEvenType.contains("双") -> allWeeks.filter { it % 2 == 0 }
                else -> allWeeks
            }
        }
        return emptyList()
    }
}
