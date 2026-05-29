package com.j.m3play.lyrics

import com.music.youlyplus.YouLyPlus

object SmartYouLyPlus {

    suspend fun getBestLyrics(
        title: String,
        artist: String,
        album: String?,
        duration: Int,
        id: String
    ): Result<String> {

        val queries = QueryCleaner.cleanTitle(title)

        for (query in queries) {
            try {
                val result = YouLyPlus.getLyrics(
                    query,
                    artist,
                    duration,
                    album,
                    id
                )

                if (result.isSuccess && !result.getOrNull().isNullOrBlank()) {
                    return result
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return Result.failure(Exception("YouLyPlus no match"))
    }
}
