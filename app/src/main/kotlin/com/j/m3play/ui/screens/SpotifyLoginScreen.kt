package com.j.m3play.ui.screens

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.j.m3play.spotify.SpotifyAuth

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SpotifyLoginScreen(
    onCookieFound: (String) -> Unit
) {
    // Ye variable check karega ki ek baar cookie milne par baar-baar function call na ho
    var cookieFetched by remember { mutableStateOf(false) }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true

                // Purani cookies clear karna taaki hamesha fresh login aayega
                CookieManager.getInstance().removeAllCookies(null)
                CookieManager.getInstance().flush()

                webViewClient = object : WebViewClient() {
                    override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                        super.doUpdateVisitedHistory(view, url, isReload)
                        
                        // Har naye page load par Cookie manager check karega
                        val cookies = CookieManager.getInstance().getCookie(url)
                        if (cookies != null && cookies.contains("sp_dc=") && !cookieFetched) {
                            
                            // 'sp_dc=' wali cookie extract karna
                            val spDc = cookies.split(";").map { it.trim() }
                                .firstOrNull { it.startsWith("sp_dc=") }
                                ?.substringAfter("sp_dc=")
                                
                            if (!spDc.isNullOrEmpty()) {
                                cookieFetched = true
                                onCookieFound(spDc) // Cookie milte hi UI ko bhej diya!
                            }
                        }
                    }
                }
                
                // Asli Spotify Login Page load karega
                loadUrl(SpotifyAuth.LOGIN_URL)
            }
        }
    )
}
