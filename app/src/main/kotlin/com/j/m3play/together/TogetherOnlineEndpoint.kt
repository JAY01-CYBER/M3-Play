/*
 * M3Play - Modern Music Player
 *
 * Copyright (c) 2026 JAY01-CYBER
 * Signature: M3PLAY::GENERAL::V1
 */

package com.j.m3play.together

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import java.net.URI

object TogetherOnlineEndpoint {

    private const val MyServerBaseUrl = "https://metroserverx.meowery.eu"

    suspend fun baseUrlOrNull(
        dataStore: DataStore<Preferences>,
    ): String {
        return MyServerBaseUrl
    }

    fun onlineWebSocketUrlOrNull(
        rawWsUrl: String,
        baseUrl: String,
    ): String? {
        val derived = deriveWebSocketUrlFromBaseUrl(baseUrl) ?: return null
        val normalized = normalizeWebSocketUrl(rawWsUrl, baseUrl) ?: return derived

        val host = runCatching { URI(normalized).host }.getOrNull()?.trim()?.lowercase() ?: return derived
        if (host == "localhost" || host == "127.0.0.1" || host == "0.0.0.0") return derived

        val baseHost = runCatching { URI(baseUrl.trim()).host }.getOrNull()?.trim()?.lowercase()
        if (baseHost != null && isIpv4Address(baseHost) && !isIpv4Address(host)) return derived

        return normalized
    }

    private fun isIpv4Address(host: String): Boolean {
        val parts = host.split('.')
        if (parts.size != 4) return false
        return parts.all { part ->
            val n = part.toIntOrNull() ?: return@all false
            n in 0..255 && part == n.toString()
        }
    }

    private fun deriveWebSocketUrlFromBaseUrl(baseUrl: String): String? {
        val uri = runCatching { URI(baseUrl.trim()) }.getOrNull() ?: return null
        val host = uri.host?.trim()?.ifBlank { null } ?: return null
        val scheme = uri.scheme?.trim()?.lowercase()
        val wsScheme = if (scheme == "https") "wss" else "ws"

        val portPart = if (uri.port != -1 && uri.port != 80 && uri.port != 443) ":${uri.port}" else ""
        val normalizedPath = uri.path?.trim()?.trimEnd('/').orEmpty().let { if (it.endsWith("/v1")) it else "$it/v1" }

        
        return "$wsScheme://$host$portPart$normalizedPath/together/ws"
    }

    private fun normalizeWebSocketUrl(raw: String, baseUrl: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return null
        if (trimmed.startsWith("ws://") || trimmed.startsWith("wss://")) return trimmed
        if (trimmed.startsWith("http://")) return "ws://${trimmed.removePrefix("http://")}"
        if (trimmed.startsWith("https://")) return "wss://${trimmed.removePrefix("https://")}"
        if (trimmed.startsWith("/")) {
            val baseUri = runCatching { URI(baseUrl.trim()) }.getOrNull() ?: return null
            val host = baseUri.host?.trim()?.ifBlank { null } ?: return null
            val scheme = baseUri.scheme?.trim()?.lowercase()
            val wsScheme = if (scheme == "https") "wss" else "ws"
            val portPart = if (baseUri.port != -1 && baseUri.port != 80 && baseUri.port != 443) ":${baseUri.port}" else ""
            val basePath = baseUri.path?.trim()?.trimEnd('/').orEmpty()
            return "$wsScheme://$host$portPart$basePath$trimmed"
        }

        val baseScheme = runCatching { URI(baseUrl.trim()).scheme?.trim()?.lowercase() }.getOrNull()
        val wsScheme = if (baseScheme == "https") "wss" else "ws"
        return "$wsScheme://$trimmed"
    }
}
