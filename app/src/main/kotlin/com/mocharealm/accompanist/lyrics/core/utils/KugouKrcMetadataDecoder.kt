package com.mocharealm.accompanist.lyrics.core.utils

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

object KugouKrcMetadataDecoder {
    data class Metadata(
        val translations: List<String> = emptyList(),
        val phonetics: List<List<String>> = emptyList()
    )

    @OptIn(ExperimentalEncodingApi::class)
    fun decode(languageHeader: String?): Metadata {
        if (languageHeader.isNullOrBlank()) return Metadata()

        val contentBase64 = languageHeader
            .substringAfter("[language:")
            .substringBeforeLast("]")
            .trim()

        if (contentBase64.isEmpty()) return Metadata()

        return try {
            val decodedBytes = Base64.decode(contentBase64)
            val jsonStr = decodedBytes.decodeToString()
            parseJsonContent(jsonStr)
        } catch (e: Exception) {
            e.printStackTrace()
            Metadata()
        }
    }

    private fun parseJsonContent(jsonStr: String): Metadata {
        try {
            val root = Json.parseToJsonElement(jsonStr).jsonObject
            val contentArray = root["content"]?.jsonArray ?: return Metadata()

            val lyricLines = mutableListOf<String>()
            val pronLines = mutableListOf<List<String>>()

            for (element in contentArray) {
                val jsonObj = element.jsonObject
                val type = jsonObj["type"]?.jsonPrimitive?.intOrNull

                if (type == 1) {
                    val allRows = jsonObj["lyricContent"]?.jsonArray ?: continue
                    for (row in allRows) {
                        val fullLineTrans = row.jsonArray.joinToString("") { part ->
                            part.jsonPrimitive.content
                        }
                        lyricLines.add(fullLineTrans)
                    }
                }
                else if (type == 0) {
                    val allRows = jsonObj["lyricContent"]?.jsonArray ?: continue
                    for (row in allRows) {
                        val rowSyllables = row.jsonArray.map { syllableParts ->
                            syllableParts.jsonArray.joinToString("") { it.jsonPrimitive.content }
                        }
                        pronLines.add(rowSyllables)
                    }
                }
            }

            return Metadata(
                translations = lyricLines,
                phonetics = pronLines
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return Metadata()
        }
    }
}