#!/usr/bin/env bash
set -euo pipefail

# Sync this repository with Arturo254/OpenTune.
#
# Usage:
#   ./scripts_sync_opentune.sh [branch]
#   ./scripts_sync_opentune.sh --from-local /path/to/OpenTune
#   ./scripts_sync_opentune.sh --from-tar /path/to/OpenTune.tar.gz
#   ./scripts_sync_opentune.sh --from-zip /path/to/OpenTune.zip
#   ./scripts_sync_opentune.sh --dry-run --from-local /path/to/OpenTune

BRANCH="main"
MODE="remote"
SOURCE=""
DRY_RUN="false"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --from-local)
      MODE="local"
      SOURCE="${2:-}"
      shift 2
      ;;
    --from-tar)
      MODE="tar"
      SOURCE="${2:-}"
      shift 2
      ;;
    --from-zip)
      MODE="zip"
      SOURCE="${2:-}"
      shift 2
      ;;
    --dry-run)
      DRY_RUN="true"
      shift
      ;;
    *)
      BRANCH="$1"
      shift
      ;;
  esac
done

TMP_DIR="$(mktemp -d)"
cleanup() { rm -rf "$TMP_DIR"; }
trap cleanup EXIT

UPSTREAM_DIR="$TMP_DIR/OpenTune"
mkdir -p "$UPSTREAM_DIR"

err() { echo "ERROR: $*" >&2; }
info() { echo "[sync] $*"; }

clone_from_remote() {
  local repos=(
    "https://github.com/Arturo254/OpenTune.git"
    "https://gitclone.com/github.com/Arturo254/OpenTune.git"
  )

  for repo in "${repos[@]}"; do
    info "Attempting clone from: $repo"
    if git clone --depth 1 --branch "$BRANCH" "$repo" "$UPSTREAM_DIR"; then
      return 0
    fi
  done

  err "Unable to clone OpenTune from remote mirrors."
  err "Tip: use --from-local, --from-tar, or --from-zip in restricted environments."
  return 1
}

extract_archive_root() {
  local unpack_dir="$1"
  local candidate

  candidate="$(find "$unpack_dir" -mindepth 1 -maxdepth 1 -type d | head -n 1 || true)"
  if [[ -z "$candidate" ]]; then
    err "Archive did not contain a top-level directory."
    return 1
  fi

  rsync -a "$candidate/" "$UPSTREAM_DIR/"
}

if [[ "$MODE" == "remote" ]]; then
  clone_from_remote
elif [[ "$MODE" == "local" ]]; then
  if [[ -z "$SOURCE" || ! -d "$SOURCE" ]]; then
    err "--from-local requires a valid directory path."
    exit 1
  fi
  rsync -a "$SOURCE/" "$UPSTREAM_DIR/"
elif [[ "$MODE" == "tar" ]]; then
  if [[ -z "$SOURCE" || ! -f "$SOURCE" ]]; then
    err "--from-tar requires a valid archive path."
    exit 1
  fi
  local_unpack="$TMP_DIR/unpack"
  mkdir -p "$local_unpack"
  tar -xf "$SOURCE" -C "$local_unpack"
  extract_archive_root "$local_unpack"
elif [[ "$MODE" == "zip" ]]; then
  if [[ -z "$SOURCE" || ! -f "$SOURCE" ]]; then
    err "--from-zip requires a valid archive path."
    exit 1
  fi
  local_unpack="$TMP_DIR/unpack"
  mkdir -p "$local_unpack"
  unzip -q "$SOURCE" -d "$local_unpack"
  extract_archive_root "$local_unpack"
else
  err "Unsupported mode: $MODE"
  exit 1
fi

if [[ ! -f "$UPSTREAM_DIR/README.md" ]]; then
  err "Imported source does not look like a repository root (README.md missing)."
  err "Source used: $MODE ${SOURCE:-$BRANCH}"
fi

RSYNC_FLAGS=( -a --delete --exclude ".git" )
if [[ "$DRY_RUN" == "true" ]]; then
  RSYNC_FLAGS+=( --dry-run --itemize-changes )
  info "Running in dry-run mode (no files will be changed)."
fi

info "Mirroring OpenTune content into current repository..."
rsync "${RSYNC_FLAGS[@]}" "$UPSTREAM_DIR/" ./

if [[ "$DRY_RUN" == "true" ]]; then
  info "Dry-run complete."
else
  info "Sync complete. Review changes with: git status && git diff"
fi
