# M3Play 3.2 upgrade notes

## Build and release

- Version 3.2.0, version code 144; compile/target SDK 37 (Android 17).
- Java 21, Gradle 9.8.0, Android Gradle Plugin 9.4.1, Kotlin 2.4.20 and KSP 2.3.12.
- Compose 1.12.1, Material 3 1.5.0-alpha29, Media3 1.11.1, Ktor 3.6.0 and NewPipeExtractor v0.26.5. Dependency versions in `gradle/libs.versions.toml` and module build files were checked against upstream Maven metadata/tags on 2026-10-05. Material 3 remains on the Expressive alpha channel.
- Install SDK packages `platforms;android-37.0` and `build-tools;37.0.0`.
- GitLab builds a debug APK for unprotected refs and merge requests. Protected branch/tag pipelines build and sign the universal release APK. Merge into protected `main` to use the existing Protected signing variables.
- Required variables: `ANDROID_SIGNING_KEY`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`. The signing key accepts Base64 text or a File variable containing binary/Base64 keystore data. Passwords belong in masked/hidden CI variables.
- Download `M3Play-release.apk` from the successful `build_release` job's artifacts. Keep the matching `mapping.txt` for crash deobfuscation. Production signing depends on the keystore and passwords configured in GitLab.
- R8 keeps app reflection boundaries (Gson Spotify DTOs, persisted queue graphs, extractor/JavaScript and dictionary integration); libraries' own consumer rules cover their internals.

[Android 17 setup](https://developer.android.com/about/versions/17/setup-sdk), [AGP 9.4 compatibility](https://developer.android.com/build/releases/agp-9-4-0-release-notes).

## UI and preferences

The global theme uses Material 3 Expressive, artwork-derived/system colors, rounded controls and stronger typography. Album and artist headers adapt between large portrait artwork and a side-by-side wide layout. Playlist controls and library filters use the shared theme.

Appearance settings include **Enable blur**, preinstalled font choices and `.ttf`/`.otf` import. Imports are validated, limited to 8 MB and copied into private app storage. Font choices expose existing repository assets (including its SF Pro Display bold asset); no new Apple font download was added. Lyrics and lyric cards inherit the selected app font.

Blur covers artwork/lyric focus and frosted navigation/player controls. Foreground labels remain sharp. Blur defaults off, requires Android 12+, and is disabled on low-memory devices and in Battery Saver. Haze uses its performance mode; capture is attached only when blur is enabled. Blur radius is bounded.

## Lyrics

The player's lyrics action opens a separate fullscreen screen, with its own close/back action and playback controls. The obsolete inline renderer is removed. The advanced renderer is the default for new preferences; an existing renderer preference is retained.

Line focus uses scale, opacity and optional blur. Timed words/syllables keep their original TTML timing and whitespace. Enhanced LRC timestamps are normalized to seconds for the renderer, preserving syllable whitespace and explicit end markers. Seeking selects a line at its exact timestamp. Manual scrolling can reach the entire song, retains momentum and resumes following after four idle seconds. The animation clock handles playback speed, pauses with the lifecycle, and polls more slowly while paused. The screen stays awake only while playback is active. Timing accuracy is limited by the lyrics supplied by the provider; plain lyrics cannot provide authentic word timing.

Provider calls are cancellation-aware at the orchestration boundary, have a per-provider timeout and use a small positive-result cache keyed by metadata and enabled provider order. Track changes cancel screen fetches. TTML rejects entity/DOCTYPE declarations and oversized documents.

### Provider contract audit

| Provider | Contract and integration |
| --- | --- |
| LyricsPlus / YouLy+ | Existing provider retained under one visible name to avoid duplicate requests. Uses documented `/v2/lyrics/get` title/artist/album/duration parameters. Removed the dead primary host listed upstream and the unsupported `id` parameter; retains the documented mirror. [Official endpoints](https://github.com/ibratabian17/LyricsPlus/blob/main/docs/endpoints.md) |
| Unison | Added public `/lyrics` reads, first by video ID, then metadata on a missing-video response. Supports TTML/LRC/plain responses; does not retry rate limits. [Official docs](https://docs.betterlyrics.org/unison) |
| Better Lyrics | Updated API host to `api.betterlyrics.org`; supplies album/duration for matching and preserves raw TTML. Uncached upstream lyrics can require unavailable API credentials; ordinary provider fallback handles misses. [Official docs](https://docs.betterlyrics.org/) |
| SimpMusic | Public HTTPS `/v1/{videoId}` reads. Accepts the documented `success` envelope and legacy `type` envelope; preserves unknown-duration results. [Official OpenAPI viewer](https://lyrics.simpmusic.org/#/docs) |
| LRCLIB | `/api/search` uses track/artist/album names, duration matching and plain-text fallback. Unknown durations are not compared to zero seconds. [Official search implementation](https://github.com/tranxuanthang/lrclib/blob/main/server/src/routes/search_lyrics.rs) |
| Paxsenix | Existing Apple Music lyrics integration reviewed against its OpenAPI `/apple-music/lyrics` contract. Retains v1/default response handling. [API docs](https://lyrics.paxsenix.org/docs) |
| KuGou, YouTube lyrics/subtitles | Existing integrations retained. No public official contract for these internal endpoints was established; they remain subject to upstream changes. |

## Search and playback compatibility

Search All collects shelves across response tabs, nested item sections, carousel and grid categories. Continuation requests preserve explicit/video filters and prevent concurrent duplicate loads. Switching search tabs cancels stale requests.

NewPipeExtractor and Media3 are updated; audio-offload configuration now calls Media3's supported API instead of reflection. No new hardcoded InnerTube client version was invented: a live YouTube Music response returned consent HTML during the audit, so live authenticated search/playback still needs device verification.

Android 17 Music Together LAN actions, including invite links, request `ACCESS_LOCAL_NETWORK`. Background playback retains the declared media-playback foreground service, initiated by user playback actions. Test notification/headset resume on Android 17 as well as normal foreground playback. See [local-network permission](https://developer.android.com/privacy-and-security/local-network-permission) and [background audio changes](https://developer.android.com/about/versions/17/changes/bg-audio).

## Verification

Regression coverage includes TTML span timing/whitespace/entity rejection, timestamp boundaries, Unison requests/fallbacks, SimpMusic envelopes, LRCLIB matching and nested search categories. Run the repository's universal debug assembly and lint, module tests, and `scripts/ci/build-release.sh` with a disposable test keystore to exercise shrinking/signing. Never replace production CI secrets with test credentials.

Build checks cannot establish visual fidelity, accessibility gestures, actual heat/battery consumption or Android 17 runtime behavior. These need a physical device/emulator. The repository's lint configuration reports existing findings without failing the task (`abortOnError = false`); inspect the report in addition to its exit status.
