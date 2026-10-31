# ArchiveTune lyrics port — implementation status

## Implemented
- Registered 15 provider candidates: Unison, Binimum, SimpMusic, BetterLyrics, BetterLyrics QQ/QRC, Paxsenix main/Apple Music/Spotify/Musixmatch, Megalobiz, LRCLIB, KuGou, YouTube subtitles, YouTube Music, YouLy+.
- Concurrent fetch, per-provider timeout, candidate quality scoring, cache and de-duplication.
- Rich TTML and QRC parsing paths, word timestamps, translation/romanization metadata and secondary/background vocals.
- Corrected karaoke inactivity: real word end controls the line end; line-timed lyrics no longer receive fabricated word timestamps.
- Preferred-provider-only mode will not silently use another enabled provider.
- Best-result ranking prioritizes genuine word synchronization.

## Not verified
- Full ArchiveTune visual/behavioral parity is not established.
- Individual live provider endpoints/API credentials were not tested.
- Android compile was not verified: Gradle wrapper dependency download is blocked by network/DNS in this environment.
- Paxsenix endpoints depend on configured API credentials.

ArchiveTune-derived code is subject to its GPL-3.0 licensing and attribution obligations.
