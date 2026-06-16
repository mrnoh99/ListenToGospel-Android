#!/usr/bin/env bash
# Configures keystore.properties for the iCloud release keystore and builds signed AAB.
#
#   ./scripts/use_listentogospel_keystore.sh
#   ./scripts/use_listentogospel_keystore.sh --configure-only
#   KEYSTORE_PATH=... ./scripts/use_listentogospel_keystore.sh

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

CONFIGURE_ONLY=false
KEYSTORE_PATH="${KEYSTORE_PATH:-$HOME/Library/Mobile Documents/com~apple~CloudDocs/AppDevelop/KeyStoreFile/listentogospel-release.jks}"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --configure-only) CONFIGURE_ONLY=true; shift ;;
    *) echo "Unknown option: $1" >&2; exit 1 ;;
  esac
done

if [[ ! -f "$KEYSTORE_PATH" ]]; then
  echo "Keystore not found: $KEYSTORE_PATH" >&2
  echo "Check iCloud Drive sync or set KEYSTORE_PATH." >&2
  exit 1
fi

if [[ -x "/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/keytool" ]]; then
  KEYTOOL="/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/keytool"
else
  KEYTOOL="$(command -v keytool)"
fi

read -r -s -p "Keystore password: " STORE_PASSWORD
echo
KEY_PASSWORD="${KEY_PASSWORD:-$STORE_PASSWORD}"

echo "Listing key aliases in keystore..."
if ! "$KEYTOOL" -list -keystore "$KEYSTORE_PATH" -storepass "$STORE_PASSWORD"; then
  echo "Wrong password or keystore error." >&2
  exit 1
fi

read -r -p "Enter keyAlias from the list above (e.g. upload or key0): " ALIAS

cat > keystore.properties <<EOF
storeFile=$KEYSTORE_PATH
storePassword=$STORE_PASSWORD
keyAlias=$ALIAS
keyPassword=$KEY_PASSWORD
EOF

echo "Wrote keystore.properties (gitignored)"
echo "storeFile=$KEYSTORE_PATH"
echo "keyAlias=$ALIAS"

if $CONFIGURE_ONLY; then
  echo
  echo "Next: ./scripts/build_release_aab.sh"
  exit 0
fi

exec "$ROOT/scripts/build_release_aab.sh"
