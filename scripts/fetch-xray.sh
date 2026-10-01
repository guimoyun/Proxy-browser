#!/bin/bash
# 下载 Xray Android arm64-v8a 二进制并放入 assets（可重复执行）
set -e
PROJ="$(cd "$(dirname "$0")/.." && pwd)"
DEST="$PROJ/app/src/main/assets/xray"
mkdir -p "$DEST"
URL="https://github.com/XTLS/Xray-core/releases/download/v1.8.24/Xray-android-arm64-v8a.zip"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
echo ">> $URL"
curl -fsSL -o "$TMP/xray.zip" "$URL"
cd "$TMP" && unzip -oq xray.zip
BIN="$(find "$TMP" -type f -name xray | head -1)"
if [ -z "$BIN" ]; then echo "未找到 xray 可执行文件"; exit 1; fi
cp "$BIN" "$DEST/xray"
chmod +x "$DEST/xray"
echo "OK: $DEST/xray ($(du -h "$DEST/xray" | cut -f1))"
