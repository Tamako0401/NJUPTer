package com.example.njupter.data.morningrun

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException

class MorningRunRepositoryTest {

    private val countBody = """{"success":true,"data":25}"""
    private val summaryBody = """{"success":true,"data":{"total":25,"face":{"time":"2026-11-18 07:26:10","location":"仙林终点3"}}}"""
    private val historyBody = """{"success":true,"data":[
        {"year":"2025-2026","term":"第一学期","count":30,"differ":0},
        {"year":"2025-2026","term":"第二学期","count":31,"differ":0}
    ]}"""
    private val recordBody = """{"success":true,"data":[
        {"date":"2026-11-11","start":"06:59:48","end":"07:08:30","departure":"打卡起点","destination":"打卡终点","status":0}
    ]}"""

    private fun repository(bodyForPath: (String) -> String) = MorningRunRepository(
        OkHttpClient.Builder().addInterceptor { chain ->
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200)
                .message("OK").body(bodyForPath(chain.request().url.encodedPath).toResponseBody()).build()
        }.build()
    )

    @Test
    fun `fetch aggregates count summary history and records`() = runBlocking {
        val snapshot = repository { path ->
            when (path) {
                "/mini_program/v4/fit/exercise/count" -> countBody
                "/mini_program/v4/fit/exercise/" -> summaryBody
                "/mini_program/v4/fit/exercise/history" -> historyBody
                "/mini_program/v4/fit/exercise/record" -> recordBody
                else -> error("unexpected path $path")
            }
        }.fetch("token-raw")

        assertEquals(25, snapshot.count)
        assertEquals(25, snapshot.total)
        assertEquals("仙林终点3", snapshot.today?.location)
        assertEquals("2026-11-18 07:26:10", snapshot.today?.time)
        assertEquals(2, snapshot.history.size)
        assertEquals(30, snapshot.history[0].count)
        assertEquals("第一学期", snapshot.history[0].term)
        assertEquals("2026-11-11", snapshot.records.first().date)
        assertEquals("06:59:48", snapshot.records.first().start)
    }

    @Test
    fun `requests carry the token header`() = runBlocking {
        var seenToken: String? = null
        val repo = MorningRunRepository(
            OkHttpClient.Builder().addInterceptor { chain ->
                seenToken = chain.request().header("token")
                Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200)
                    .message("OK").body(countBody.toResponseBody()).build()
            }.build()
        )
        repo.fetch("raw-token")
        assertEquals("raw-token", seenToken)
    }

    @Test
    fun `missing face yields null today and failed secondary endpoints degrade to empty lists`() = runBlocking {
        val snapshot = repository { path ->
            when (path) {
                "/mini_program/v4/fit/exercise/count" -> countBody
                "/mini_program/v4/fit/exercise/" -> """{"success":true,"data":{"total":25,"face":null}}"""
                "/mini_program/v4/fit/exercise/history" -> "not json"
                "/mini_program/v4/fit/exercise/record" -> "not json"
                else -> error("unexpected path $path")
            }
        }.fetch("token-raw")
        assertNull(snapshot.today)
        assertTrue(snapshot.history.isEmpty())
        assertTrue(snapshot.records.isEmpty())
    }

    @Test
    fun `rejected token throws TokenRejectedException`() = runBlocking {
        val rejected = repository { """{"success":false,"errMsg":"token错误","errCode":5004,"data":null}""" }
        try {
            rejected.fetch("token-raw")
            fail("expected TokenRejectedException")
        } catch (e: TokenRejectedException) {
            assertEquals("token错误", e.message)
        }
    }

    @Test
    fun `other business failures throw IOException with server message`() = runBlocking {
        val failing = repository { """{"success":false,"errMsg":"服务器开小差","errCode":1,"data":null}""" }
        try {
            failing.fetch("token-raw")
            fail("expected IOException")
        } catch (e: IOException) {
            assertEquals("服务器开小差", e.message)
        }
    }

    @Test
    fun `count endpoint non-integer data fails the fetch`() = runBlocking {
        val malformed = repository { path ->
            if (path.endsWith("/fit/exercise/count")) """{"success":true,"data":"not-a-number"}""" else countBody
        }
        try {
            malformed.fetch("token-raw")
            fail("expected IOException")
        } catch (e: IOException) {
            assertTrue(e.message!!.isNotBlank())
        }
    }
}
