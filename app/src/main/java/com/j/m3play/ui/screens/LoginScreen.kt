package com.j.m3play.ui.screens

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.arturo254.innertube.YouTube
import com.arturo254.innertube.utils.parseCookieString
import com.j.m3play.LocalPlayerAwareWindowInsets
import com.j.m3play.R
import com.j.m3play.constants.AccountChannelHandleKey
import com.j.m3play.constants.AccountEmailKey
import com.j.m3play.constants.AccountNameKey
import com.j.m3play.constants.InnerTubeCookieKey
import com.j.m3play.constants.VisitorDataKey
import com.j.m3play.ui.component.IconButton
import com.j.m3play.ui.utils.backToMain
import com.j.m3play.utils.rememberPreference
import com.j.m3play.utils.reportException
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

private const val YOUTUBE_MUSIC_URL = "https://music.youtube.com"
private const val MAX_RETRY_ATTEMPTS = 3
private const val RETRY_DELAY_MS = 1000L

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class, DelicateCoroutinesApi::class)
@Composable
fun LoginScreen(navController: NavController) {
    var visitorData by rememberPreference(VisitorDataKey, "")
    var innerTubeCookie by rememberPreference(InnerTubeCookieKey, "")
    var accountName by rememberPreference(AccountNameKey, "")
    var accountEmail by rememberPreference(AccountEmailKey, "")
    var accountChannelHandle by rememberPreference(AccountChannelHandleKey, "")

    var webView: WebView? = null
    var isLoadingAccountInfo by remember { mutableStateOf(false) }

    suspend fun fetchAccountInfoWithRetry(retryCount: Int = 0) {
        try {
            YouTube.accountInfo().onSuccess { accountInfo ->
                // Verificar que la información no esté vacía
                val name = accountInfo.name.takeIf { it.isNotBlank() } ?: ""
                val email = accountInfo.email?.takeIf { it.isNotBlank() } ?: ""
                val handle = accountInfo.channelHandle?.takeIf { it.isNotBlank() } ?: ""

                // Solo actualizar si tenemos al menos el nombre
                if (name.isNotEmpty()) {
                    accountName = name
                    accountEmail = email
                    accountChannelHandle = handle

                    Timber.tag("WebView")
                        .d("Account info retrieved successfully: $name, $email, $handle")
                    isLoadingAccountInfo = false
                } else {
                    // Si el nombre está vacío, reintentar
                    if (retryCount < MAX_RETRY_ATTEMPTS) {
                        Timber.tag("WebView")
                            .w("Account name is empty, retrying... Attempt ${retryCount + 1}")
                        delay(RETRY_DELAY_MS)
                        fetchAccountInfoWithRetry(retryCount + 1)
                    } else {
                        Timber.tag("WebView")
                            .e("Failed to retrieve account name after $MAX_RETRY_ATTEMPTS attempts")
                        isLoadingAccountInfo = false
                    }
                }
            }.onFailure { exception ->
                if (retryCount < MAX_RETRY_ATTEMPTS) {
                    Timber.tag("WebView")
                        .w("Failed to retrieve account info, retrying... Attempt ${retryCount + 1}: ${exception.message}")
                    delay(RETRY_DELAY_MS)
                    fetchAccountInfoWithRetry(retryCount + 1)
                } else {
                    reportException(exception)
                    Timber.tag("WebView")
                        .e(
                            exception,
                            "Failed to retrieve account info after $MAX_RETRY_ATTEMPTS attempts"
                        )
                    isLoadingAccountInfo = false
                }
            }
        } catch (e: Exception) {
            reportException(e)
            Timber.tag("WebView").e(e, "Unexpected error while fetching account info")
            isLoadingAccountInfo = false
        }
    }

    AndroidView(
        modifier =
            Modifier
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
                .fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(
                        view: WebView,
                        url: String,
                        favicon: android.graphics.Bitmap?
                    ) {
//                        Timber.tag("WebView").d("Page started: $url") // Uncomment this line to debug WebView
                        super.onPageStarted(view, url, favicon)
                    }

                    override fun onPageFinished(view: WebView, url: String?) {
                        Timber.tag("WebView").d("Page finished: $url")
                        if (url != null && url.startsWith(YOUTUBE_MUSIC_URL)) {
                            val youTubeCookieString = CookieManager.getInstance().getCookie(url)
                            val parsedCookies = parseCookieString(youTubeCookieString)

                            if ("SAPISID" in parsedCookies) {
                                innerTubeCookie = youTubeCookieString
                                isLoadingAccountInfo = true

                                GlobalScope.launch {
                                    // Pequeña espera para asegurar que las cookies se establezcan correctamente
                                    delay(500)
                                    fetchAccountInfoWithRetry()
                                }

                                // Obtener visitor data
                                loadUrl("javascript:Android.onRetrieveVisitorData(window.yt.config_.VISITOR_DATA)")
                            } else {
                                innerTubeCookie = ""
                                Timber.tag("WebView").e("SAPISID not found in cookies")
                            }
                        }
                    }

                    override fun shouldOverrideUrlLoading(
                        view: WebView,
                        request: WebResourceRequest
                    ): Boolean {
                        val url = request.url.toString()
                        Timber.tag("WebView").d("Loading URL: $url")
                        return super.shouldOverrideUrlLoading(view, request)
                    }
                }
                settings.apply {
                    javaScriptEnabled = true
                    setSupportZoom(true)
                    builtInZoomControls = true
                }
                CookieManager.getInstance().setAcceptCookie(true)
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                addJavascriptInterface(
                    object {
                        @JavascriptInterface
                        fun onRetrieveVisitorData(newVisitorData: String?) {
                            if (innerTubeCookie.isEmpty()) {
                                visitorData = ""
                                Timber.tag("WebView")
                                    .e("InnerTube cookie is empty, cannot retrieve visitor data")
                                return
                            }

                            if (!newVisitorData.isNullOrBlank()) {
                                visitorData = newVisitorData
                                Timber.tag("WebView").d("Visitor data retrieved: $visitorData")
                            } else {
                                Timber.tag("WebView").w("Visitor data is null or blank")
                            }
                        }
                    },
                    "Android",
                )
                webView = this
                loadUrl(
                    "https://accounts.google.com/ServiceLogin?ltmpl=music&service=youtube&passive=true&continue=$YOUTUBE_MUSIC_URL",
                )
            }
        },
    )

    TopAppBar(
        title = {
            Text(
                if (isLoadingAccountInfo) {
                    stringResource(R.string.login) + " - Loading..."
                } else {
                    stringResource(R.string.login)
                }
            )
        },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null,
                )
            }
        },
    )

    BackHandler(enabled = webView?.canGoBack() == true) {
        webView?.goBack()
    }
}