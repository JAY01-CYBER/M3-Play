# M3 Play source patch notes

This ZIP contains source changes based on the repository audit. It is not a signed APK.

## Applied patches

### Security and authentication
- Disabled cleartext networking globally in `app/src/main/res/xml/network_security_config.xml`.
- Set `.DebugActivity` to `android:exported="false"` in the app manifest.
- Removed Discord token Logcat output, stopped clearing the process-wide WebView cookie/storage store on Discord login entry, enabled WebView debugging only for debug builds, restricted navigations/callback page processing to HTTPS Discord origins, and added WebView cleanup on screen disposal.
- Added WebView cleanup to the YouTube login screen and made its WebView reference Compose state so it survives recomposition.
- Excluded DataStore preferences and the WebView data directory from cloud/device backups. This is a backup exclusion, not encryption-at-rest.
- Narrowed FileProvider access to the `cache/images/` directory used by the pre-Android-Q image-sharing path.
- Removed private Last.fm, Paxsenix, Together and Spotify client-secret values from APK BuildConfig fields. The public Last.fm API key and Spotify client ID remain configurable identifiers.
- Changed Spotify authorization-code exchange to Authorization Code + PKCE and removed the bundled client-secret requirement. The redirect URI remains `m3play://callback` and must match the Spotify developer-console configuration.
- Removed private credential injection from GitHub Actions build workflows.
- Reduced build/test workflow token permissions to `contents: read` where write access was not required.
- Added keystore patterns to `.gitignore` and removed an unused project-local `persistent-debug.keystore` that was not referenced by build configuration.

### Playback and stability
- Hardened MIME/codecs parsing and missing content-length handling in both `DownloadUtil.kt` and `MusicService.kt`.
- Removed the no-op `GlobalScope` cache task and made the URL cache concurrent in `DownloadUtil.kt`.
- Removed unused `GlobalScope` imports/opt-in from `App.kt`.

### Database
- Removed Room destructive-migration fallbacks and removed automatic database-file deletion after open/migration failure. On failure, the app now reports the error and preserves the database file.
- Kept the existing schema reconciliation used by the explicit universal migration path, and extended its checks to include primary-key positions, foreign-key definitions, and table-level unique constraints.
- Reused one process-wide maintenance executor instead of allocating a new executor every time the database opens.

### Build tooling
- Marked `gradlew` executable.

## Integration changes to account for

- Spotify must be registered as a public/native client with Authorization Code + PKCE enabled. Confirm `m3play://callback` is registered and test login end-to-end.
- Last.fm operations that require the shared secret need a trusted server-side proxy. `LASTFM_SECRET` is intentionally blank in the APK.
- Paxsenix and Together private API credentials must be supplied through a trusted backend/proxy. Their private-key-backed features will not authenticate with the blank client fields until that backend flow exists.
- The Discord integration still uses a JavaScript bridge in a WebView. Navigation is restricted to Discord HTTPS origins, but `addJavascriptInterface` is exposed to frames; a complete migration to an origin-scoped `WebMessageListener` or official OAuth flow remains recommended before production release.
- DataStore/WebView backup exclusion does not encrypt the credentials at rest. Consider a Keystore-backed credential store and a migration path for existing sessions.
- Some existing `runBlocking` calls remain in synchronous Media3 data-source resolution and other legacy paths. Removing them safely requires a larger asynchronous playback refactor; they were not mechanically rewritten.
- Remaining non-null assertions across the repository require case-by-case review; they were not mass-replaced because blanket edits could change behavior or hide invalid states.

## Validation

- All 435 XML files under `app/src/main` parsed successfully.
- All workflow YAML files parsed successfully.
- Kotlin parser checks found no syntax errors in the edited Kotlin source files.
- A full Gradle compile/test could not be completed in this environment: the wrapper attempted to download Gradle 9.8.0 from `services.gradle.org`, but DNS/network access was unavailable. Run `./gradlew :app:compileUniversalDebugKotlin testUniversalDebugUnitTest` in an Android SDK environment before release.
- Real-device tests remain required for playback, Spotify PKCE, Discord login, YouTube login, backup/restore and Room migrations.
