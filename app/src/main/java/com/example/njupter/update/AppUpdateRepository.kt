package com.example.njupter.update

import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.URI
import java.util.concurrent.TimeUnit

internal const val RELEASE_DOWNLOAD_PREFIX = "https://github.com/Tamako0401/NJUPTer/releases/download/"

data class AppUpdate(
    val versionCode: Int,
    val versionName: String,
    val notes: String,
    val apkUrl: String
)

internal fun isReleaseAssetUrl(url: String): Boolean = try {
    val uri = URI(url)
    url.startsWith(RELEASE_DOWNLOAD_PREFIX) && uri.host == "github.com" &&
        uri.scheme == "https" && uri.userInfo == null && uri.query == null && uri.fragment == null &&
        !uri.path.split('/').contains("..")
} catch (_: Exception) { false }

internal fun parseUpdateManifest(json: String, installedCode: Int, ignoredCode: Int): AppUpdate? {
    val root = JsonParser.parseString(json).asJsonObject
    val code = root.get("versionCode").asInt
    val name = root.get("versionName").asString
    val apk = root.get("apkUrl").asString
    if (code <= installedCode || code <= ignoredCode || name.isBlank() ||
        !isReleaseAssetUrl(apk) || !apk.endsWith(".apk")) return null
    return AppUpdate(code, name, root.get("notes")?.asString.orEmpty(), apk)
}

class AppUpdateRepository(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS)
        .build()
) {

    suspend fun check(installedCode: Int, ignoredCode: Int): AppUpdate? = withContext(Dispatchers.IO) {
        try {
            val release = JsonParser.parseString(readJson(
                "https://api.github.com/repos/Tamako0401/NJUPTer/releases/latest"
            )).asJsonObject
            if (release.get("draft").asBoolean || release.get("prerelease").asBoolean) return@withContext null
            val assets = release.getAsJsonArray("assets").map { it.asJsonObject }
            val manifest = assets.firstOrNull { it.get("name").asString == "update.json" }
                ?.get("browser_download_url")?.asString ?: return@withContext null
            if (!isReleaseAssetUrl(manifest)) return@withContext null
            val update = parseUpdateManifest(readJson(manifest), installedCode, ignoredCode)
                ?: return@withContext null
            // Offer only an APK actually attached to this successfully published release.
            update.takeIf { candidate -> assets.any {
                it.get("browser_download_url").asString == candidate.apkUrl &&
                    it.get("size").asLong > 0
            } }
        } catch (_: IOException) {
            null // Offline/rate limited: startup must remain usable.
        } catch (_: RuntimeException) {
            null // Invalid or older release metadata is not an update.
        }
    }

    private fun readJson(url: String): String {
        val request = Request.Builder().url(url)
            .header("Accept", "application/json")
            .header("User-Agent", "NJUPTer-Android")
            .build()
        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Update HTTP ${response.code}")
            val body = response.body ?: throw IOException("Empty update response")
            val source = body.source()
            if (source.request(262145)) throw IOException("Update response too large")
            source.readUtf8()
        }
    }
}
