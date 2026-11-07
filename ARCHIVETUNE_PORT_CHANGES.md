# ArchiveTune lyrics integration — change log

ArchiveTune's 15 provider entries are registered in M3's LyricsHelper:
Unison, Binimum, SimpMusic, BetterLyrics, BetterLyrics Portato,
Paxsenix, Paxsenix Apple Music, Paxsenix Spotify, Paxsenix Musixmatch,
Megalobiz, LRCLIB, KuGou, YouTube Subtitles, YouTube Lyrics, YouLy+.
Paxsenix variants live together in PaxsenixVariantLyricsProviders.kt.

New in this revision:
- Unison getAllLyrics now returns alternate results using the Unison library API.
- LyricsV2 preserves only real word timing (no fake word timing for line-only LRC).
- Real word-synced karaoke lines stop being active at the final word end time.
- Background/secondary singer handling is preserved.

Limits: not proven 1:1 ArchiveTune parity. Provider network functionality
and Android Gradle compilation have not been verified in this environment.
ArchiveTune adaptations are subject to GPL-3.0 attribution/license requirements.
