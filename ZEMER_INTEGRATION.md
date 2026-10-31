# Zemer Cipher integration notes

This branch integrates Zemer Cipher as an Android library module (`:zemercipher`).

## Integrated behavior
- Zemer's player configuration store and bundled player configuration assets initialize from `App`.
- M3 Play keeps NewPipe as its primary signature/N-parameter resolver.
- If NewPipe cannot resolve a WEB_REMIX ciphered stream URL, Zemer's `CipherDeobfuscator` is tried as a fallback.
- For WEB_REMIX playback only, Zemer's BotGuard-backed PoToken generator is called off the UI thread.
- The player-request token is passed to the WEB_REMIX player request; the video-bound streaming token is added to the selected stream URL.
- The raw stream URL cache strips `pot` so a visitor/video-bound token is not reused across later resolutions.
- If token generation fails, M3 Play continues with its existing fallback client chain.
- Zemer token/session identifiers are not written to logs.

## Important limitations
- This source integration has not been compiled with Gradle or tested against live YouTube playback in this environment.
- Zemer's PoToken generator uses a synchronous public API internally; M3 wraps it on `Dispatchers.IO`.
- Runtime remote player configuration still depends on the upstream Zemer configuration endpoint being reachable.
- NewPipe remains primary; Zemer cipher fallback is limited to WEB_REMIX to avoid changing other client behavior.
- Review the included GPL-3.0 license and attribution before redistributing.

## Verification required
Run:
- `./gradlew :zemercipher:assembleRelease`
- `./gradlew :app:compileArm64ReleaseKotlin`
- `./gradlew :app:lintVitalAnalyzeRelease`
Then test WEB_REMIX playback, Android VR playback, offline startup, proxy-enabled playback, and WebView renderer recovery on a device.


## Follow-up STS alignment fix
- WEB_REMIX player requests now use the signature timestamp extracted by Zemer from the same player.js used by its cipher engine.
- Other YouTube clients continue using M3 Play's existing NewPipe STS.
- Added explicit log messages to distinguish a Zemer cipher attempt, missing cipher data, and a null/success result.
- Cancellation is rethrown instead of being swallowed by the fallback.
