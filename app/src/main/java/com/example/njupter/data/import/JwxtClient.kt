package com.example.njupter.data.import

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.nio.charset.StandardCharsets

object JwxtEndpoints {
    const val LOGIN_URL = "http://jwglxt.njupt.edu.cn/sso/ddlogin"
    const val TIMETABLE_URL =
        "http://jwglxt.njupt.edu.cn/kbcx/xskbcx_cxXskbcxIndex.html?gnmkdm=N2151&layout=default"
    const val TIMETABLE_PATH = "/kbcx/xskbcx_cxXskbcxIndex.html"

    internal const val REFERER_URL =
        "http://jwglxt.njupt.edu.cn/xtgl/index_initMenu.html?jsdm=xs"
}

/**
 * 负责从教务系统抓取网页的 HTTP 客户端
 */
class JwxtClient(private val cookieString: String) {
    private val client = OkHttpClient.Builder().build()
    
    suspend fun fetchTimetableHtml(): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(JwxtEndpoints.TIMETABLE_URL)
            .addHeader("Cookie", cookieString)
            .addHeader("Referer", JwxtEndpoints.REFERER_URL)
            .build()
        
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("课表页面请求失败（HTTP ${response.code}）")
            }
            
            val bytes = response.body?.bytes() ?: throw Exception("课表页面返回了空内容")
            String(bytes, StandardCharsets.UTF_8)
        }
    }
}
