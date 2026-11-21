# Zemer Cipher integration notes

This branch integrates Zemer Cipher as an Android library module (`:zemercipher`).

## Integrated behavior
- Zemer's player configuration store and bundled player configuration assets initialize from `App`.
- NewPipe remains the first URL resolver for every supported YouTube client.
- If NewPipe cannot resolve a ciphered stream URL, Zemer's signature decipher and N-parameter transformation are attempted for the active client.
- Player requests use the STS extracted by Zemer from the same player.js used by its cipher engine, with the existing NewPipe STS retained as a fallback.
- Zemer's Web PoToken generator is used only for clients whose InnerTube configuration requires Web Service Integrity tokens: WEB, WEB_REMIX, WEB_CREATOR, MWEB, WEB_EMBEDDED_PLAYER, and supported TVHTML5 variants.
- Android and iOS clients do not receive Web PoTokens.
- The player-request token is passed to eligible player requests; the video-bound streaming token is added to eligible stream URLs.
- Stream URL cache entries are keyed by video, client, and itag so client-bound URLs are not reused across clients. Cached URLs omit `pot` so visitor/video-bound tokens are not reused.
- If token generation or Zemer cipher resolution fails, M3 Play continues through its existing client fallback chain.
- Zemer token/session identifiers are not written to logs.

## Important limitations
- This source integration has not been compiled with Gradle or tested against live YouTube playback in this environment.
- Zemer's PoToken generator uses a synchronous public API internally; M3 wraps it on `Dispatchers.IO`.
- Runtime remote player configuration still depends on the upstream Zemer configuration endpoint being reachable.
- Client-specific YouTube restrictions, login requirements, and availability still apply; integrating the cipher fallback does not make every client usable for every video.
- Review the included GPL-3.0 license and attribution before redistributing.

## Verification required
Run:
- `./gradlew :zemercipher:assembleRelease`
- `./gradlew :app:compileArm64ReleaseKotlin`
- `./gradlew :app:lintVitalAnalyzeRelease`

Then test representative clients separately: WEB_REMIX, WEB, WEB_CREATOR, TVHTML5, IOS, ANDROID_MUSIC, and ANDROID_VR. Confirm both ciphered and direct-URL formats, metered/unmetered networks, proxy-enabled playback, and WebView renderer recovery.

## Follow-up integration changes
- WEB_REMIX initially received Zemer STS and Web PoTokens, with NewPipe-first URL resolution and Zemer fallback.
- STS alignment is now applied to all player clients so the request timestamp matches Zemer's current player.js.
- Cipher fallback now applies to every active client, not only WEB_REMIX.
- Web PoTokens are applied only to clients identified by the InnerTube layer as requiring Service Integrity tokens; native Android/iOS clients are excluded.
- Cancellation is rethrown instead of being swallowed by fallback handling.

## Glossy-aligned playback integration (2026-11)

- Zemer is attempted first for `signatureCipher` / `cipher` formats; NewPipe remains a fallback.
- Zemer n-parameter transformation is applied consistently to direct, Zemer-deciphered, and NewPipe-resolved URLs.
- Zemer player.js/WebView is prewarmed after deferred proxy preferences are applied; failure is non-fatal and lazy initialization remains available.
- The stream fallback chain retains M3's broader supported client set (iOS, mobile, Android Music/VR variants, Creator, TestSuite, Unplugged, iPadOS, VisionOS, TVHTML5, embedded TV, Web, Web Creator, Web Remix). Unsupported client versions from Glossy's separate Innertube fork are not fabricated.
- Web/TV PoToken eligibility remains restricted to the clients listed in `requiresWebPoToken`; Android/iOS clients do not receive Web PoTokens.
