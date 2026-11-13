# SimpMusic playlist background port

- Uses SimpMusic's KMPalette 3.1.0 palette state/generate pipeline for playlist cover-driven page background.
- Uses the dominant swatch first, then muted/vibrant fallbacks, and adaptively darkens the selected color.
- Holds the last resolved color while a new palette is being generated and animates color transitions over 500 ms.
- Preserves M3 Play's existing AndroidX Palette extraction and PlayerColorExtractor gradient for the cover shadow.
- Palette background applies to the playlist screen's root; existing playlist content and controls remain intact.

Build status: not run in this environment; please verify with the project's Gradle build.
