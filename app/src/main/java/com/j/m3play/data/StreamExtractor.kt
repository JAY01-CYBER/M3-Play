package com.j.m3play.data

object StreamExtractor {
    private const val PIPED_API = "https://pipedapi.kavin.rocks"

    suspend fun getAudioStream(videoId: String): String = "$PIPED_API/streams/$videoId"
}
