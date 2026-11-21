# Apple Music-inspired playlist top bar update

Changes in `OnlinePlaylistScreen.kt`:
- Uses `CenterAlignedTopAppBar` so the collapsed playlist title is centered like the supplied reference.
- Uses white navigation/title/action icon colors over a palette-derived, less surface-washed background.
- Removes circular filled backgrounds from top-bar icons for the flatter Apple Music-like placement.
- Adds a Share playlist action using the YouTube Music playlist URL while retaining Search and the existing overflow menu.
- Keeps the existing palette extraction, playlist actions, playback, list and mini-player code intact.

Validation: source-level checks only. A Gradle build and device screenshot comparison were not run in this environment.
