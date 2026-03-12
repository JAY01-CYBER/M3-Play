package com.j.m3play.data

// Piped API ka use karke stream link nikalna
object StreamExtractor {
    private const val PIPED_API = "https://pipedapi.kavin.rocks"

    suspend fun getAudioStream(videoId: String): String {
        // Asli app mein yahan Retrofit call hogi
        // Ye API humein direct .m4a ya .webm link deti hai
        return "$PIPED_API/streams/$videoId"
    }
}
