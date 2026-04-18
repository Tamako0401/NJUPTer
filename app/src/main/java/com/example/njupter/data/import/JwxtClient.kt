package com.example.njupter.data.import

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * 负责从教务系统抓取网页的 HTTP 客户端
 */
class JwxtClient(private val cookieString: String, private val xh: String) {
    private val client = OkHttpClient.Builder().build()
    
    suspend fun fetchTimetableHtml(): String = withContext(Dispatchers.IO) {
        // 构建包含选课学号的课表 URL
        val timetableUrl = "http://jwxt.njupt.edu.cn/xskbcx.aspx?xh=$xh&gnmkdm=N121603"

        // 构建请求并注入从 WebView 拿到的 Cookie
        val request = Request.Builder()
            .url(timetableUrl)
            .addHeader("Cookie", cookieString)
            // 查课表必须要 Referer，否则会Object moved to here (而且 href='')
            .addHeader("Referer", "http://jwxt.njupt.edu.cn/xs_main.aspx?xh=$xh")
            .build()
        
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("Failed to fetch timetable, status code: ${response.code}")
            }
            
            val bytes = response.body?.bytes() ?: throw Exception("Empty response body")
            String(bytes, charset("GBK"))
        }
    }
}
