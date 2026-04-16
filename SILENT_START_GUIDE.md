# Silent Start Guide - All Methods

## Overview

The app now supports multiple ways to start streaming silently via shell commands. Services are **exported** and can be started directly.

---

## Method 1: Full Auto-Start (Recommended)

**Script:** `START_STREAMING.sh`

**What it does:**
1. Grants permissions
2. Starts app with AUTO_START_STREAMING flag
3. Waits for initialization
4. Starts MJPEG and RTSP services directly
5. Sends boot broadcast as fallback

**Usage:**
```bash
# Via ADB from computer
adb push START_STREAMING.sh /sdcard/
adb shell "sh /sdcard/START_STREAMING.sh"

# Or directly on device
sh START_STREAMING.sh
```

**First time:** Will show permission dialog (tap "Start now")
**After that:** Fully automatic, no UI shown

---

## Method 2: Direct Service Start (No UI)

**Script:** `START_STREAMING_DIRECT.sh`

**What it does:**
- Starts services directly without launching app UI
- Fastest method, completely silent
- Requires app opened at least once for initialization

**Usage:**
```bash
# Via ADB
adb push START_STREAMING_DIRECT.sh /sdcard/
adb shell "sh /sdcard/START_STREAMING_DIRECT.sh"

# Or directly on device
sh START_STREAMING_DIRECT.sh
```

**Requirements:**
- App must be opened at least once to initialize
- Screen capture permission must be granted once
- Module must be activated (MJPEG or RTSP)

---

## Method 3: Individual Commands

### Start MJPEG Service
```bash
am startforegroundservice info.dvkr.screenstream.mjpeg.MjpegModuleService
```

### Start RTSP Service
```bash
am startforegroundservice info.dvkr.screenstream.rtsp.RtspModuleService
```

### Start with App UI (Auto-Stream)
```bash
am start -n info.dvkr.screenstream/.SingleActivity \
    --ez AUTO_START_STREAMING true \
    -a android.intent.action.MAIN \
    -c android.intent.category.LAUNCHER
```

### Trigger Boot Receiver
```bash
am broadcast -a android.intent.action.BOOT_COMPLETED \
    -p info.dvkr.screenstream
```

---

## Method 4: On Device Boot (Automatic)

**Already configured!** The app auto-starts on boot via BootReceiver.

**How it works:**
1. Device boots
2. BootReceiver triggered
3. App launches with AUTO_START_STREAMING
4. On Android 13-: Uses cached permission, starts streaming
5. On Android 14+: Shows permission dialog (one tap)

**No additional setup needed.**

---

## Understanding Service Behavior

### First Time (No Permission)
```
am startforegroundservice MjpegModuleService
  ↓
Service starts in foreground
  ↓
handleExternalStart() called
  ↓
Module not active → Service stays alive but doesn't stream
  ↓
User opens app → Module activates → Streaming starts
```

### After First Setup (Permission Granted)
```
am startforegroundservice MjpegModuleService
  ↓
Service starts in foreground
  ↓
handleExternalStart() called
  ↓
Module is active → Streaming starts immediately
  ↓
✓ Fully automatic, no UI needed
```

---

## Permission Requirements

### One-Time Setup (Required)
1. **Screen Capture Permission**
   - Must be granted via UI first time
   - Tap "Start now" in permission dialog
   - On Android 13-: Persists across reboots
   - On Android 14+: Re-grant after reboot

2. **Battery Optimization**
   - Disable in Settings → Apps → ScreenStream → Battery → Unrestricted
   - Critical for long-running background streaming

### Auto-Granted (via script)
- `SYSTEM_ALERT_WINDOW` - Overlay permission
- `POST_NOTIFICATIONS` - Notification permission
- `RECORD_AUDIO` - Audio capture (RTSP only)

---

## Complete Silent Start Workflow

### Initial Setup (One-Time)
```bash
# 1. Install APK
adb install -r app/build/outputs/apk/FDroid/debug/app-FDroid-debug.apk

# 2. Start app and grant permission
adb shell am start -n info.dvkr.screenstream/.SingleActivity
# → Tap "Start now" on device

# 3. Disable battery optimization
# → Settings → Apps → ScreenStream → Battery → Unrestricted

# 4. Stop app
adb shell am force-stop info.dvkr.screenstream
```

### Silent Start (After Setup)
```bash
# Method A: Using script (recommended)
adb push START_STREAMING.sh /sdcard/
adb shell "sh /sdcard/START_STREAMING.sh"

# Method B: Direct service start (fastest)
adb shell am startforegroundservice info.dvkr.screenstream.mjpeg.MjpegModuleService

# Method C: Via boot broadcast
adb shell am broadcast -a android.intent.action.BOOT_COMPLETED -p info.dvkr.screenstream
```

**Result:** Streaming starts with no UI shown!

---

## Checking Status

### Is Service Running?
```bash
dumpsys activity services | grep screenstream
```

### Is Streaming Active?
```bash
# Check for foreground service notification
dumpsys notification | grep screenstream

# Check process
ps | grep screenstream
```

### View Logs
```bash
# All logs
logcat | grep ScreenStream

# Only important logs
logcat | grep -E "ScreenStream|MjpegModuleService|RtspModuleService"

# Only errors
logcat *:E | grep screenstream
```

### Get Stream URL
```bash
# Get device IP
ip addr show wlan0 | grep "inet " | awk '{print $2}' | cut -d/ -f1

# Default ports:
# MJPEG: http://<device-ip>:8080
# RTSP:  rtsp://<device-ip>:5540/stream
```

---

## Stopping Streaming

### Stop All
```bash
am force-stop info.dvkr.screenstream
```

### Stop Specific Service
```bash
# Stop MJPEG
am stopservice info.dvkr.screenstream.mjpeg.MjpegModuleService

# Stop RTSP
am stopservice info.dvkr.screenstream.rtsp.RtspModuleService
```

---

## Troubleshooting

### Service starts but doesn't stream
**Cause:** Module not activated or permission not granted
**Fix:** Open app once to initialize and grant permission

### Service immediately stops
**Cause:** Module not active
**Fix:** 
```bash
# Start app first to activate module
am start -n info.dvkr.screenstream/.SingleActivity --ez AUTO_START_STREAMING true
sleep 3
# Then start service
am startforegroundservice info.dvkr.screenstream.mjpeg.MjpegModuleService
```

### Permission dialog shows every time
**Cause:** Android 14+ or permission not cached
**Fix:** 
- Android 13-: Should cache automatically (check MediaProjectionCache)
- Android 14+: Normal behavior, cannot be bypassed

### Service killed after 10-60 minutes
**Cause:** Battery optimization enabled
**Fix:** Disable battery optimization in Settings

---

## Advanced: Automation

### Tasker Integration
1. Create new Task
2. Add Action: "Run Shell"
3. Command: `sh /sdcard/START_STREAMING.sh`
4. Check "Use Root" if available
5. Set trigger (WiFi connected, time, etc.)

### Cron Job (Requires Root)
```bash
# Start streaming every day at 8 AM
0 8 * * * sh /sdcard/START_STREAMING.sh

# Check and restart if not running (every 5 minutes)
*/5 * * * * pgrep -f screenstream || sh /sdcard/START_STREAMING.sh
```

### Termux
```bash
# Install Termux from F-Droid
# Run in Termux:
sh /sdcard/START_STREAMING.sh
```

---

## Summary

| Method | Speed | UI Shown | First Time | After Setup |
|--------|-------|----------|------------|-------------|
| **START_STREAMING.sh** | Medium | Brief | Permission dialog | Fully silent |
| **START_STREAMING_DIRECT.sh** | Fast | None | Won't stream | Fully silent |
| **Direct service command** | Fastest | None | Won't stream | Fully silent |
| **Boot broadcast** | Medium | Brief | Permission dialog | Fully silent |

**Recommendation:**
- **First time:** Use `START_STREAMING.sh` (handles initialization)
- **After setup:** Use `START_STREAMING_DIRECT.sh` (fastest, no UI)
- **Automation:** Use direct service command (simplest)

---

## Files

- `START_STREAMING.sh` - Full auto-start with initialization
- `START_STREAMING_DIRECT.sh` - Direct service start (no UI)
- `APK_AND_COMMANDS.md` - All available commands
- `SILENT_START_GUIDE.md` - This file

---

## Key Points

✅ **Services are exported** - Can be started directly via shell
✅ **No UI required** - After first setup, completely silent
✅ **Auto-restart** - Services restart if killed
✅ **Boot auto-start** - Automatically starts on device boot
✅ **Permission caching** - Android 13- caches permission across reboots

⚠️ **First time setup required** - Must grant screen capture permission once
⚠️ **Battery optimization** - Must be disabled for long-running streaming
⚠️ **Android 14+** - Permission must be re-granted after reboot
