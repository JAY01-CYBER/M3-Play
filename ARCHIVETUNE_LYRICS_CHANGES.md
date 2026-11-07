# ArchiveTune-style lyrics changes in this package

## Actual source changes included

- Added `BinimumLyricsProvider.kt`, adapted from ArchiveTune's Binimum provider. It queries the Binimum lyrics API, ranks word-timed results first, fetches TTML, validates HTTPS URLs and TTML content, and can continue through multiple matches.
- Registered `BinimumLyricsProvider` in `LyricsHelper.baseProviders` so it participates in normal provider fetching.
- Preserved the inactive-line fix in `LyricsUtils.findCurrentLineIndex`: word-timed lyrics return no active line after the final word's end time (seconds are converted to milliseconds).
- Preserved the matching word-synced UI line-end logic in `LyricsV2.kt`: the karaoke line end is derived from the final word end instead of waiting for the next lyric line.
- Preserved secondary/background vocalist alignment and accompaniment rendering logic in `LyricsV2.kt`.
- Included timing/parser tests for inactive word-timed lines, enhanced LRC timing, QRC word timing, and line-timed QRC fallback.

## Not claimed as complete

This is not a verified 1:1 port of every ArchiveTune lyric setting and UI. A full Android compile could not be confirmed in this environment because the Gradle wrapper download failed due to unavailable network/DNS. Please run the project build in Android Studio before release.
