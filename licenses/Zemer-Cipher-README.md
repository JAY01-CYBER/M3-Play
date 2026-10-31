# zemer-cipher

Android library for YouTube cipher deobfuscation and PoToken generation.

**Scope:** this library deciphers web clients' sig/n and mints poTokens. Which stream clients are
chosen, and how each is tested for whole-song delivery past the CDN's 1-MiB free window, lives in
[`zemer-app`](https://github.com/ZemerTeam/zemer-app) (`YTPlayerUtils` + `tests/client-fulldownload.mjs`).

## Origin

The WebView signature-cipher / n-transform deciphering here (`CipherDeobfuscator`, `CipherWebView`,
the injected `window._cipherSigFunc`) was **originally written by me
([alltechdev](https://github.com/alltechdev))** - first implemented into [`zemer-app`](https://github.com/ZemerTeam/zemer-app) on
**2026-02-12**
([`f905d49`](https://github.com/ZemerTeam/zemer-app/commit/f905d49da8b4486b659fa32d68e2f45f939fb56a)).
This repository is that same code, extracted into a standalone Android library
(`com.zemer:cipher`).

**Remote config:** [`zemer-app`](https://github.com/ZemerTeam/zemer-app) fetches `player_configs.json`
from this repo's `master` at runtime (via `PlayerConfigStore`) to self-heal YouTube player rotations
without an app update.

## Features

- Signature cipher deobfuscation for YouTube streaming URLs
- N-parameter transformation to avoid throttling
- PoToken generation using BotGuard
- **Remote-updatable player configs** - a config pushed to this repo's `master` fixes
  deployed apps within minutes, no APK release needed

## Player configs (`library/src/main/assets/player_configs.json`)

YouTube rotates its `player_ias` JS frequently; each rotation needs a per-player config
(sig call expression, n-transform URL class, signatureTimestamp). All configs live in
**one JSON file**, which is:

1. **Bundled** in the APK as the offline default,
2. **Fetched at runtime** by `PlayerConfigStore` from this repo's raw `master` URL
   (6 h TTL + ETag), and **force-refreshed the moment an unknown player breaks
   deciphering** - so pushing a new entry to `master` is the deploy,
3. Read by the `zemer-app` test harness - app, devices, and tests cannot drift apart.

### Entry shape (schemaVersion 1)

```json
"445213fb": { "sig": "mP(4,155,INPUT)", "nClass": "Yx", "sts": 20613, "aliases": ["d62bd338"] }
```

- key - the 8-hex player hash from the player JS URL; `aliases` - the md5-of-first-10000-bytes
  fallback hash
- `sig` - the signature deobfuscation call, locked to `name(int,int,INPUT)`
- `nClass` - the URL class for the n-transform IIFE (built from a local template)
- `sts` - the player's signatureTimestamp

### Adding a rotated player

1. In `zemer-app`: `node tests/validate-player-config.mjs <hash>` - deciphers a real stream
   and checks the CDN returns **HTTP 206** (the only ground truth; multiple constant pairs
   can "decipher" while only one is accepted). It prints a paste-ready JSON entry.
2. Add the entry to `player_configs.json` here. Duplicate hashes/aliases reject the whole
   file - run the unit tests.
3. Push to `master` - deployed apps self-heal from that URL within minutes.
4. Bump the submodule pointer in `zemer-app` afterwards (bundled defaults stay fresh).

### Automated rotation

The steps above also run automatically. `.github/workflows/player-monitor.yml` watches for
`player_ias` rotations, derives each unknown config, validates it against the live CDN, and
(when enabled) commits it straight to `master`, so a rotation deploys without anyone editing
the file by hand. The deriver and validators live in `tools/` (`propose-config.mjs`,
`verify-entry.mjs`, `apply-entry.mjs`).

The pipeline is fail-closed. A hash reaches a commit only after two independent HTTP 206
checks (`propose-config`, then `verify-entry`) and a parser-parity gate, and the untrusted
player JS is evaluated only in an isolated job that holds no write credential. After the push
it re-reads the deployed file over git protocol and reverts only a commit it can prove is bad.

Deploy is gated by the `AUTO_DEPLOY_CONFIG` repository variable, which is also the kill
switch: unset validates and alerts only (writing nothing), `branch` commits to the branch the
run was triggered from, and `master` commits to `master` (the live deploy). The manual steps
above remain the fallback for adding a config by hand.

### Safety model

`PlayerConfigParser` is the validation boundary: every value is regex-locked so remote data
can never inject free-form JS into the cipher WebView. Invalid entries are skipped; invalid
files (including hash/alias collisions) are rejected wholesale and devices keep their
last-good table. Bump `schemaVersion` **only** on breaking shape changes - older apps reject
newer schema files and keep working from their last-good table.

Run the tests with `./gradlew :library:testDebugUnitTest`. The `config-parity/` fixtures are
shared with the `zemer-app` harness: file-level accept/reject verdicts (and the n-IIFE
template) are pinned byte-for-byte across both readers.

## Usage

### Initialization

```kotlin
// Initialize in your Application class
ZemerCipher.initialize(
    context = applicationContext,
    proxy = yourProxy,  // optional
    debugLogging = BuildConfig.DEBUG  // optional
)
```

### Cipher Deobfuscation

```kotlin
// Deobfuscate a signature cipher URL
val deobfuscatedUrl = CipherDeobfuscator.deobfuscateStreamUrl(signatureCipher, videoId)

// Transform n-parameter in URL
val transformedUrl = CipherDeobfuscator.transformNParamInUrl(url)
```

### PoToken Generation

```kotlin
val generator = PoTokenGenerator()
val result = generator.getWebClientPoToken(videoId, sessionId)
// result.playerRequestPoToken - for player requests
// result.streamingDataPoToken - for streaming data requests
```

## Credits

Almost all of this library is original work, written into `zemer-app` and extracted here
(see Origin above): the WebView signature-cipher and n-transform deciphering, the runtime
execution and script injection, the n-parameter transform logic, and the remote-updatable
config system that lets it self-heal (the `player_configs.json` schema, `PlayerConfigParser`,
`PlayerConfigStore`). The config deriver and live HTTP 206 validator that feed it live in
`zemer-app`. MetrolistGroup's
[`faraday`](https://github.com/MetrolistGroup/faraday) ports this config model, deriver, and
validator, and credits [`zemer-cipher`](https://github.com/ZemerTeam/zemer-cipher) and
[`zemer-app`](https://github.com/ZemerTeam/zemer-app) in its README. MetrolistGroup's
[`innertubex`](https://github.com/MetrolistGroup/innertubex) ships a port of the cipher solver
(its `ZemerCipherSolver` class) and lists Zemer among its cipher deobfuscation paths.

The automated CI rotation system (watch a `player_ias` rotation, then derive, live-validate, and
commit the config automatically) was built first by faraday in July 2026. The `tools/` pipeline
here is zemer-cipher's own later implementation of that idea (August 2026) on top of this format;
because devices already self-heal from `master`, a commit here is also the live deploy.

Two narrow pieces build on prior work and are credited here:

- The BotGuard poToken client follows [BgUtils](https://github.com/LuanRT/BgUtils)
  (MIT License) patterns.
- Reading YouTube's signature and n functions out of the player is a known deobfuscation
  technique, also documented by [yt-dlp](https://github.com/yt-dlp/yt-dlp),
  [NewPipe](https://github.com/TeamNewPipe/NewPipe), and others.

## License

GPL-3.0
