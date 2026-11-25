package com.example.njupter.data.morningrun

import com.google.gson.Gson
import com.google.gson.JsonParser
import java.nio.charset.StandardCharsets
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

data class MiniProgramToken(
    val raw: String,
    val studentId: String,
    val name: String,
    val department: String,
    val expiresAtEpochSeconds: Long
)

object MiniProgramTokenParser {
    private val JWT = Regex("eyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+")

    private data class TokenUser(val name: String?, val studentId: String?, val department: String?)

    fun parse(input: String): MiniProgramToken? {
        val raw = JWT.find(input)?.value ?: return null
        val payloadJson = runCatching {
            String(
                Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT).decode(raw.split('.')[1]),
                StandardCharsets.UTF_8
            )
        }.getOrNull() ?: return null
        val root = runCatching { JsonParser.parseString(payloadJson).asJsonObject }.getOrNull() ?: return null
        val exp = runCatching { root.get("exp")?.asLong }.getOrNull() ?: return null
        val info = runCatching { root.get("userInfo")?.asString }.getOrNull() ?: return null
        val user = runCatching { Gson().fromJson(info, TokenUser::class.java) }.getOrNull() ?: return null
        if (user.studentId.isNullOrBlank()) return null
        return MiniProgramToken(raw, user.studentId, user.name.orEmpty(), user.department.orEmpty(), exp)
    }
}
