# Accord icon port into M3 Play

Ported 28 matching drawable vector assets from Accord-alpha into existing M3 Play drawable resource names.

The port preserves M3 Play resource names so existing layout/code references continue to resolve.
Vector colors are theme-aware via the platform textColorPrimary attribute or are left to the existing ImageView tint.

Playback transport controls intentionally NOT changed:
- play.xml
- pause.xml
- skip_next.xml
- skip_previous.xml
- apple_play.xml
- apple_pause.xml
- apple_next.xml
- apple_prev.xml
