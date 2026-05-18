#!/bin/bash
# Run this script BEFORE building the APK to download required binaries
# Requires: curl, wget

set -e

ASSETS_DIR="app/src/main/assets"
mkdir -p "$ASSETS_DIR"

echo "=========================================="
echo "  VideoDownloader - Binary Setup Script"
echo "=========================================="
echo ""

ARCH="${1:-arm64}"  # arm64 (default), arm, x86_64

echo "Target architecture: $ARCH"
echo ""

# ── yt-dlp ──────────────────────────────────────────────────────────────────
echo "[1/2] Downloading yt-dlp..."
YT_DLP_URL="https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp"
curl -L "$YT_DLP_URL" -o "$ASSETS_DIR/yt-dlp"
chmod +x "$ASSETS_DIR/yt-dlp"
echo "✓ yt-dlp downloaded ($(du -sh "$ASSETS_DIR/yt-dlp" | cut -f1))"

# ── aria2c ───────────────────────────────────────────────────────────────────
echo ""
echo "[2/2] Downloading aria2c for Android ($ARCH)..."

# Using aria2c static builds for Android from GitHub
if [ "$ARCH" = "arm64" ]; then
    ARIA2C_URL="https://github.com/P3TERX/aria2-static-build/releases/latest/download/aria2-aarch64-linux-android.zip"
elif [ "$ARCH" = "arm" ]; then
    ARIA2C_URL="https://github.com/P3TERX/aria2-static-build/releases/latest/download/aria2-arm-linux-androideabi.zip"
else
    ARIA2C_URL="https://github.com/P3TERX/aria2-static-build/releases/latest/download/aria2-x86_64-linux-android.zip"
fi

curl -L "$ARIA2C_URL" -o /tmp/aria2c.zip 2>/dev/null || {
    echo "⚠ aria2c download failed - will use yt-dlp built-in downloader instead"
    # Create empty placeholder
    echo "#!/bin/sh" > "$ASSETS_DIR/aria2c"
    chmod +x "$ASSETS_DIR/aria2c"
}

if [ -f /tmp/aria2c.zip ]; then
    unzip -o /tmp/aria2c.zip -d /tmp/aria2c_extracted/ 2>/dev/null || true
    ARIA2C_BIN=$(find /tmp/aria2c_extracted -name "aria2c" -type f 2>/dev/null | head -1)
    if [ -n "$ARIA2C_BIN" ]; then
        cp "$ARIA2C_BIN" "$ASSETS_DIR/aria2c"
        chmod +x "$ASSETS_DIR/aria2c"
        echo "✓ aria2c downloaded ($(du -sh "$ASSETS_DIR/aria2c" | cut -f1))"
    fi
    rm -rf /tmp/aria2c.zip /tmp/aria2c_extracted
fi

# ── ffmpeg ───────────────────────────────────────────────────────────────────
echo ""
echo "[Optional] Downloading ffmpeg for Android ($ARCH)..."
echo "  (ffmpeg is used for merging video+audio streams for 4K/2K)"

if [ "$ARCH" = "arm64" ]; then
    FFMPEG_URL="https://github.com/eugenesan/ffmpeg-android/releases/latest/download/ffmpeg-aarch64"
else
    FFMPEG_URL=""
fi

if [ -n "$FFMPEG_URL" ]; then
    curl -L "$FFMPEG_URL" -o "$ASSETS_DIR/ffmpeg" 2>/dev/null || {
        echo "⚠ ffmpeg not available via this URL - 4K merging may not work"
        echo "  You can manually place an ARM64 ffmpeg binary at: $ASSETS_DIR/ffmpeg"
        touch "$ASSETS_DIR/ffmpeg"
    }
    chmod +x "$ASSETS_DIR/ffmpeg" 2>/dev/null || true
else
    echo "  Skipping ffmpeg for this arch. Place manually at $ASSETS_DIR/ffmpeg if needed."
    touch "$ASSETS_DIR/ffmpeg"
fi

echo ""
echo "=========================================="
echo "  Setup complete!"
echo ""
echo "  Files in $ASSETS_DIR:"
ls -lh "$ASSETS_DIR/"
echo ""
echo "  Now open the project in Android Studio"
echo "  and click: Build → Build Bundle(s)/APK(s) → Build APK(s)"
echo "=========================================="
