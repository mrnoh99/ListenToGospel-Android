#!/usr/bin/env bash
# Shortens all chapter M4A files to ~N seconds for testing auto-advance between chapters.
# Originals are kept in .audio-backup/ at the repo root (gitignored).
#
#   ./scripts/audio_test_mode.sh enable
#   ./scripts/audio_test_mode.sh restore
#   ./scripts/audio_test_mode.sh status
#   ./scripts/audio_test_mode.sh enable 8

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
AUDIO_DIR="$ROOT/app/src/main/assets/AudioFiles"
BACKUP_DIR="$ROOT/.audio-backup"
MARKER_FILE="$ROOT/.audio-test-mode"
SECONDS="${2:-10}"

require_ffmpeg() {
  if ! command -v ffmpeg >/dev/null 2>&1; then
    echo "Error: ffmpeg not found. Install ffmpeg and retry." >&2
    exit 1
  fi
}

count_m4a() {
  find "$1" -type f -name '*.m4a' 2>/dev/null | wc -l | tr -d ' '
}

status() {
  local mode="NORMAL (or unknown)"
  if [[ -f "$MARKER_FILE" ]]; then
    mode="TEST (short clips)"
  fi
  echo "Mode: $mode"
  echo "Active audio: $(count_m4a "$AUDIO_DIR") file(s) in app/src/main/assets/AudioFiles"
  echo "Backup:       $(count_m4a "$BACKUP_DIR") file(s) in .audio-backup"
  if [[ -f "$MARKER_FILE" ]]; then
    echo "Marker:"
    cat "$MARKER_FILE"
  fi
}

enable_short() {
  require_ffmpeg

  if [[ ! -d "$AUDIO_DIR" ]]; then
    echo "Error: Audio folder not found: $AUDIO_DIR" >&2
    echo "Run ./copy_audio_assets.sh first." >&2
    exit 1
  fi

  local active_count
  active_count="$(count_m4a "$AUDIO_DIR")"
  if [[ "$active_count" == "0" ]]; then
    echo "Error: No .m4a files under $AUDIO_DIR" >&2
    exit 1
  fi

  if [[ ! -d "$BACKUP_DIR" ]]; then
    echo "Backing up $active_count file(s) to .audio-backup ..."
    mkdir -p "$BACKUP_DIR"
    cp -R "$AUDIO_DIR/." "$BACKUP_DIR/"
  else
    echo "Using existing .audio-backup (originals not re-copied)."
  fi

  local backup_count
  backup_count="$(count_m4a "$BACKUP_DIR")"
  if [[ "$backup_count" == "0" ]]; then
    echo "Error: Backup folder is empty: $BACKUP_DIR" >&2
    exit 1
  fi

  echo "Creating ~${SECONDS}s test clips from backup ($backup_count files) ..."
  local index=0
  while IFS= read -r -d '' src; do
    index=$((index + 1))
    rel="${src#"$BACKUP_DIR"/}"
    dest="$AUDIO_DIR/$rel"
    mkdir -p "$(dirname "$dest")"
    tmp="${dest}.tmp.m4a"
    rm -f "$tmp"
    ffmpeg -hide_banner -loglevel error -y \
      -i "$src" \
      -t "$SECONDS" \
      -c:a aac -b:a 96k -movflags +faststart \
      "$tmp"
    mv -f "$tmp" "$dest"
    echo "  [$index/$backup_count] $rel"
  done < <(find "$BACKUP_DIR" -type f -name '*.m4a' -print0)

  {
    echo "enabled_at=$(date -Iseconds)"
    echo "seconds=$SECONDS"
    echo "file_count=$backup_count"
  } >"$MARKER_FILE"

  echo ""
  echo "Done. Test mode ON (~${SECONDS}s per chapter)."
  echo "Rebuild/run the app, then test chapter auto-advance."
  echo "Restore originals: ./scripts/audio_test_mode.sh restore"
}

restore_full() {
  if [[ ! -d "$BACKUP_DIR" ]]; then
    echo "Error: No backup found at $BACKUP_DIR" >&2
    exit 1
  fi

  local backup_count
  backup_count="$(count_m4a "$BACKUP_DIR")"
  if [[ "$backup_count" == "0" ]]; then
    echo "Error: Backup folder has no .m4a files." >&2
    exit 1
  fi

  echo "Restoring $backup_count original file(s) ..."
  mkdir -p "$AUDIO_DIR"
  cp -R "$BACKUP_DIR/." "$AUDIO_DIR/"
  rm -f "$MARKER_FILE"
  echo "Done. Full audio restored."
  echo "Optional: rm -rf .audio-backup to free disk space"
}

case "${1:-}" in
  enable) enable_short ;;
  restore) restore_full ;;
  status) status ;;
  *)
    echo "Usage:"
    echo "  ./scripts/audio_test_mode.sh enable [seconds]"
    echo "  ./scripts/audio_test_mode.sh restore"
    echo "  ./scripts/audio_test_mode.sh status"
    exit 1
    ;;
esac
