# Glossy-inspired Settings UI migration — iteration 3

This iteration applies a consistent Glossy-inspired grouped-card visual language
across M3 Play settings screens while retaining M3 Play's own state, preference
keys, navigation, and business logic.

## Shared components
- `ui/component/Material3SettingsGroup.kt`: settings groups now use a continuous
  rounded surface with subtle inset separators and consistent section labels.
- `ui/screens/settings/SettingsLayout.kt`: home categories use grouped cards,
  refined row/icon spacing, a cleaner title, and adjusted search spacing.
- `ui/screens/settings/SettingsDimensions.kt`: shared settings dimensions aligned
  to the new grouped-card language.

## Individual screens with visual-token updates
IntegrationScreen, AccountSettings, ChangelogScreen, ThemeCreatorScreen,
PoTokenScreen, BackupAndRestore, PalettePickerScreen, LastFMSettings,
CustomizeBackground, DiscordExperimental, PrivacySettings, ContentSettings,
UpdateScreen, AppearanceSettings, DebugSettings, MusicTogetherScreen,
DiscordSettings, StorageSettings, and PlayerSettings.

Changes are deliberately limited to layout/surface styling tokens (card corner
radii and neutral surface fills) in these screens. Existing setting actions,
preference bindings, dialogs, and navigation logic were not intentionally
rewritten.

## Not a full one-to-one transplant
Some M3 Play-specific screens (Spotify/Discord login, Font Settings, ListenBrainz,
network permission, and other integration helpers) do not have direct equivalents
in Glossy. Their functionality remains M3 Play-specific. This is a staged visual
adaptation, not a pixel-perfect copy of every Glossy screen.

## Build verification
The Android compile task could not run because the Gradle 9.8 distribution could
not be downloaded in this environment (network/DNS unavailable). Build and
device testing are still required before release.
