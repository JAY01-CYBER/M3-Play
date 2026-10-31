/*
 * M3Play — Zemer Cipher integration.
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Zemer Cipher source and license notices are preserved in :zemercipher.
 */
package com.j.m3play.utils

import android.content.Context
import com.j.m3play.innertube.YouTube
import com.zemer.cipher.ZemerCipher
import com.zemer.cipher.CipherDeobfuscator
import com.zemer.cipher.potoken.PoTokenGenerator
import com.zemer.cipher.potoken.PoTokenResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Best-effort adapter for Zemer's Web PoToken generator.
 *
 * Token generation is deliberately off the caller/UI thread. If WebView or BotGuard
 * is unavailable, playback continues through M3 Play's existing client fallback chain.
 */
object ZemerPoTokenProvider {
    @Volatile
    private var initialized = false

    private val generator: PoTokenGenerator by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        PoTokenGenerator()
    }

    fun initialize(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            ZemerCipher.initialize(
                context = context.applicationContext,
                proxy = YouTube.proxy,
                debugLogging = false
            )
            initialized = true
        }
    }


    /**
     * Returns the STS extracted from the same player.js that Zemer's cipher engine uses.
     * WEB_REMIX player requests must use this value so the returned cipher matches that JS.
     */
    suspend fun signatureTimestamp(): Int? {
        if (!initialized) return null
        return try {
            CipherDeobfuscator.signatureTimestamp()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Timber.tag("ZemerPoToken").w(
                "Zemer player STS unavailable (${error.javaClass.simpleName}); using existing STS fallback"
            )
            null
        }
    }

    suspend fun generate(videoId: String, visitorData: String): PoTokenResult? {
        if (!initialized || videoId.isBlank() || visitorData.isBlank()) return null

        return withContext(Dispatchers.IO) {
            try {
                // Keep the library's HTTP client aligned with M3 Play's current proxy.
                ZemerCipher.proxy = YouTube.proxy
                generator.getWebClientPoToken(videoId, visitorData)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Timber.tag("ZemerPoToken").w(
                    "PoToken generation unavailable (${error.javaClass.simpleName}); using existing playback fallbacks"
                )
                null
            }
        }
    }
}
