#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
WRAPPER_PROPS="$ROOT_DIR/gradle/wrapper/gradle-wrapper.properties"
DIST_URL="$(awk -F= '/^distributionUrl=/{print $2}' "$WRAPPER_PROPS" | sed 's#\\:#:#g')"
ZIP_NAME="$(basename "$DIST_URL")"
DIST_KEY="${ZIP_NAME%.zip}"
DIST_DIR_BASE="$HOME/.gradle/wrapper/dists/$DIST_KEY"
mkdir -p "$DIST_DIR_BASE"
( cd "$ROOT_DIR" && ./gradlew --version >/dev/null 2>&1 ) || true
HASH_DIR="$(find "$DIST_DIR_BASE" -mindepth 1 -maxdepth 1 -type d | head -n 1 || true)"
if [[ -z "$HASH_DIR" ]]; then
  HASH_DIR="$DIST_DIR_BASE/localseed"
  mkdir -p "$HASH_DIR"
fi
GRADLE_BIN="$(readlink -f "$(command -v gradle)")"
GRADLE_HOME="$(cd "$(dirname "$GRADLE_BIN")/.." && pwd)"
UNZIPPED_DIR_NAME="${DIST_KEY%-bin}"
TARGET_DIR="$HASH_DIR/$UNZIPPED_DIR_NAME"
rm -rf "$TARGET_DIR"
mkdir -p "$TARGET_DIR"
cp -a "$GRADLE_HOME"/. "$TARGET_DIR"/
rm -f "$HASH_DIR/$ZIP_NAME.part" "$HASH_DIR/$ZIP_NAME.lck"
: > "$HASH_DIR/$ZIP_NAME.ok"
echo "Seeded wrapper distribution at: $TARGET_DIR"
