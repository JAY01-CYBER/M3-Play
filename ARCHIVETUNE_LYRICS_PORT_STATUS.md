# ArchiveTune lyrics integration status

This source snapshot integrates the ArchiveTune-derived lyrics work into M3 Play's existing player and lyrics pipeline.

## Included

- Provider registry includes Unison, Binimum, SimpMusic, BetterLyrics, BetterLyrics Portato, Paxsenix main/Apple Music/Spotify/Musixmatch, Megalobiz, LRCLIB, KuGou, YouTube subtitles, YouTube lyrics, and YouLy+.
- Binimum provider lookup and TTML retrieval.
- TTML parsing path preserving word timing, background-vocal metadata, singer/agent metadata, provider translations, and provider romanization where present.
- QRC parsing path and synced-lyrics handling.
- Lyrics V2 renderer integration for word timing, secondary singer/background-vocal line alignment, and line completion based on the final timed word.
- Lyrics quality scoring, concurrent provider lookup, caching, and duplicate-result removal in M3's existing helper.

## Not certified as full parity

This is an integration into M3 Play, not a byte-for-byte port of the whole ArchiveTune application. ArchiveTune-specific settings screens, all rendering variants, translation workflows, image-card/share features, and every upstream provider edge case have not been verified as identical. Provider APIs may change independently.

## Verification

The delivered source archive passed ZIP integrity validation. A Gradle Kotlin compile could not be completed in the build environment because the Gradle wrapper distribution was unavailable and network DNS access to `services.gradle.org` failed. Please run the project's normal Gradle build in an environment with the required Gradle distribution and dependencies available.

## Licensing

The project is GPL-3.0. Adapted ArchiveTune lyrics components retain applicable attribution in source headers; see `LICENSE` and `THIRD_PARTY_NOTICES.md`.
