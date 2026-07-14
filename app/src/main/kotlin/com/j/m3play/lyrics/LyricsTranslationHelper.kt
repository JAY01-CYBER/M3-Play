/**
 * M3Play Project
 */
package com.j.m3play.lyrics

import android.content.Context
import com.j.m3play.db.MusicDatabase
import com.j.m3play.db.entities.LyricsEntity
import com.j.m3play.db.entities.SongEntity
import com.j.m3play.api.DeepLService
import com.j.m3play.api.MistralService
import com.j.m3play.api.OpenRouterService
import com.j.m3play.api.OpenRouterStreamingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * A helper class that provides AI-powered translation for lyrics.
 */
object LyricsTranslationHelper {
    private val _status = MutableStateFlow<TranslationStatus>(TranslationStatus.Idle)
    val status: StateFlow<TranslationStatus> = _status.asStateFlow()

    private val _hasActiveTranslations = MutableStateFlow(false)
    val hasActiveTranslations: StateFlow<Boolean> = _hasActiveTranslations.asStateFlow()

    private val _translationSaved = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val translationSaved: SharedFlow<Unit> = _translationSaved.asSharedFlow()

    private val _manualTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val manualTrigger: SharedFlow<Unit> = _manualTrigger.asSharedFlow()

    private val _clearTranslationsTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val clearTranslationsTrigger: SharedFlow<Unit> = _clearTranslationsTrigger.asSharedFlow()

    private var translationJob: kotlinx.coroutines.Job? = null
    private var isCompositionActive = true

    private val translationCache = ConcurrentHashMap<String, List<String>>()

    private val LanguageCodeToName = mapOf(
        "en" to "English", "es" to "Spanish", "fr" to "French", "de" to "German",
        "it" to "Italian", "pt" to "Portuguese", "ru" to "Russian", "ja" to "Japanese",
        "ko" to "Korean", "zh" to "Chinese", "ar" to "Arabic", "hi" to "Hindi",
        "bn" to "Bengali", "pa" to "Punjabi", "tr" to "Turkish", "vi" to "Vietnamese",
        "th" to "Thai", "id" to "Indonesian", "pl" to "Polish", "nl" to "Dutch",
        "sv" to "Swedish", "uk" to "Ukrainian"
    )

    fun setCompositionActive(active: Boolean) { isCompositionActive = active }
    fun triggerManualTranslation() { _manualTrigger.tryEmit(Unit) }
    fun triggerClearTranslations() { _clearTranslationsTrigger.tryEmit(Unit); _hasActiveTranslations.value = false }

    fun clearTranslations(lyrics: LyricsEntity): LyricsEntity {
        return lyrics.copy(translatedLyrics = "", translationLanguage = "", translationMode = "")
    }

    fun cancelTranslation() {
        translationJob?.cancel()
        if (_status.value is TranslationStatus.Translating) _status.value = TranslationStatus.Idle
    }

    private fun getCacheKey(text: String, mode: String, targetLanguage: String): String = "${text.hashCode()}_${mode}_${targetLanguage}"

    private fun tryParsePartialTranslation(content: String, expectedLines: Int): List<String> {
        return content.lines().map { it.trim() }.filter { it.isNotBlank() }
            .map { it.replace(Regex("^\\d+\\.\\s*"), "").replace(Regex("^Line\\s+\\d+:\\s*", RegexOption.IGNORE_CASE), "").replace(Regex("^-\\s*"), "") }
    }

    fun loadTranslationsFromDatabase(lyrics: List<LyricsEntry>, lyricsEntity: LyricsEntity?, targetLanguage: String, mode: String) {
        if (lyricsEntity == null || lyricsEntity.translatedLyrics.isNullOrBlank()) {
            _hasActiveTranslations.value = false
            lyrics.forEach { it.translatedTextFlow.value = null }
            return
        }
        if (lyricsEntity.translationLanguage != targetLanguage || lyricsEntity.translationMode != mode) {
            _hasActiveTranslations.value = false
            lyrics.forEach { it.translatedTextFlow.value = null }
            return
        }
        val translatedLines = lyricsEntity.translatedLyrics.split("\n")
        val nonEmptyEntries = lyrics.filter { it.text.isNotBlank() }
        if (translatedLines.size >= nonEmptyEntries.size) {
            var transIndex = 0
            lyrics.forEach { entry ->
                if (entry.text.isNotBlank() && transIndex < translatedLines.size) {
                    entry.translatedTextFlow.value = translatedLines[transIndex]
                    transIndex++
                }
            }
            val cacheKey = getCacheKey(nonEmptyEntries.joinToString("\n") { it.text }, mode, targetLanguage)
            translationCache[cacheKey] = translatedLines
            _hasActiveTranslations.value = true
        }
    }

    fun translateLyrics(
        lyrics: List<LyricsEntry>, targetLanguage: String, apiKey: String, baseUrl: String, model: String, mode: String,
        scope: CoroutineScope, context: Context, provider: String = "OpenRouter", deeplApiKey: String = "", deeplFormality: String = "default",
        useStreaming: Boolean = true, songId: String = "", database: MusicDatabase? = null, systemPrompt: String = "",
    ) {
        translationJob?.cancel()
        _status.value = TranslationStatus.Translating
        lyrics.forEach { it.translatedTextFlow.value = null }

        translationJob = scope.launch(Dispatchers.IO) {
            try {
                val effectiveApiKey = if (provider == "DeepL") deeplApiKey else apiKey
                if (effectiveApiKey.isBlank()) {
                    _status.value = TranslationStatus.Error("API Key is missing")
                    return@launch
                }
                if (lyrics.isEmpty()) {
                    _status.value = TranslationStatus.Error("Lyrics are missing")
                    return@launch
                }
                val nonEmptyEntries = lyrics.mapIndexedNotNull { index, entry -> if (entry.text.isNotBlank()) index to entry else null }
                if (nonEmptyEntries.isEmpty()) return@launch

                val fullText = nonEmptyEntries.joinToString("\n") { it.second.text }
                val cacheKey = getCacheKey(fullText, mode, targetLanguage)
                val cachedTranslations = translationCache[cacheKey]
                if (cachedTranslations != null && cachedTranslations.size >= nonEmptyEntries.size) {
                    nonEmptyEntries.forEachIndexed { idx, (originalIndex, _) -> if (idx < cachedTranslations.size) lyrics[originalIndex].translatedTextFlow.value = cachedTranslations[idx] }
                    _hasActiveTranslations.value = true
                    _status.value = TranslationStatus.Success
                    if (songId.isNotBlank() && database != null) {
                        try {
                            val currentLyrics = database.lyrics(songId).first()
                            if (currentLyrics != null && currentLyrics.translatedLyrics.isNullOrBlank()) {
                                database.query { upsert(currentLyrics.copy(translatedLyrics = cachedTranslations.joinToString("\n"), translationLanguage = targetLanguage, translationMode = mode)) }
                                _translationSaved.tryEmit(Unit)
                            }
                        } catch (e: Exception) {}
                    }
                    delay(3000)
                    if (_status.value is TranslationStatus.Success && isCompositionActive) _status.value = TranslationStatus.Idle
                    return@launch
                }

                val fullLanguageName = LanguageCodeToName[targetLanguage] ?: targetLanguage

                val result = if (provider == "DeepL") {
                    DeepLService.translate(fullText, targetLanguage, deeplApiKey, deeplFormality)
                } else if (provider == "Mistral") {
                    MistralService.translate(fullText, fullLanguageName, apiKey, model, mode, systemPrompt)
                } else if (useStreaming && provider != "Custom") {
                    var translatedLines: List<String>? = null
                    var hasError = false
                    var errorMessage = ""
                    val contentAccumulator = StringBuilder()

                    OpenRouterStreamingService.streamTranslation(fullText, fullLanguageName, apiKey, baseUrl, model, mode, systemPrompt).collect { chunk ->
                        when (chunk) {
                            is OpenRouterStreamingService.StreamChunk.Content -> {
                                contentAccumulator.append(chunk.text)
                                val partialResult = tryParsePartialTranslation(contentAccumulator.toString(), nonEmptyEntries.size)
                                if (partialResult.isNotEmpty()) {
                                    partialResult.forEachIndexed { idx, translation ->
                                        if (idx < nonEmptyEntries.size && translation.isNotBlank()) lyrics[nonEmptyEntries[idx].first].translatedTextFlow.value = translation
                                    }
                                    _status.value = TranslationStatus.Translating
                                }
                            }
                            is OpenRouterStreamingService.StreamChunk.Complete -> translatedLines = chunk.translatedLines
                            is OpenRouterStreamingService.StreamChunk.Error -> { hasError = true; errorMessage = chunk.message }
                        }
                    }
                    if (hasError) Result.failure(Exception(errorMessage)) else if (translatedLines != null) Result.success(translatedLines!!) else Result.failure(Exception("No translation received"))
                } else {
                    OpenRouterService.translate(fullText, fullLanguageName, apiKey, baseUrl, model, mode, systemPrompt)
                }

                result.onSuccess { translatedLines ->
                    if (!isCompositionActive) return@onSuccess
                    translationCache[getCacheKey(fullText, mode, targetLanguage)] = translatedLines
                    if (songId.isNotBlank() && database != null) {
                        try {
                            val currentLyrics = database.lyrics(songId).first()
                            if (currentLyrics != null) {
                                database.query { upsert(currentLyrics.copy(translatedLyrics = translatedLines.joinToString("\n"), translationLanguage = targetLanguage, translationMode = mode)) }
                                _translationSaved.tryEmit(Unit)
                            }
                        } catch (e: Exception) {}
                    }
                    val expectedCount = nonEmptyEntries.size
                    if (translatedLines.size >= expectedCount) {
                        nonEmptyEntries.forEachIndexed { idx, (originalIndex, _) -> lyrics[originalIndex].translatedTextFlow.value = translatedLines[idx] }
                    } else {
                        translatedLines.forEachIndexed { idx, translation -> if (idx < nonEmptyEntries.size) lyrics[nonEmptyEntries[idx].first].translatedTextFlow.value = translation }
                    }
                    _hasActiveTranslations.value = true
                    _status.value = TranslationStatus.Success
                    delay(3000)
                    if (_status.value is TranslationStatus.Success && isCompositionActive) _status.value = TranslationStatus.Idle
                }.onFailure { error ->
                    if (!isCompositionActive) return@onFailure
                    _status.value = TranslationStatus.Error(error.message ?: "Failed")
                }
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException && isCompositionActive) _status.value = TranslationStatus.Error(e.message ?: "Failed")
            }
        }
    }

    sealed class TranslationStatus {
        data object Idle : TranslationStatus()
        data object Translating : TranslationStatus()
        data object Success : TranslationStatus()
        data class Error(val message: String) : TranslationStatus()
    }
}
