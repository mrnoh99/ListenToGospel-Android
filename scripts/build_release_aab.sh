#!/usr/bin/env bash
# Builds a signed release App Bundle (.aab) for Google Play.
# Requires: keystore.properties and audio assets in audioPack.

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

if [[ ! -f keystore.properties ]]; then
  echo "Missing keystore.properties. Run first:" >&2
  echo "  ./scripts/use_listentogospel_keystore.sh" >&2
  exit 1
fi

AUDIO_DIR="audioPack/src/main/assets/AudioFiles"
if [[ ! -d "$AUDIO_DIR" ]]; then
  echo "Missing $AUDIO_DIR — copy audio before release build." >&2
  exit 1
fi

M4A_COUNT="$(find "$AUDIO_DIR" -name '*.m4a' -type f | wc -l | tr -d ' ')"
if [[ "$M4A_COUNT" -lt 89 ]]; then
  echo "Warning: expected 89 .m4a files, found $M4A_COUNT"
fi

if [[ -x "/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/java" ]]; then
  export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
  export PATH="$JAVA_HOME/bin:$PATH"
fi

echo "Building signed release bundle..."
./gradlew bundleRelease --no-daemon

BUNDLE="$(find app/build/outputs/bundle/release -name '*.aab' -type f | head -1)"
if [[ -n "$BUNDLE" ]]; then
  SIZE_MB="$(python3 -c "import os; print(f'{os.path.getsize(\"$BUNDLE\") / (1024*1024):.2f}')")"
  echo
  echo "SUCCESS: $BUNDLE"
  echo "Size MB: $SIZE_MB"
  echo "Upload this file to Play Console -> App bundle"
else
  echo "Build finished but .aab not found under app/build/outputs/bundle/release" >&2
  exit 1
fi
