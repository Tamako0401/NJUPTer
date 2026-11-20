package com.example.njupter.data.campuspass

import com.google.gson.JsonParser
import java.nio.charset.StandardCharsets
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * 零信任「校园通行」的 cdr JWT：payload 直接是 {"iat","exp","no","iss"}，
 * 与晨跑小程序 token（userInfo 为 JSON 字符串）结构不同，故独立解析。
 */
data class CampusPassToken(
    val raw: String,
    val studentId: String,
    val expiresAtEpochSeconds: Long
)

object CampusPassTokenParser {
    private val JWT = Regex("eyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+")

    @OptIn(ExperimentalEncodingApi::class)
    private val URL_SAFE = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT)

    fun parse(input: String): CampusPassToken? {
        val raw = JWT.find(input)?.value ?: return null
        val payloadJson = runCatching {
            String(URL_SAFE.decode(raw.split('.')[1]), StandardCharsets.UTF_8)
        }.getOrNull() ?: return null
        val root = runCatching { JsonParser.parseString(payloadJson).asJsonObject }.getOrNull() ?: return null
        val exp = runCatching { root.get("exp")?.asLong }.getOrNull() ?: return null
        val no = runCatching { root.get("no")?.asString }.getOrNull() ?: return null
        if (no.isBlank()) return null
        return CampusPassToken(raw, no, exp)
    }
}
