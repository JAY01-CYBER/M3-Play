/*
 * M3Play Utility Module
 *
 * Internal helper functions
 * Signature: M3PLAY::UTILITY::V1
 */

package com.my.kizzy.utils

import com.my.kizzy.remote.ApiResponse
import com.my.kizzy.rpc.RpcImage
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode

suspend fun HttpResponse.toImageAsset(): String? {
    return try {
        if (this.status == HttpStatusCode.OK)
            this.body<ApiResponse>().id
        else
            null
    } catch (e: Exception) {
        null
    }
}

fun String.toRpcImage(): RpcImage? {
    if (this.isBlank()) return null
    return when {
        this.startsWith("attachments") -> RpcImage.DiscordImage(this)
        this.startsWith("mp:") -> RpcImage.DiscordImage(this.removePrefix("mp:"))
        this.startsWith("b7.") -> RpcImage.DiscordImage(this)
        this.startsWith("external/") -> RpcImage.DiscordImage(this)
        this.startsWith("http://", ignoreCase = true) || this.startsWith("https://", ignoreCase = true) -> RpcImage.ExternalImage(this)
        else -> RpcImage.DiscordImage(this)
    }
}


