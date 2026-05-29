package com.j.m3play.lyrics

import java.util.Locale

object LyricsTranslator {

    fun detectLanguage(text: String): String {
        return when {
            Regex("[\\u0900-\\u097F]").containsMatchIn(text) -> "hi"
            Regex("[\\u4E00-\\u9FFF]").containsMatchIn(text) -> "zh"
            Regex("[\\u3040-\\u30FF]").containsMatchIn(text) -> "ja"
            Regex("[\\uAC00-\\uD7AF]").containsMatchIn(text) -> "ko"
            else -> "en"
        }
    }

    suspend fun translate(text: String, targetLang: String): String {
        return try {
            val url = "https://translate.googleapis.com/translate_a/single" +
                    "?client=gtx&sl=auto&tl=$targetLang&dt=t&q=" +
                    java.net.URLEncoder.encode(text, "UTF-8")

            val response = java.net.URL(url).readText()

            Regex("\"(.*?)\"")
                .findAll(response)
                .map { it.groupValues[1] }
                .joinToString(" ")

        } catch (e: Exception) {
            e.printStackTrace()
            text
        }
    }

    fun getDeviceLanguage(): String {
        return Locale.getDefault().language
    }
}
