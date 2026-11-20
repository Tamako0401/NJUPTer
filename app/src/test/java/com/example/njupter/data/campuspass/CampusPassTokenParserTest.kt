package com.example.njupter.data.campuspass

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.nio.charset.StandardCharsets
import kotlin.io.encoding.Base64

class CampusPassTokenParserTest {

    companion object {
        private const val STUDENT_ID = "B25091308"
        private const val IAT = 1795097703L
        private const val EXP = 1795108503L

        private fun b64u(bytes: ByteArray): String =
            Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT).encode(bytes)

        private fun jwt(payloadJson: String): String {
            val header = b64u("""{"alg":"HS256","typ":"JWT"}""".toByteArray(StandardCharsets.UTF_8))
            val payload = b64u(payloadJson.toByteArray(StandardCharsets.UTF_8))
            val signature = b64u("sig-sig-sig".toByteArray(StandardCharsets.UTF_8))
            return "$header.$payload.$signature"
        }

        private val realShapePayload =
            """{"iat":$IAT,"exp":$EXP,"no":"$STUDENT_ID","iss":"cdr"}"""
    }

    @Test
    fun `parses full cdr token`() {
        val raw = jwt(realShapePayload)
        val token = CampusPassTokenParser.parse(raw)!!
        assertEquals(raw, token.raw)
        assertEquals(STUDENT_ID, token.studentId)
        assertEquals(EXP, token.expiresAtEpochSeconds)
    }

    @Test
    fun `extracts token from full vxLogin response json`() {
        val raw = jwt(realShapePayload)
        val response = """{"status":200,"msg":"success","data":{"id":"$STUDENT_ID","token":"$raw"},"time":"1795097703850"}"""
        val token = CampusPassTokenParser.parse(response)!!
        assertEquals(raw, token.raw)
        assertEquals(STUDENT_ID, token.studentId)
    }

    @Test
    fun `rejects token without student number`() {
        val raw = jwt("""{"iat":$IAT,"exp":$EXP,"iss":"cdr"}""")
        assertNull(CampusPassTokenParser.parse(raw))
    }

    @Test
    fun `rejects non token input`() {
        assertNull(CampusPassTokenParser.parse("abc"))
        assertNull(CampusPassTokenParser.parse(""))
    }
}
