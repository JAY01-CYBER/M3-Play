package com.zemer.cipher

import android.content.Context
import okhttp3.OkHttpClient
import java.net.Proxy

/**
 * Configuration and initialization for the Zemer Cipher library.
 */
object ZemerCipher {
    /**
     * Optional proxy for network requests.
     */
    var proxy: Proxy? = null

    /**
     * Enable verbose debug logging.
     */
    var debugLogging: Boolean = false

    @Volatile
    private var cachedHttpClient: OkHttpClient? = null

    @Volatile
    private var cachedHttpClientProxy: Proxy? = null

    /**
     * The ONE OkHttpClient for every HTTP call in the library. Built lazily and rebuilt only
     * when [proxy] changes: a per-request `OkHttpClient.Builder().build()` (the previous
     * pattern, in three separate files) allocates a fresh dispatcher + connection pool per
     * call, defeats TCP/TLS connection reuse entirely, and lets the proxy configuration
     * drift between copies.
     */
    internal val httpClient: OkHttpClient
        @Synchronized get() {
            val p = proxy
            cachedHttpClient?.let { if (cachedHttpClientProxy === p) return it }
            return OkHttpClient.Builder()
                .apply { p?.let { proxy(it) } }
                .build()
                .also {
                    cachedHttpClient = it
                    cachedHttpClientProxy = p
                }
        }

    /**
     * Initialize the cipher library with the application context.
     * Must be called before using any cipher or potoken functionality.
     */
    fun initialize(context: Context, proxy: Proxy? = null, debugLogging: Boolean = false) {
        CipherDeobfuscator.initialize(context)
        this.proxy = proxy
        this.debugLogging = debugLogging
        PlayerConfigStore.initialize(context)
        PlayerConfigStore.scheduleStartupRefresh()
        PlayerDatesStore.initialize(context)
    }
}
