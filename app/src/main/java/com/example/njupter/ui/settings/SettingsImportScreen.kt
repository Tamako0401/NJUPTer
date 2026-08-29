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
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.njupter.R
import com.example.njupter.data.import.JwxtEndpoints
import com.example.njupter.ui.theme.NJUPTerTheme
import java.net.URI

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun JwxtImportScreen(
    isActive: Boolean = true,
    onBack: () -> Unit,
    onCookiesObtained: (String) -> Unit
) {
    val defaultTitle = stringResource(R.string.jwxt_login_title)
    var title by remember(defaultTitle) { mutableStateOf(defaultTitle) }

    // 为了防止多次触发成功回调
    var isSuccess by remember { mutableStateOf(false) }
    var hasRequestedTimetable by remember { mutableStateOf(false) }

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
            if (LocalInspectionMode.current) {
                Text(
                    text = "WebView is not available in Preview",
                    modifier = Modifier.padding(16.dp)
                )
            } else {
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

                                    url?.let { currentUrl ->
                                        val uri = runCatching { URI(currentUrl) }.getOrNull()
                                        val isNewJwxt = uri?.host.equals(
                                            "jwglxt.njupt.edu.cn",
                                            ignoreCase = true
                                        )
                                        val isTimetable = isNewJwxt &&
                                            uri?.path == JwxtEndpoints.TIMETABLE_PATH

                                        if (isTimetable && !isSuccess) {
                                            val cookies = cookieManager.getCookie(
                                                JwxtEndpoints.TIMETABLE_URL
                                            )
                                            cookieManager.flush()
                                            isSuccess = true
                                            onCookiesObtained(cookies ?: "")
                                        } else if (
                                            isNewJwxt &&
                                            uri?.path != "/sso/ddlogin" &&
                                            !hasRequestedTimetable
                                        ) {
                                            hasRequestedTimetable = true
                                            view?.loadUrl(JwxtEndpoints.TIMETABLE_URL)
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

                            loadUrl(JwxtEndpoints.LOGIN_URL)
                        }
                    },
                    update = { webView ->
                        if (isActive) webView.onResume() else webView.onPause()
                    },
                    onRelease = { webView ->
                        webView.stopLoading()
                        webView.destroy()
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun JwxtImportScreenPreview() {
    NJUPTerTheme {
        JwxtImportScreen(
            onBack = {},
            onCookiesObtained = { _ -> }
        )
    }
}
