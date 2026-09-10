package com.example.njupter.update

import org.junit.Assert.*
import org.junit.Test
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.io.IOException

class AppUpdateRepositoryTest {
    private fun manifest(code: Int, url: String = "${RELEASE_DOWNLOAD_PREFIX}main-100/NJUPTer.apk") =
        """{"versionCode":$code,"versionName":"1.1.0","notes":"Changes","apkUrl":"$url"}"""

    @Test fun `newer installable release is offered`() {
        assertEquals(101, parseUpdateManifest(manifest(101), 100, 0)?.versionCode)
    }

    @Test fun `same version older releases and ignored versions stay silent`() {
        assertNull(parseUpdateManifest(manifest(100), 100, 0))
        assertNull(parseUpdateManifest(manifest(99), 100, 0))
        assertNull(parseUpdateManifest(manifest(101), 100, 101))
        assertEquals(102, parseUpdateManifest(manifest(102), 100, 101)?.versionCode)
    }

    @Test fun `downloads must be APKs in this repository release assets`() {
        listOf("http://github.com/Tamako0401/NJUPTer/releases/download/a/app.apk",
            "https://github.com.evil.test/Tamako0401/NJUPTer/releases/download/a/app.apk",
            "https://github.com/other/repo/releases/download/a/app.apk",
            "${RELEASE_DOWNLOAD_PREFIX}a/update.json").forEach {
            assertNull(parseUpdateManifest(manifest(101, it), 100, 0))
        }
    }

    private fun repository(release: String, metadata: String = manifest(101)) = AppUpdateRepository(
        OkHttpClient.Builder().addInterceptor { chain ->
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200)
                .message("OK").body((if (chain.request().url.host == "api.github.com") release else metadata).toResponseBody()).build()
        }.build()
    )

    private fun release(includeApk: Boolean = true): String {
        val assets = mutableListOf("""{"name":"update.json","browser_download_url":"${RELEASE_DOWNLOAD_PREFIX}main-100/update.json","size":100}""")
        if (includeApk) assets += """{"name":"NJUPTer.apk","browser_download_url":"${RELEASE_DOWNLOAD_PREFIX}main-100/NJUPTer.apk","size":12345}"""
        return """{"draft":false,"prerelease":false,"assets":[${assets.joinToString(",") }]}"""
    }

    @Test fun `published manifest and matching attached APK produce an update`() = runBlocking {
        assertEquals(101, repository(release()).check(100, 0)?.versionCode)
    }

    @Test fun `missing APK old release and malformed metadata stay silent`() = runBlocking {
        assertNull(repository(release(false)).check(100, 0))
        assertNull(repository("""{"draft":false,"prerelease":false,"assets":[]}""").check(100, 0))
        assertNull(repository(release(), "not json").check(100, 0))
    }

    @Test fun `offline and HTTP rate limit never interrupt startup`() = runBlocking {
        val offline = OkHttpClient.Builder().addInterceptor { throw IOException("offline") }.build()
        assertNull(AppUpdateRepository(offline).check(100, 0))
        val limited = OkHttpClient.Builder().addInterceptor { chain ->
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(403)
                .message("Rate limited").body("{}".toResponseBody()).build()
        }.build()
        assertNull(AppUpdateRepository(limited).check(100, 0))
    }
}
