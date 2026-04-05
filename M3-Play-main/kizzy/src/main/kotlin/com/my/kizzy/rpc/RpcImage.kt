/*
 * M3Play - Modern Music Player
 *
 * Copyright (c) 2026 JAY01-CYBER
 * Signature: M3PLAY::GENERAL::V1
 */

package com.my.kizzy.rpc

import com.my.kizzy.repository.KizzyRepository

/**
 * Modified by Koiverse
 */
sealed class RpcImage {
    abstract suspend fun resolveImage(repository: KizzyRepository): String?

    class DiscordImage(val image: String) : RpcImage() {
        override suspend fun resolveImage(repository: KizzyRepository): String {
            return if (image.startsWith("mp:")) image else "mp:$image"
        }
    }

    class ExternalImage(val image: String) : RpcImage() {
        override suspend fun resolveImage(repository: KizzyRepository): String? {
            if (image.startsWith("mp:") || image.startsWith("external/") || image.startsWith("attachments/")) {
                return if (image.startsWith("mp:")) image else "mp:$image"
            }
            return repository.getImage(image)
        }
    }
}
