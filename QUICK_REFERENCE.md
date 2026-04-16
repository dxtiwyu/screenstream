# Quick Reference - ScreenStream Commands

## 🚀 Silent Start (After First Setup)

### Fastest (No UI)
```bash
adb shell am startforegroundservice info.dvkr.screenstream.mjpeg.MjpegModuleService
```

### With Script
```bash
adb push START_STREAMING_DIRECT.sh /sdcard/
adb shell "sh /sdcard/START_STREAMING_DIRECT.sh"
```

### Full Auto-Start
```bash
adb push START_STREAMING.sh /sdcard/
adb shell "sh /sdcard/START_STREAMING.sh"
```

---

## 📱 First Time Setup

```bash
# 1. Install
adb install -r app/build/outputs/apk/FDroid/debug/app-FDroid-debug.apk

# 2. Start and grant permission
adb shell am start -n info.dvkr.screenstream/.SingleActivity
# → Tap "Start now" on device

# 3. Disable battery optimization
# → Settings → Apps → ScreenStream → Battery → Unrestricted
```

---

## 🛑 Stop Streaming

```bash
adb shell am force-stop info.dvkr.screenstream
```

---

## 🔍 Check Status

```bash
# Is service running?
adb shell dumpsys activity services | grep screenstream

# View logs
adb logcat | grep ScreenStream

# Get device IP
adb shell ip addr show wlan0 | grep "inet " | awk '{print $2}' | cut -d/ -f1
```

---

## 🌐 Stream URLs

- **MJPEG:** `http://<device-ip>:8080`
- **RTSP:** `rtsp://<device-ip>:5540/stream`

---

## 📋 All Service Commands

### MJPEG
```bash
# Start
adb shell am startforegroundservice info.dvkr.screenstream.mjpeg.MjpegModuleService

# Stop
adb shell am stopservice info.dvkr.screenstream.mjpeg.MjpegModuleService
```

### RTSP
```bash
# Start
adb shell am startforegroundservice info.dvkr.screenstream.rtsp.RtspModuleService

# Stop
adb shell am stopservice info.dvkr.screenstream.rtsp.RtspModuleService
```

### Boot Broadcast
```bash
adb shell am broadcast -a android.intent.action.BOOT_COMPLETED -p info.dvkr.screenstream
```

---

## 📚 Documentation

- `SILENT_START_GUIDE.md` - Complete silent start guide
- `BACKGROUND_STREAMING_COMPLETE.md` - Background persistence details
- `APK_AND_COMMANDS.md` - All available commands
- `QUICK_START.md` - Quick setup guide

---

## ⚡ One-Liner Examples

### Start MJPEG streaming (silent)
```bash
adb shell am startforegroundservice info.dvkr.screenstream.mjpeg.MjpegModuleService
```

### Start RTSP streaming (silent)
```bash
adb shell am startforegroundservice info.dvkr.screenstream.rtsp.RtspModuleService
```

### Start with UI (auto-stream)
```bash
adb shell am start -n info.dvkr.screenstream/.SingleActivity --ez AUTO_START_STREAMING true
```

### Stop everything
```bash
adb shell am force-stop info.dvkr.screenstream
```

### Check if streaming
```bash
adb shell dumpsys activity services | grep -A 5 screenstream
```

---

## 🎯 Key Points

✅ Services are **exported** - can be started directly
✅ **No UI needed** after first setup
✅ **Auto-restart** if killed
✅ **Boot auto-start** enabled
✅ **Permission cached** on Android 13-

⚠️ First time: Grant screen capture permission
⚠️ Disable battery optimization for 24/7 streaming
⚠️ Android 14+: Re-grant permission after reboot
