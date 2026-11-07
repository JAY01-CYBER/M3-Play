# Accompanist source integration (no Accompanist Maven artifacts)

This project vendors the supplied Accompanist source directly into the Android app:
- Latest Lyrics UI Kotlin sources are in `app/src/main/kotlin/com/mocharealm/accompanist/lyrics/ui/`.
- Latest Lyrics Core Kotlin sources are in `app/src/main/kotlin/com/mocharealm/accompanist/lyrics/core/`.
- Phonetics runtime, Android pack loader, and language resolver source are in `app/src/main/kotlin/com/mocharealm/accompanist/lyrics/phonetics/`.
- Mandarin, Cantonese, and Japanese `.lpd` dictionaries are in `app/src/main/assets/` and are kept uncompressed for memory mapping.
- `M3PlayPhoneticProvider` initializes the vendored dictionary packs and is wired into `LyricsV2`.
- The upstream `ContinuousRoundedRectangle` usage was replaced with Compose's `RoundedCornerShape` so the UI does not require the separate `gaze-capsule` artifact.

No `com.mocharealm.accompanist:*` or `com.mocharealm.gaze:*` Maven dependency is needed for this integration.

## Build
Run `./gradlew :app:compileUniversalDebugKotlin` or your normal CI build workflow.

The build could not be executed in this editing environment because Gradle is not installed and the Gradle wrapper distribution cannot be downloaded without network access. Source integration and dependency-removal checks were performed, but a real Android build remains to be confirmed in CI/Android Studio.
