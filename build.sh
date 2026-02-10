#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SRC_DIR="$SCRIPT_DIR/src"
OUT_DIR="$SCRIPT_DIR/out"

echo "Compiling..."
rm -rf "$OUT_DIR"
mkdir -p "$OUT_DIR"
javac -d "$OUT_DIR" "$SRC_DIR"/tasktracker/*.java

echo "Build successful → $OUT_DIR"
