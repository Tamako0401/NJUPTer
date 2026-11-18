package com.example.njupter.data.morningrun

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

private const val BASE = "https://wechat.njupt.edu.cn/mini_program/v4"

data class SemesterCount(val year: String, val term: String, val count: Int, val differ: Int)
data class PunchRecord(
    val date: String,
    val start: String,
    val end: String,
    val departure: String,
    val destination: String
)

data class PunchFace(val time: String, val location: String)

data class MorningRunSnapshot(
    val count: Int,
    val total: Int,
    val history: List<SemesterCount>,
    val records: List<PunchRecord>,
    val today: PunchFace?
)
class TokenRejectedException(message: String = "token rejected") : IOException(message)

class MorningRunRepository(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS)
        .build()
) {
    suspend fun fetch(token: String): MorningRunSnapshot = withContext(Dispatchers.IO) {
        val count = fetchCount(token)
        val summary = fetchSummary(token)
        val history = fetchHistory(token)
        val records = fetchRecords(token)
        MorningRunSnapshot(
            count = count,
            total = summary.total,
            history = history,
            records = records,
            today = summary.face
        )
    }

    private fun get(path: String, token: String): String {
        val request = Request.Builder().url("$BASE$path")
            .header("token", token)
            .header("Accept", "application/json")
            .build()
        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            val body = response.body ?: throw IOException("Empty response")
            val source = body.source()
            if (source.request(262145)) throw IOException("Response too large")
            source.readUtf8()
        }
    }

    private fun parse(path: String, token: String): JsonObject {
        val json = get(path, token)
        return runCatching { JsonParser.parseString(json).asJsonObject }
            .getOrNull() ?: throw IOException("Malformed response from $path")
    }

    /** Envelope check shared by every endpoint: HTTP is always 200, business failure lives in the body. */
    private fun requireSuccess(root: JsonObject) {
        if (root.get("success")?.takeIf { it.isJsonPrimitive }?.asBoolean == false) {
            val errCode = root.get("errCode")?.takeIf { it.isJsonPrimitive }?.asInt
            val errMsg = root.get("errMsg")?.takeIf { it.isJsonPrimitive }?.asString
            if (errCode == 5004) throw TokenRejectedException(errMsg ?: "token rejected")
            throw IOException(errMsg ?: "请求失败 (errCode $errCode)")
        }
    }

    private fun fetchCount(token: String): Int {
        val root = parse("/fit/exercise/count", token)
        requireSuccess(root)
        val data = root.get("data") ?: throw IOException("count: missing data")
        if (!data.isJsonPrimitive) throw IOException("count: unexpected data type")
        return runCatching { data.asInt }.getOrElse { throw IOException("count: invalid number") }
    }

    private data class Summary(val total: Int, val face: PunchFace?)

    private fun fetchSummary(token: String): Summary {
        // Secondary endpoint: parse failure degrades to defaults, never blocks the main count.
        return runCatching {
            val root = parse("/fit/exercise/", token)
            requireSuccess(root)
            val data = root.get("data")?.takeIf { it.isJsonObject }?.asJsonObject
                ?: return@runCatching Summary(0, null)
            val total = data.get("total")?.takeIf { it.isJsonPrimitive }?.asInt ?: 0
            val face = data.get("face")?.takeIf { it.isJsonObject }?.asJsonObject?.let { faceObj ->
                PunchFace(
                    time = faceObj.get("time")?.takeIf { p -> p.isJsonPrimitive }?.asString.orEmpty(),
                    location = faceObj.get("location")?.takeIf { p -> p.isJsonPrimitive }?.asString.orEmpty()
                )
            }
            Summary(total, face)
        }.getOrDefault(Summary(0, null))
    }

    private fun fetchHistory(token: String): List<SemesterCount> {
        // Secondary endpoint: parse failure degrades to empty, never blocks the main count.
        return runCatching {
            val root = parse("/fit/exercise/history", token)
            requireSuccess(root)
            val data = root.get("data")?.takeIf { it.isJsonArray }?.asJsonArray ?: return@runCatching emptyList()
            data.filter { it.isJsonObject }.map { it.asJsonObject }.map { obj ->
                SemesterCount(
                    year = obj.get("year")?.takeIf { it.isJsonPrimitive }?.asString.orEmpty(),
                    term = obj.get("term")?.takeIf { it.isJsonPrimitive }?.asString.orEmpty(),
                    count = obj.get("count")?.takeIf { it.isJsonPrimitive }?.asInt ?: 0,
                    differ = obj.get("differ")?.takeIf { it.isJsonPrimitive }?.asInt ?: 0
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun fetchRecords(token: String): List<PunchRecord> {
        // Secondary endpoint: parse failure degrades to empty, never blocks the main count.
        return runCatching {
            val root = parse("/fit/exercise/record", token)
            requireSuccess(root)
            val data = root.get("data")?.takeIf { it.isJsonArray }?.asJsonArray ?: return@runCatching emptyList()
            data.filter { it.isJsonObject }.map { it.asJsonObject }.map { obj ->
                PunchRecord(
                    date = obj.get("date")?.takeIf { it.isJsonPrimitive }?.asString.orEmpty(),
                    start = obj.get("start")?.takeIf { it.isJsonPrimitive }?.asString.orEmpty(),
                    end = obj.get("end")?.takeIf { it.isJsonPrimitive }?.asString.orEmpty(),
                    departure = obj.get("departure")?.takeIf { it.isJsonPrimitive }?.asString.orEmpty(),
                    destination = obj.get("destination")?.takeIf { it.isJsonPrimitive }?.asString.orEmpty()
                )
            }
        }.getOrDefault(emptyList())
    }
}
