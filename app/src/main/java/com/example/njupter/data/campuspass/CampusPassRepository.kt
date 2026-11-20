package com.example.njupter.data.campuspass

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

private const val BASE = "https://0trust-y.njupt.edu.cn:9458"
private val JSON_MEDIA_TYPE = "application/json;charset=UTF-8".toMediaType()

/**
 * 校园通行快照。onCampus: null=未知（接口缺字段）；inDorm: null=未返校（无通行记录）。
 */
data class CampusPassSnapshot(
    val onCampus: Boolean?,
    val inDorm: Boolean?,
    val lastAccessTime: String,
    val lastAccessPlace: String,
    val dormBuilding: String,
    val dormRoom: String
)

class CampusPassTokenRejectedException(message: String = "token rejected") : IOException(message)

class CampusPassRepository(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS)
        .build()
) {
    suspend fun fetch(token: CampusPassToken): CampusPassSnapshot = withContext(Dispatchers.IO) {
        val onCampus = fetchOnCampus(token)
        val record = fetchLatestRecord(token)
        CampusPassSnapshot(
            onCampus = onCampus,
            inDorm = record.inDorm,
            lastAccessTime = record.time,
            lastAccessPlace = record.place,
            dormBuilding = record.building,
            dormRoom = record.room
        )
    }

    private fun post(path: String, body: okhttp3.RequestBody, token: String): String {
        val request = Request.Builder().url("$BASE$path")
            .header("token", token)
            .post(body)
            .build()
        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            val responseBody = response.body ?: throw IOException("Empty response")
            val source = responseBody.source()
            if (source.request(262145)) throw IOException("Response too large")
            source.readUtf8()
        }
    }

    private fun parse(path: String, json: String): JsonObject =
        runCatching { JsonParser.parseString(json).asJsonObject }
            .getOrNull() ?: throw IOException("Malformed response from $path")

    /**
     * 网关无独立 errCode：msg 400 / data「请重新登录」即 token 失效。
     * 网关按 User-Agent 拦截非浏览器客户端（实测 okhttp 默认 UA 可过）。
     */
    private fun requireOk(root: JsonObject) {
        val msg = root.get("msg")?.takeIf { it.isJsonPrimitive }?.asString
        val data = root.get("data")?.takeIf { it.isJsonPrimitive }?.asString
        if (msg == "400" || data == "请重新登录") {
            throw CampusPassTokenRejectedException(data ?: "token rejected")
        }
        // selectStuType 的正常信封是 status:200；getstubytable 同样。
        val status = root.get("status")?.takeIf { it.isJsonPrimitive }?.asInt
        if (status != null && status != 200) throw IOException(msg ?: "请求失败 (status $status)")
    }

    private fun fetchOnCampus(token: CampusPassToken): Boolean? {
        val body = FormBody.Builder().add("personCode", token.studentId).build()
        val root = parse("/accessRecord/vx/selectStuState", post("/accessRecord/vx/selectStuState", body, token.raw))
        requireOk(root)
        val state = root.getAsJsonObject("data")?.get("state")?.takeIf { it.isJsonPrimitive }?.asInt
            ?: return null
        return when (state) {
            1 -> true
            0 -> false
            else -> null
        }
    }

    private data class LatestRecord(
        val inDorm: Boolean?,
        val time: String,
        val place: String,
        val building: String,
        val room: String
    )

    /**
     * getstubytable 是 JSON 体（multipart 会返回 Whitelabel Error Page）。
     * 解析失败按空记录处理，不影响主状态展示。
     */
    private fun fetchLatestRecord(token: CampusPassToken): LatestRecord = runCatching {
        val body = """{"page":0,"size":1,"yq":"","xh":"${token.studentId}"}""".toRequestBody(JSON_MEDIA_TYPE)
        val root = parse("/accessRecord/vx/getstubytable", post("/accessRecord/vx/getstubytable", body, token.raw))
        requireOk(root)
        val row = root.getAsJsonObject("data")
            ?.getAsJsonArray("result")
            ?.firstOrNull { it.isJsonObject }
            ?.asJsonObject
            ?: return@runCatching LatestRecord(null, "", "", "", "")
        LatestRecord(
            inDorm = when (row.get("inorout")?.takeIf { it.isJsonPrimitive }?.asInt) {
                1, 3 -> true
                null -> null
                else -> false
            },
            time = row.get("access_DATA")?.takeIf { it.isJsonPrimitive }?.asString.orEmpty(),
            place = row.get("yq")?.takeIf { it.isJsonPrimitive }?.asString.orEmpty(),
            building = row.get("lh")?.takeIf { it.isJsonPrimitive }?.asString.orEmpty(),
            room = row.get("fh")?.takeIf { it.isJsonPrimitive }?.asString.orEmpty()
        )
    }.getOrDefault(LatestRecord(null, "", "", "", ""))
}
