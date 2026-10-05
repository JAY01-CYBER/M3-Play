/*
 * M3Play — Music, thoughtfully crafted.
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * This file is part of M3Play. See the repository LICENSE for terms.
 * Existing copyright and attribution notices are preserved below.
 */

package com.j.m3play.lyrics

import android.content.Context
import com.j.m3play.betterlyrics.Unison
import com.j.m3play.constants.EnableUnisonLyricsKey
import com.j.m3play.utils.dataStore
import com.j.m3play.utils.get

object UnisonLyricsProvider : LyricsProvider {
    override val name = "Unison"
    override fun isEnabled(context: Context): Boolean = context.dataStore[EnableUnisonLyricsKey] ?: true
    override suspend fun getLyrics(id: String, title: String, artist: String, album: String?, duration: Int): Result<String> =
        Unison.getLyrics(id, title, artist, album, duration)
}
