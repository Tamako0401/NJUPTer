package com.example.njupter.data.morningrun

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.nio.charset.StandardCharsets
import kotlin.io.encoding.Base64

class MiniProgramTokenParserTest {

    companion object {
        // Mirrors the captured mini-program token: payload {"userInfo":"<JSON string>","exp":<seconds>}.
        // userInfo carries {name, studentId, department, openId, role[]}.
        private const val STUDENT_ID = "B25091308"
        private const val NAME = "卜羿涵"
        private const val DEPARTMENT = "计算机学院"
        private const val EXP = 1795018971L

        private fun b64(bytes: ByteArray): String =
            Base64.UrlSafe.encode(bytes).trimEnd('=')

        private fun jwt(exp: Long = EXP): String {
            val header = b64("""{"alg":"HS256","typ":"JWT"}""".toByteArray(StandardCharsets.UTF_8))
            val user = Gson().toJson(
                mapOf(
                    "name" to NAME,
                    "studentId" to STUDENT_ID,
                    "department" to DEPARTMENT,
                    "openId" to "oXyz",
                    "role" to listOf("student")
                )
            )
            val escaped = Gson().toJson(user) // userInfo is a JSON *string* inside the payload
            val payload = b64(
                """{"userInfo":$escaped,"exp":$exp}""".toByteArray(StandardCharsets.UTF_8)
            )
            val signature = b64("sig-sig-sig".toByteArray(StandardCharsets.UTF_8))
            return "$header.$payload.$signature"
        }
    }

    @Test
    fun `parses full token and extracts identity and expiry`() {
        val token = MiniProgramTokenParser.parse(jwt())!!
        assertEquals(jwt(), token.raw)
        assertEquals(STUDENT_ID, token.studentId)
        assertEquals(NAME, token.name)
        assertEquals(DEPARTMENT, token.department)
        assertEquals(EXP, token.expiresAtEpochSeconds)
    }

    @Test
    fun `extracts token embedded in larger text`() {
        val raw = jwt()
        val token = MiniProgramTokenParser.parse("""{"token":"$raw","extra":"noise"}""")!!
        assertEquals(raw, token.raw)
        assertEquals(STUDENT_ID, token.studentId)
        assertEquals(NAME, token.name)
    }

    @Test
    fun `rejects non token input`() {
        assertNull(MiniProgramTokenParser.parse("abc"))
        assertNull(MiniProgramTokenParser.parse(""))
    }

    @Test
    fun `rejects malformed segments`() {
        assertNull(MiniProgramTokenParser.parse("eyJ.eyJ."))
        // Three well-formed segments whose middle does not decode to JSON.
        val bad = "eyJh.eyJi.eyJj"
        assertNull(MiniProgramTokenParser.parse(bad))
    }

    @Test
    fun `rejects token without exp or studentId`() {
        val header = b64("""{"alg":"HS256","typ":"JWT"}""".toByteArray(StandardCharsets.UTF_8))
        val noExp = b64("""{"userInfo":"{\"studentId\":\"B1\"}"}""".toByteArray(StandardCharsets.UTF_8))
        assertNull(MiniProgramTokenParser.parse("$header.$noExp.eyJz"))
        val noStudent = b64("""{"userInfo":"{\"name\":\"x\"}","exp":1795018971}""".toByteArray(StandardCharsets.UTF_8))
        assertNull(MiniProgramTokenParser.parse("$header.$noStudent.eyJz"))
    }

    @Test
    fun `parses payload whose base64 length is 2 mod 4 without padding`() {
        // kotlin.io Base64 defaults to PaddingOption.PRESENT; JWT segments carry no '='.
        // A payload 2 mod 4 (two missing pad chars) is the case that default rejects.
        val header = b64("""{"alg":"HS256","typ":"JWT"}""".toByteArray(StandardCharsets.UTF_8))
        // Full captured-shape userInfo (name/studentId/department/openId/role) so the
        // payload bytes % 3 == 1 → base64 len % 4 == 2, exactly like the real token.
        val user = Gson().toJson(
            mapOf(
                "name" to NAME,
                "studentId" to STUDENT_ID,
                "department" to DEPARTMENT,
                "openId" to "oTest",
                "role" to listOf("student")
            )
        )
        val escaped = Gson().toJson(user)
        val payloadJson = """{"userInfo":$escaped,"exp":$EXP}"""
        val unpadded = Base64.UrlSafe.withPadding(kotlin.io.encoding.Base64.PaddingOption.ABSENT)
            .encode(payloadJson.toByteArray(StandardCharsets.UTF_8))
        check(unpadded.length % 4 == 2) { "test needs payload len%4==2, got ${unpadded.length}" }
        val sig = b64("sig".toByteArray(StandardCharsets.UTF_8))
        val token = MiniProgramTokenParser.parse("$header.$unpadded.$sig")!!
        assertEquals(STUDENT_ID, token.studentId)
        assertEquals(NAME, token.name)
        assertEquals(EXP, token.expiresAtEpochSeconds)
    }
}
