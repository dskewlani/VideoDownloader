# 📱 VideoDownloader - Ultimate Android Video Downloader

A feature-rich Android app to download 4K/2K/1080p60/HDR videos from 1000+ sites.

## ✨ Features
- 🔢 Serial-numbered download queue
- ⚡ Maximum speed (aria2c · 16 parallel connections)
- 🎯 4K 60fps · 2K · 1080p60 · HDR priority
- 📺 Built-in ExoPlayer with gesture controls
- 🖼️ Picture-in-Picture (PiP) mode
- 📋 Clipboard auto-detect
- 🌙 AMOLED dark theme
- 📊 Download history with thumbnails
- 🔁 Auto-retry on failure
- 📤 Share video URL directly into app

## 🛠️ Build Instructions (5 Minutes)

### Step 1: Install Android Studio
Download free from: https://developer.android.com/studio
Install with default settings (includes Android SDK).

### Step 2: Download Binaries
Open Terminal in the project folder and run:

```bash
chmod +x setup_binaries.sh
./setup_binaries.sh arm64
```

This downloads yt-dlp + aria2c + ffmpeg into `app/src/main/assets/`.

> **Windows users:** Run these manually:
> 1. Download `yt-dlp.exe` from https://github.com/yt-dlp/yt-dlp/releases
>    → Rename to `yt-dlp` → Place in `app/src/main/assets/`
> 2. Download aria2c for Android ARM64 from https://github.com/P3TERX/aria2-static-build
>    → Rename to `aria2c` → Place in `app/src/main/assets/`
> 3. Create empty file named `ffmpeg` in `app/src/main/assets/` (optional)

### Step 3: Open in Android Studio
1. Open Android Studio
2. Click **"Open"** → Select this `VideoDownloader` folder
3. Wait for Gradle sync to complete (~2-3 min first time)

### Step 4: Build APK
```
Build menu → Build Bundle(s)/APK(s) → Build APK(s)
```
APK will be at: `app/build/outputs/apk/debug/app-debug.apk`

### Step 5: Install on Phone
- Enable **"Install from Unknown Sources"** in phone Settings
- Transfer APK to phone and tap to install
- OR connect phone via USB and click **Run** (▶) in Android Studio

## 📲 Supported Sites
YouTube, YouTube Shorts, Instagram Reels, TikTok, Twitter/X, Facebook,
Vimeo, Reddit, Dailymotion, Twitch, SoundCloud, and 1000+ more via yt-dlp.

## 📁 Download Location
Videos saved to: `Downloads/VideoDownloader/` on your phone.

Filename format: `VD_1_VideoTitle_4K60fps.mp4`

## ⚡ Speed Tips
- Uses aria2c with 16 parallel connections per video
- Up to 3 videos download simultaneously
- Downloads resume automatically if interrupted
- Speed depends on your internet connection and the source server

## 🎮 Player Controls
| Gesture | Action |
|---------|--------|
| Single tap | Show/hide controls |
| Double-tap left | Seek back 10s |
| Double-tap right | Seek forward 10s |
| Speed button | 0.25x - 2x playback |
| PiP button | Picture-in-Picture |
| Fullscreen | Landscape mode |

## 📋 Requirements
- Android 8.0+ (API 26)
- Internet connection
- ~100MB free storage (for app + binaries)
- ARM64 device (most modern phones)

## ⚠️ Notes
- For personal use only; respect copyright laws
- Some sites may block downloading
- 4K downloads require ffmpeg for stream merging
- First download may be slower (yt-dlp fetching info)
