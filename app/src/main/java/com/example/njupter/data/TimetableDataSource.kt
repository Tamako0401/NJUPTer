package com.example.njupter.data

import android.content.Context
import java.util.UUID
import java.io.InputStreamReader
import java.io.File
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 数据源接口：负责数据的原始读取（不包含业务逻辑）和写入
 */
interface TimetableDataSource {
    suspend fun getAllTimetables(): List<TimetableMetadata>
    suspend fun createTimetable(name: String, startDate: Long, totalWeeks: Int): TimetableMetadata
    suspend fun updateTimetableMetadata(id: String, name: String, startDate: Long, totalWeeks: Int)
    suspend fun loadTimetable(id: String): TimetableData
    suspend fun saveTimetable(id: String, data: TimetableData)
    suspend fun deleteTimetable(id: String)
}

data class TimetableMetadata(
    val id: String,
    val name: String,
    val lastModified: Long,
    val startDate: Long = System.currentTimeMillis(),
    val totalWeeks: Int = 20
)

/**
 * 封装一次性加载的数据包
 */
data class TimetableData(
    val courses: List<CourseInfo>,
    val sessions: List<CourseSession>
)

// --- DTOs (Data Transfer Objects) 用于 JSON 解析 ---
private data class TimetableJsonRoot(
    val courses: List<CourseInfoJson>,
    val sessions: List<CourseSessionJson>
)

private data class CourseInfoJson(
    val id: String, 
    val name: String, 
    val teacher: String, 
    val room: String,
    val colorIndex: Int = -1
)

private data class CourseSessionJson(
    val courseId: String, 
    val dayOfWeek: Int, 
    val startNode: Int, 
    val length: Int,
    val weeks: List<Int>? = null
)
private data class TimetableIndex(
    val timetables: List<TimetableMetadata>
)

/**
 * Assets 实现：仅从 APK 包内的 assets/timetable.json 读取
 */
class AssetTimetableDataSource(private val context: Context) : TimetableDataSource {

    private val gson = Gson()
    private val defaultId = "default_assets"

    override suspend fun getAllTimetables(): List<TimetableMetadata> {
        return listOf(TimetableMetadata(defaultId, "Default (Assets)", 0L, System.currentTimeMillis(), 20))
    }

    override suspend fun createTimetable(name: String, startDate: Long, totalWeeks: Int): TimetableMetadata {
        throw UnsupportedOperationException("Cannot create timetable in assets")
    }

    override suspend fun updateTimetableMetadata(id: String, name: String, startDate: Long, totalWeeks: Int) {
        throw UnsupportedOperationException("Cannot update timetable in assets")
    }

    override suspend fun loadTimetable(id: String): TimetableData = withContext(Dispatchers.IO) {
        if (id != defaultId) return@withContext TimetableData(emptyList(), emptyList())
        try {
            val inputStream = context.assets.open("timetable.json")
            val reader = InputStreamReader(inputStream)
            val root = gson.fromJson(reader, TimetableJsonRoot::class.java)
            reader.close()

            val domainCourses = root.courses.map {
                CourseInfo(id = it.id, name = it.name, teacher = it.teacher, classroom = it.room, colorIndex = it.colorIndex)
            }
            val domainSessions = root.sessions.map {
                CourseSession(
                    courseId = it.courseId,
                    day = it.dayOfWeek,
                    startSection = it.startNode,
                    endSection = it.startNode + it.length - 1,
                    weeks = it.weeks ?: (1..20).toList()
                )
            }
            return@withContext TimetableData(domainCourses, domainSessions)
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext TimetableData(emptyList(), emptyList())
        }
    }

    override suspend fun saveTimetable(id: String, data: TimetableData) {
        // Read-only
    }
    
    override suspend fun deleteTimetable(id: String) {
        // Read-only
    }
}

/**
 * 本地文件存储实现：支持多课表
 */
class LocalFileDataSource(private val context: Context) : TimetableDataSource {

    private val gson = Gson()
    private val oldFileName = "timetable_data.json"
    private val indexFileName = "timetables_index.json"

    private fun getIndexFile() = File(context.filesDir, indexFileName)
    private fun getDataFile(id: String) = File(context.filesDir, "timetable_$id.json")

    override suspend fun getAllTimetables(): List<TimetableMetadata> = withContext(Dispatchers.IO) {
        val indexFile = getIndexFile()
        if (indexFile.exists()) {
            try {
                // Manually parse list for now as type token might be complex without helper
                // Or better, define TimetableIndex wrapper
                val json = indexFile.readText()
                val index = gson.fromJson(json, TimetableIndex::class.java)
                return@withContext index.timetables
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext emptyList()
            }
        }

        // Migration logic
        val oldFile = File(context.filesDir, oldFileName)
        val initialList = if (oldFile.exists()) {
            val defaultMeta = TimetableMetadata(id = "default", name = "My Timetable", lastModified = System.currentTimeMillis())
            val newFile = getDataFile("default")
            // Copy instead of rename to be safe, then delete maybe? Or just rename.
            oldFile.renameTo(newFile)
            listOf(defaultMeta)
        } else {
            val defaultMeta = TimetableMetadata(id = UUID.randomUUID().toString(), name = "My Timetable", lastModified = System.currentTimeMillis())
            // Create empty file
            val emptyData = TimetableData(emptyList(), emptyList())
            val root = TimetableJsonRoot(emptyList(), emptyList())
            getDataFile(defaultMeta.id).writeText(gson.toJson(root))
            listOf(defaultMeta)
        }
        
        saveIndex(initialList)
        return@withContext initialList
    }

    private suspend fun saveIndex(list: List<TimetableMetadata>) = withContext(Dispatchers.IO) {
        val file = getIndexFile()
        val json = gson.toJson(TimetableIndex(list))
        file.writeText(json)
    }

    override suspend fun createTimetable(name: String, startDate: Long, totalWeeks: Int): TimetableMetadata = withContext(Dispatchers.IO) {
        val meta = TimetableMetadata(
            id = UUID.randomUUID().toString(),
            name = name,
            lastModified = System.currentTimeMillis(),
            startDate = startDate,
            totalWeeks = totalWeeks
        )
        // Create empty file
        val root = TimetableJsonRoot(emptyList(), emptyList())
        getDataFile(meta.id).writeText(gson.toJson(root))

        val currentList = getAllTimetables().toMutableList()
        currentList.add(meta)
        saveIndex(currentList)
        
        return@withContext meta
    }

    override suspend fun updateTimetableMetadata(id: String, name: String, startDate: Long, totalWeeks: Int) = withContext(Dispatchers.IO) {
        val currentList = getAllTimetables().toMutableList()
        val index = currentList.indexOfFirst { it.id == id }
        if (index != -1) {
            val old = currentList[index]
            currentList[index] = old.copy(
                name = name, 
                startDate = startDate, 
                totalWeeks = totalWeeks,
                lastModified = System.currentTimeMillis()
            )
            saveIndex(currentList)
        }
    }

    override suspend fun loadTimetable(id: String): TimetableData = withContext(Dispatchers.IO) {
        val file = getDataFile(id)
        if (file.exists()) {
            try {
                val reader = file.reader()
                val root = gson.fromJson(reader, TimetableJsonRoot::class.java)
                reader.close()

                val domainCourses = root.courses.map {
                    CourseInfo(id = it.id, name = it.name, teacher = it.teacher, classroom = it.room, colorIndex = it.colorIndex)
                }
                val domainSessions = root.sessions.map {
                    CourseSession(
                        courseId = it.courseId,
                        day = it.dayOfWeek,
                        startSection = it.startNode,
                        endSection = it.startNode + it.length - 1,
                        weeks = it.weeks ?: (1..20).toList()
                    )
                }
                return@withContext TimetableData(domainCourses, domainSessions)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return@withContext TimetableData(emptyList(), emptyList())
    }

    override suspend fun saveTimetable(id: String, data: TimetableData) = withContext(Dispatchers.IO) {
        val file = getDataFile(id)
        val root = TimetableJsonRoot(
            courses = data.courses.map {
                CourseInfoJson(id = it.id, name = it.name, teacher = it.teacher, room = it.classroom, colorIndex = it.colorIndex)
            },
            sessions = data.sessions.map {
                CourseSessionJson(
                    courseId = it.courseId,
                    dayOfWeek = it.day,
                    startNode = it.startSection,
                    length = it.endSection + 1 - it.startSection, // length calculation
                    weeks = it.weeks
                )
            }
        )
        val json = gson.toJson(root)
        file.writeText(json)
        
        // Update last modified
        val metas = getAllTimetables().toMutableList()
        val index = metas.indexOfFirst { it.id == id }
        if (index != -1) {
            val old = metas[index]
            metas[index] = old.copy(lastModified = System.currentTimeMillis())
            saveIndex(metas)
        }
    }

    override suspend fun deleteTimetable(id: String) = withContext(Dispatchers.IO) {
        getDataFile(id).delete()
        val currentList = getAllTimetables().toMutableList()
        currentList.removeAll { it.id == id }
        saveIndex(currentList)
        Unit
    }
}
