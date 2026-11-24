package com.example.njupter.data.campuspass

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException

class CampusPassRepositoryTest {

    private val token = CampusPassToken(
        raw = "synthetic-campus-pass-token",
        studentId = "test-campus-student",
        expiresAtEpochSeconds = 1_700_010_800L
    )

    private val stuStateBody = """{"data":{"code":"${token.studentId}","state":1},"status":200,"msg":"success","time":"1700000000000"}"""
    private val stubyTableBody = """{"data":{"pagination":{"current":1,"pageSize":1,"total":1},"result":[{"yq":"虚构校区","xh":"${token.studentId}","inorout":1,"fh":"T-101","lh":"测试宿舍 A 栋","access_DATA":"2023-11-14 22:13:20"}]},"status":200,"msg":"success","time":"1700000000000"}"""
    private val emptyResultBody = """{"data":{"pagination":{"current":1,"pageSize":1,"total":0},"result":[]},"status":200,"msg":"success"}"""
    private val rejectedBody = """{"msg":"400","data":"请重新登录","time":"1700000000000"}"""

    private fun repository(bodyForPath: (String) -> String) = CampusPassRepository(
        OkHttpClient.Builder().addInterceptor { chain ->
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200)
                .message("OK").body(bodyForPath(chain.request().url.encodedPath).toResponseBody()).build()
        }.build()
    )

    @Test
    fun `fetch aggregates state and latest record`() = runBlocking {
        val snapshot = repository { path ->
            when (path) {
                "/accessRecord/vx/selectStuState" -> stuStateBody
                "/accessRecord/vx/getstubytable" -> stubyTableBody
                else -> error("unexpected path $path")
            }
        }.fetch(token)

        assertEquals(true, snapshot.onCampus)
        assertEquals(true, snapshot.inDorm)
        assertEquals("2023-11-14 22:13:20", snapshot.lastAccessTime)
        assertEquals("虚构校区", snapshot.lastAccessPlace)
        assertEquals("测试宿舍 A 栋", snapshot.dormBuilding)
        assertEquals("T-101", snapshot.dormRoom)
    }

    @Test
    fun `inorout 3 means in dorm per spa semantics`() = runBlocking {
        val body = stubyTableBody.replace("\"inorout\":1", "\"inorout\":3")
        val snapshot = repository { path ->
            if (path.endsWith("getstubytable")) body else stuStateBody
        }.fetch(token)
        assertEquals(true, snapshot.inDorm)
    }

    @Test
    fun `inorout 2 means not in dorm`() = runBlocking {
        val body = stubyTableBody.replace("\"inorout\":1", "\"inorout\":2")
        val snapshot = repository { path ->
            if (path.endsWith("getstubytable")) body else stuStateBody
        }.fetch(token)
        assertEquals(false, snapshot.inDorm)
    }

    @Test
    fun `empty result means not returned`() = runBlocking {
        val snapshot = repository { path ->
            if (path.endsWith("getstubytable")) emptyResultBody else stuStateBody
        }.fetch(token)
        assertNull(snapshot.inDorm)
        assertEquals("", snapshot.lastAccessTime)
        assertEquals("", snapshot.lastAccessPlace)
        assertEquals("", snapshot.dormBuilding)
        assertEquals("", snapshot.dormRoom)
    }

    @Test
    fun `state zero means off campus`() = runBlocking {
        val snapshot = repository { path ->
            if (path.endsWith("selectStuState")) stuStateBody.replace("\"state\":1", "\"state\":0")
            else stubyTableBody
        }.fetch(token)
        assertEquals(false, snapshot.onCampus)
    }

    @Test
    fun `token rejection when gateway says relogin`() = runBlocking {
        try {
            repository { rejectedBody }.fetch(token)
            fail("expected CampusPassTokenRejectedException")
        } catch (e: CampusPassTokenRejectedException) {
            assertTrue(e.message != null)
        }
    }

    @Test
    fun `requests carry token header and form body`() = runBlocking {
        var seenToken: String? = null
        var seenPersonCode: String? = null
        var seenXh: String? = null
        val repo = CampusPassRepository(
            OkHttpClient.Builder().addInterceptor { chain ->
                seenToken = chain.request().header("token")
                val body = chain.request().body
                if (chain.request().url.encodedPath.endsWith("selectStuState") && body != null) {
                    val buffer = okio.Buffer()
                    body.writeTo(buffer)
                    val text = buffer.readUtf8()
                    seenPersonCode = Regex("personCode=([^&]*)").find(text)?.groupValues?.get(1)
                }
                if (chain.request().url.encodedPath.endsWith("getstubytable") && body != null) {
                    val buffer = okio.Buffer()
                    body.writeTo(buffer)
                    seenXh = Regex("\"xh\":\"([^\"]*)\"").find(buffer.readUtf8())?.groupValues?.get(1)
                }
                Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200)
                    .message("OK").body(stuStateBody.toResponseBody()).build()
            }.build()
        )
        repo.fetch(token)
        assertEquals(token.raw, seenToken)
        assertEquals(token.studentId, seenPersonCode)
        assertEquals(token.studentId, seenXh)
    }

    @Test
    fun `malformed stubytable degrades to empty record but state still shown`() = runBlocking {
        val snapshot = repository { path ->
            when (path) {
                "/accessRecord/vx/selectStuState" -> stuStateBody
                "/accessRecord/vx/getstubytable" -> "not json"
                else -> error("unexpected path $path")
            }
        }.fetch(token)
        assertEquals(true, snapshot.onCampus)
        assertNull(snapshot.inDorm)
        assertEquals("", snapshot.lastAccessTime)
    }
}
