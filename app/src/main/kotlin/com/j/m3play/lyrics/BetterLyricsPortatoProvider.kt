/*
 * M3Play — Music, thoughtfully crafted.
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.j.m3play.lyrics

import android.content.Context
import com.j.m3play.betterlyrics.BetterLyrics
import com.j.m3play.constants.EnableBetterLyricsKey
import com.j.m3play.utils.dataStore
import com.j.m3play.utils.get

/** QQ/QRC endpoint from the existing BetterLyrics client, kept as a separate candidate. */
object BetterLyricsPortatoProvider : LyricsProvider {
    override val name: String = "BetterLyrics · QQ/QRC"

    override fun isEnabled(context: Context): Boolean =
        context.dataStore[EnableBetterLyricsKey] ?: true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        album: String?,
        duration: Int,
    ): Result<String> =
        BetterLyrics.getPortatoLyrics(
            title = title,
            artist = artist,
            album = album,
            durationSeconds = duration,
        )

    override suspend fun getAllLyrics(
        id: String,
        title: String,
        artist: String,
        album: String?,
        duration: Int,
        callback: (String) -> Unit,
    ) {
        BetterLyrics.getAllPortatoLyrics(
            title = title,
            artist = artist,
            album = album,
            durationSeconds = duration,
            callback = callback,
        )
    }
}
