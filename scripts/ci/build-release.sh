#!/usr/bin/env bash
# ANDROID_SIGNING_KEY: Base64 keystore text, or a GitLab File variable
# containing either the Base64 text or the binary keystore.
set -euo pipefail
set +x

for name in ANDROID_SIGNING_KEY ANDROID_KEYSTORE_PASSWORD ANDROID_KEY_ALIAS ANDROID_KEY_PASSWORD ANDROID_HOME; do
  if [[ -z "${!name:-}" ]]; then
    printf 'Missing required variable: %s. Check CI/CD variables and protected branch/tag settings.\n' "$name" >&2
    exit 1
  fi
done

signing_dir="$(mktemp -d)"
trap 'rm -rf "$signing_dir"' EXIT
keystore="$signing_dir/release.keystore"

if [[ -f "$ANDROID_SIGNING_KEY" ]]; then
  if ! base64 --decode "$ANDROID_SIGNING_KEY" > "$keystore" 2>/dev/null; then
    cp "$ANDROID_SIGNING_KEY" "$keystore"
  fi
elif ! printf '%s' "$ANDROID_SIGNING_KEY" | base64 --decode > "$keystore"; then
  echo 'ANDROID_SIGNING_KEY must contain Base64 keystore data or be a GitLab File variable.' >&2
  exit 1
fi

# Validate the store password and alias before starting the expensive build.
keytool -list -keystore "$keystore" \
  -storepass:env ANDROID_KEYSTORE_PASSWORD -alias "$ANDROID_KEY_ALIAS" > /dev/null

# Match the existing GitHub workflow: build unsigned, then sign with apksigner.
# R8 needs a larger heap. Keep Kotlin in this JVM to fit an 8 GB runner.
bash ./gradlew --no-daemon --console=plain --max-workers=2 \
  '-Dorg.gradle.jvmargs=-Xmx4096M -Dfile.encoding=UTF-8 -Djava.awt.headless=true -XX:+UseParallelGC' \
  -Pkotlin.compiler.execution.strategy=in-process \
  :app:assembleUniversalRelease

build_tools="$ANDROID_HOME/build-tools/37.0.0"
output_dir="app/build/outputs/apk/universal/release"
unsigned_apk="$output_dir/app-universal-release-unsigned.apk"
signed_apk="$output_dir/M3Play-release.apk"

"$build_tools/zipalign" -f -P 16 4 "$unsigned_apk" "$signing_dir/aligned.apk"
"$build_tools/apksigner" sign \
  --ks "$keystore" \
  --ks-key-alias "$ANDROID_KEY_ALIAS" \
  --ks-pass env:ANDROID_KEYSTORE_PASSWORD \
  --key-pass env:ANDROID_KEY_PASSWORD \
  --out "$signed_apk" \
  "$signing_dir/aligned.apk"
"$build_tools/apksigner" verify --verbose "$signed_apk"
"$build_tools/zipalign" -c -P 16 4 "$signed_apk"
