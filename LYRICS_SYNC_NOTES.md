# Lyrics integration notes

## Changes in this build

- The lyrics helper prefers a synchronized result (LRC, TTML, or QRC) over a plain-text result, while keeping plain text as a fallback if no provider returns synchronized lyrics.
- The UI recognizes QRC word-timed lyrics alongside LRC and TTML in the main lyrics screen, alternate lyrics screen, and provider-selection preview.
- LRC parsing accepts one-to-three digit minute fields, optional fractional timestamps, and fractional seconds with one, two, or three digits.
- Singer/agent metadata is preserved when parsed lyrics are passed to the lyrics UI.
- Paxsenix now initializes its user-agent and API key from build configuration. It is skipped when no key is configured instead of always making a request that cannot succeed.

## Paxsenix configuration

Paxsenix requires an API key. Add this to your local `local.properties` file (do not commit the key):

```properties
PAXSENIX_API_KEY=your_api_key_here
```

Alternatively, provide `PAXSENIX_API_KEY` as an environment variable when building. Without a key, the app skips the Paxsenix provider and continues to other sources.

## Build verification

The archive was checked for ZIP integrity. A Gradle compile could not be run in this environment because `services.gradle.org` was unreachable, so compile success is not claimed.
