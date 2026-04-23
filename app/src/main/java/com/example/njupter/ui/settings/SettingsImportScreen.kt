package com.example.njupter.ui.settings

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import com.example.njupter.R

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun JwxtImportScreen(
    onBack: () -> Unit,
    onCookiesObtained: (String, String) -> Unit
) {
    val defaultTitle = stringResource(R.string.jwxt_login_title)
    var title by remember(defaultTitle) { mutableStateOf(defaultTitle) }

    // 为了防止多次触发成功回调
    var isSuccess by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true

                        // 需要支持 Cookie
                        val cookieManager = CookieManager.getInstance()
                        cookieManager.setAcceptCookie(true)
                        cookieManager.setAcceptThirdPartyCookies(this, true)

                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                view?.title?.let { title = it }

                                // 检查当前 URL 域名的 Cookie
                                url?.let { currentUrl ->
                                    val cookies = cookieManager.getCookie(currentUrl)
                                    // 获取登录后重定向的主页面链接，提取学号 xh
                                    val xhMatch = Regex("xh=([A-Za-z0-9]+)").find(currentUrl)
                                    val xh = xhMatch?.groupValues?.get(1)

                                    if (currentUrl.contains("jwxt.njupt.edu.cn") && xh != null && !isSuccess) {
                                        isSuccess = true
                                        onCookiesObtained(cookies ?: "", xh)
                                    }
                                }
                            }

                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                return super.shouldOverrideUrlLoading(view, request)
                            }
                        }

                        // 会重定向到统一身份认证
                        loadUrl("http://jwxt.njupt.edu.cn/login_cas.aspx")
                    }
                }
            )
        }
    }
}
