# Apple Music-style Lyrics Update

This build keeps M3Play's existing lyric providers, LRC/TTML parsing, selection, manual scrolling, and playback timing intact.

## Updated

- `LyricsV2.kt`
  - Added distance-aware spring response for lyric rows so the focused row leads and surrounding rows follow with a softer cascade.
  - Reduced word nudge from 5dp-equivalent translation to a subtle 1.5px-style impulse.
  - Made active lyrics Bold and surrounding lyrics SemiBold.
  - Softened karaoke sweep edge for a smoother progressive reveal.
- `LyricsMotion.kt`
  - Reworked active/inactive opacity hierarchy to keep nearby lyrics readable.
  - Reduced inactive-line scale difference to a subtle 0.975.
  - Reduced blur to a very light supporting effect.
  - Removed the previous bouncy scale behavior in favor of a smooth cubic-bezier transition.

## Notes

`LyricsV2` is already the default lyrics implementation in M3Play. The legacy lyrics implementation remains available. The build could not be Gradle-compiled in this environment because the Gradle wrapper attempted to download Gradle 9.8 and network access was unavailable.
