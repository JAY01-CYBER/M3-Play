/*
 * M3Play — Music, thoughtfully crafted.
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Accompanist phonetics are vendored in this project; no Accompanist Maven
 * artifact is required. Dictionary packs live in app/src/main/assets.
 */
package com.j.m3play.lyrics

import android.content.Context
import android.icu.text.Transliterator
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.PhoneticLevel
import com.mocharealm.accompanist.lyrics.core.utils.PhoneticLanguageHint
import com.mocharealm.accompanist.lyrics.core.utils.PhoneticProjection
import com.mocharealm.accompanist.lyrics.core.utils.PhoneticProvider
import com.mocharealm.accompanist.lyrics.core.utils.PhoneticRequest
import com.mocharealm.accompanist.lyrics.core.utils.PhoneticResponse
import com.mocharealm.accompanist.lyrics.core.utils.PhoneticTextRange
import com.mocharealm.accompanist.lyrics.core.utils.withPhonetics
import com.mocharealm.accompanist.lyrics.phonetics.AndroidPackLoader
import com.mocharealm.accompanist.lyrics.phonetics.AsciiFormatter
import com.mocharealm.accompanist.lyrics.phonetics.CjkPhoneticProfile
import com.mocharealm.accompanist.lyrics.phonetics.FormatOptions
import com.mocharealm.accompanist.lyrics.phonetics.LanguageHint
import com.mocharealm.accompanist.lyrics.phonetics.LanguageResult
import com.mocharealm.accompanist.lyrics.phonetics.PackLoader
import com.mocharealm.accompanist.lyrics.phonetics.ParseOptions
import com.mocharealm.accompanist.lyrics.phonetics.ParseWorkspace
import com.mocharealm.accompanist.lyrics.phonetics.PassthroughStyle
import com.mocharealm.accompanist.lyrics.phonetics.PhoneticEngine
import com.mocharealm.accompanist.lyrics.phonetics.PhoneticProfile
import com.mocharealm.accompanist.lyrics.phonetics.PhoneticSpan
import com.mocharealm.accompanist.lyrics.phonetics.Pronunciation
import com.mocharealm.accompanist.lyrics.phonetics.PronunciationFormatter
import com.mocharealm.accompanist.lyrics.phonetics.ReadingCandidate
import com.mocharealm.accompanist.lyrics.phonetics.ReadingLocale
import com.mocharealm.accompanist.lyrics.phonetics.ReadingUnit
import com.mocharealm.accompanist.lyrics.phonetics.Resolution
import com.mocharealm.accompanist.lyrics.phonetics.SharedPackLoader
import com.mocharealm.accompanist.lyrics.phonetics.TextRange
import com.mocharealm.accompanist.lyrics.phonetics.data.CantoneseData
import com.mocharealm.accompanist.lyrics.phonetics.data.JapaneseData
import com.mocharealm.accompanist.lyrics.phonetics.data.MandarinData
import java.util.Locale

/** Thread-safe shared dictionary packs; each parse uses its own workspace. */
object M3PlayPhoneticProvider : PhoneticProvider {
    @Volatile private var loader: PackLoader? = null

    fun initialize(context: Context) {
        if (loader != null) return
        synchronized(this) {
            if (loader == null) {
                loader = SharedPackLoader(AndroidPackLoader(context.applicationContext))
            }
        }
    }

    private val koreanTransliterator by lazy {
        Transliterator.getInstance("Hangul-Latin; Latin-ASCII")
    }

    private val delegate: PhoneticProvider by lazy {
        val packs = checkNotNull(loader) {
            "Call M3PlayPhoneticProvider.initialize(applicationContext) before using phonetics."
        }
        val cjk = CjkPhoneticProfile(
            listOf(
                MandarinData.mainland(packs),
                MandarinData.taiwan(packs),
                CantoneseData.hongKong(packs),
                JapaneseData.japanese(packs),
            ),
            defaultLocale = ReadingLocale.MANDARIN_CN,
        )
        val engine = PhoneticEngine(profiles = listOf(cjk, HangulProfile))
        val formatter = AsciiFormatter(
            FormatOptions(passthrough = PassthroughStyle.OMIT),
            formatters = listOf(object : PronunciationFormatter {
                override val notation: String = "m3play-hangul"
                override fun format(pronunciation: Pronunciation, options: FormatOptions): String =
                    synchronized(koreanTransliterator) {
                        koreanTransliterator
                            .transliterate(pronunciation.units.joinToString("") { it.value })
                            .lowercase(Locale.ROOT)
                    }
            }),
        )

        object : PhoneticProvider {
            override val phoneticLevel = PhoneticLevel.LINE
            override fun getPhonetic(string: String): String = getPhonetic(string, null)

            override fun getPhonetic(string: String, languageTag: String?): String {
                val hints = languageTag?.takeIf { string.isNotEmpty() }?.let {
                    listOf(PhoneticLanguageHint(PhoneticTextRange(0, string.length), it))
                }.orEmpty()
                return resolve(PhoneticRequest(string, hints)).phonetic.orEmpty()
            }

            override fun resolve(request: PhoneticRequest): PhoneticResponse {
                val parsed = engine.parse(
                    request.text,
                    ParseOptions(
                        hints = request.hints.map {
                            LanguageHint(TextRange(it.range.start, it.range.end), it.languageTag)
                        },
                    ),
                    ParseWorkspace(),
                )
                val ranges = request.projectionRanges
                val completePartition = ranges.firstOrNull()?.start == 0 &&
                    ranges.lastOrNull()?.end == request.text.length &&
                    ranges.zipWithNext().all { (a, b) -> a.end == b.start }
                val projections = when {
                    ranges.isEmpty() -> emptyList()
                    !completePartition -> ranges.map {
                        val latin = formatter.formatRange(parsed, TextRange(it.start, it.end))
                        PhoneticProjection(latin?.ifBlank { null }, aligned = latin != null)
                    }
                    else -> formatter.formatRanges(
                        parsed,
                        ranges.map { TextRange(it.start, it.end) },
                    )?.map {
                        PhoneticProjection(it.latin.ifBlank { null }, separatorBefore = it.separatorBefore)
                    } ?: ranges.map { PhoneticProjection(null, aligned = false) }
                }
                return PhoneticResponse(formatter.format(parsed).latin.ifBlank { null }, projections)
            }
        }
    }

    override val phoneticLevel: PhoneticLevel
        get() = PhoneticLevel.LINE
    override fun getPhonetic(string: String): String = delegate.getPhonetic(string)
    override fun getPhonetic(string: String, languageTag: String?): String =
        delegate.getPhonetic(string, languageTag)
    override fun resolve(request: PhoneticRequest): PhoneticResponse = delegate.resolve(request)

    fun enrich(lyrics: SyncedLyrics): SyncedLyrics = lyrics.withPhonetics(this)

    private object HangulProfile : PhoneticProfile {
        override fun matches(codePoint: Int): Boolean =
            com.mocharealm.accompanist.lyrics.phonetics.isHangulCodePoint(codePoint)

        override fun resolve(
            text: String,
            range: TextRange,
            options: ParseOptions,
            workspace: ParseWorkspace,
        ): LanguageResult {
            val reading = ReadingCandidate(
                Pronunciation(
                    locale = null,
                    units = listOf(ReadingUnit(range.textIn(text), sourceRange = range)),
                    notation = "m3play-hangul",
                    languageTag = "ko",
                ),
                sourceIds = listOf("m3play-android-icu"),
            )
            return LanguageResult(
                listOf(PhoneticSpan(range, null, Resolution.RESOLVED, reading, listOf(reading)))
            )
        }
    }
}
