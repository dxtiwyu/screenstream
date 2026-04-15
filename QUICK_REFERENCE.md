# ScreenStream - Quick Reference

## 📦 APK Location
```
app/build/outputs/apk/FDroid/debug/app-FDroid-debug.apk
```

## 🚀 Quick Start Commands

### Install APK
```bash
adb install app/build/outputs/apk/FDroid/debug/app-FDroid-debug.apk
```

### Start App & Begin Streaming (One-Liner)
```bash
adb shell "am start -n info.dvkr.screenstream/.SingleActivity --ez AUTO_START_STREAMING true && sleep 2 && am startservice -n info.dvkr.screenstream/info.dvkr.screenstream.tile.TileActionService"
```

### Using the Script
```bash
# From computer via ADB
adb push START_STREAMING.sh /sdcard/
adb shell "sh /sdcard/START_STREAMING.sh"

# Directly on Android device
sh /sdcard/START_STREAMING.sh
```

### Stop Streaming
```bash
adb shell am force-stop info.dvkr.screenstream
```

## 🔧 Essential Commands

| Action | Command |
|--------|---------|
| **Install** | `adb install app-FDroid-debug.apk` |
| **Start App** | `adb shell am start -n info.dvkr.screenstream/.SingleActivity` |
| **Stop App** | `adb shell am force-stop info.dvkr.screenstream` |
| **Grant Permissions** | `adb shell pm grant info.dvkr.screenstream android.permission.SYSTEM_ALERT_WINDOW` |
| **View Logs** | `adb logcat \| grep ScreenStream` |
| **Get Device IP** | `adb shell ip addr show wlan0 \| grep inet` |
| **Check Status** | `adb shell ps \| grep screenstream` |

## 🌐 Default Stream URLs

- **MJPEG:** `http://<device-ip>:8080`
- **RTSP:** `rtsp://<device-ip>:5540/stream`

## ⚡ Build Commands

```bash
# F-Droid (Ad-free)
./gradlew :app:assembleFDroidDebug

# Play Store (with ads)
./gradlew :app:assemblePlayStoreDebug
```

## 🐛 Troubleshooting

**App won't start streaming?**
```bash
# Disable battery optimization
adb shell dumpsys deviceidle whitelist +info.dvkr.screenstream

# Grant overlay permission
adb shell appops set info.dvkr.screenstream SYSTEM_ALERT_WINDOW allow
```

**Check if running:**
```bash
adb shell dumpsys activity services | grep screenstream
```

**View errors:**
```bash
adb logcat *:E | grep screenstream
```

## 📝 Package Info

- **Package:** `info.dvkr.screenstream`
- **Main Activity:** `info.dvkr.screenstream.SingleActivity`
- **Version:** 4.3.7 (43007)
- **Min SDK:** 23 (Android 6.0)
- **Target SDK:** 36

## ✨ Recent Fixes Applied

1. ✅ **MediaCodec Buffer Leak** - Fixed crash after few minutes
2. ✅ **WiFi Auto-Reconnect** - No manual restart needed when WiFi reconnects
3. ✅ **Infinite Retry** - Keeps trying to find IP address with exponential backoff
