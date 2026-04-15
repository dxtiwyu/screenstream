# ScreenStream APK Location and Shell Commands

## Built APK Location

**File:** `app/build/outputs/apk/FDroid/debug/app-FDroid-debug.apk`
- **Size:** 19.45 MB
- **Build Type:** FDroid Debug (Ad-free, no WebRTC)
- **Package Name:** `info.dvkr.screenstream`
- **Version:** 4.3.7 (Build 43007)

### Alternative Build Commands

```bash
# Build F-Droid Debug (recommended - ad-free)
./gradlew :app:assembleFDroidDebug

# Build F-Droid Release (signed, optimized)
./gradlew :app:assembleFDroidRelease

# Build Play Store Debug (includes ads + WebRTC)
./gradlew :app:assemblePlayStoreDebug

# Build Play Store Release
./gradlew :app:assemblePlayStoreRelease
```

## Installation Commands

### Via ADB (from computer)
```bash
# Install APK
adb install app/build/outputs/apk/FDroid/debug/app-FDroid-debug.apk

# Install and replace existing
adb install -r app/build/outputs/apk/FDroid/debug/app-FDroid-debug.apk

# Install to specific device (if multiple connected)
adb -s <device_id> install app-FDroid-debug.apk
```

### Direct on Android Device
```bash
# Copy APK to device
adb push app/build/outputs/apk/FDroid/debug/app-FDroid-debug.apk /sdcard/

# Install from device shell
pm install /sdcard/app-FDroid-debug.apk
```

## Shell Commands for Silent Start & Auto-Stream

### Method 1: Using the Auto-Start Script (Recommended)

```bash
# Make script executable
chmod +x START_STREAMING.sh

# Run the script
sh START_STREAMING.sh

# Or via ADB from computer
adb push START_STREAMING.sh /sdcard/
adb shell "sh /sdcard/START_STREAMING.sh"
```

### Method 2: Manual Commands

#### Basic Start (Opens app)
```bash
# Start the main activity
am start -n info.dvkr.screenstream/.SingleActivity

# Start with flags for background launch
am start -n info.dvkr.screenstream/.SingleActivity \
    --activity-clear-top \
    --activity-single-top
```

#### Start with Auto-Stream Intent
```bash
# Start with AUTO_START_STREAMING flag (used by boot receiver)
am start -n info.dvkr.screenstream/.SingleActivity \
    --ez AUTO_START_STREAMING true \
    -a android.intent.action.MAIN \
    -c android.intent.category.LAUNCHER
```

#### Trigger Quick Settings Tile
```bash
# Simulate tile click (requires app to be running)
am startservice -n info.dvkr.screenstream/info.dvkr.screenstream.tile.TileActionService
```

#### Send Boot Completed Broadcast
```bash
# Trigger boot receiver (simulates device boot)
am broadcast -a android.intent.action.BOOT_COMPLETED \
    -p info.dvkr.screenstream
```

### Method 3: Complete Silent Start Sequence

```bash
#!/system/bin/sh
# Complete sequence for silent start with streaming

PACKAGE="info.dvkr.screenstream"

# 1. Grant permissions (requires root or adb)
pm grant $PACKAGE android.permission.SYSTEM_ALERT_WINDOW
pm grant $PACKAGE android.permission.POST_NOTIFICATIONS

# 2. Start app in background
am start -n $PACKAGE/.SingleActivity \
    --ez AUTO_START_STREAMING true \
    --activity-no-animation \
    --activity-clear-top

# 3. Wait for initialization
sleep 2

# 4. Trigger streaming via tile service
am startservice -n $PACKAGE/info.dvkr.screenstream.tile.TileActionService

# 5. Send boot broadcast as fallback
am broadcast -a android.intent.action.BOOT_COMPLETED -p $PACKAGE
```

## Stop Streaming Commands

```bash
# Force stop the app (stops all streaming)
am force-stop info.dvkr.screenstream

# Kill the app process
am kill info.dvkr.screenstream

# Stop specific service (if you know the service name)
am stopservice info.dvkr.screenstream/.mjpeg.MjpegModuleService
am stopservice info.dvkr.screenstream/.rtsp.RtspModuleService
```

## Check App Status

```bash
# Check if app is running
ps | grep screenstream

# Check app info
dumpsys package info.dvkr.screenstream | grep -A 5 "versionName"

# Check granted permissions
dumpsys package info.dvkr.screenstream | grep -A 20 "granted=true"

# Check if streaming is active (look for foreground service)
dumpsys activity services | grep -A 10 screenstream
```

## Useful ADB Commands

```bash
# View app logs in real-time
adb logcat | grep ScreenStream

# View only errors
adb logcat *:E | grep screenstream

# Clear app data (reset to defaults)
adb shell pm clear info.dvkr.screenstream

# Uninstall app
adb uninstall info.dvkr.screenstream

# Get device IP address
adb shell ip addr show wlan0 | grep inet
```

## Automation Examples

### Auto-start on Device Boot

The app already has boot receivers configured. To enable:

1. Install the app
2. Grant necessary permissions
3. Start the app at least once manually
4. Enable "Start on Boot" in app settings (if available)
5. Reboot device - app should auto-start

### Cron Job (requires root)

```bash
# Add to crontab (requires root and cron)
# Start streaming every day at 8 AM
0 8 * * * sh /sdcard/START_STREAMING.sh

# Check and restart if not running (every 5 minutes)
*/5 * * * * pgrep -f screenstream || sh /sdcard/START_STREAMING.sh
```

### Tasker Integration

1. Create new Task in Tasker
2. Add Action: "Run Shell"
3. Command: `sh /sdcard/START_STREAMING.sh`
4. Check "Use Root" if available
5. Set trigger (e.g., WiFi Connected, Time, etc.)

## Troubleshooting

### App doesn't start streaming automatically

**Possible causes:**
1. Screen capture permission not granted (first-time setup required)
2. No WiFi connection / No IP address found
3. Battery optimization blocking background start

**Solutions:**
```bash
# Disable battery optimization
adb shell dumpsys deviceidle whitelist +info.dvkr.screenstream

# Check if permission is granted
adb shell dumpsys package info.dvkr.screenstream | grep SYSTEM_ALERT_WINDOW

# Grant permission manually
adb shell appops set info.dvkr.screenstream SYSTEM_ALERT_WINDOW allow
```

### "d: inaccessible or not found" error

This error occurs when trying to run a command that doesn't exist. Make sure:

1. You're in the correct directory
2. The script has execute permissions: `chmod +x START_STREAMING.sh`
3. Use `sh` or `bash` to run the script: `sh START_STREAMING.sh`
4. On Android, use: `adb shell "sh /sdcard/START_STREAMING.sh"`

### App crashes or stops unexpectedly

```bash
# View crash logs
adb logcat -b crash

# View app-specific logs
adb logcat | grep -E "ScreenStream|AndroidRuntime"

# Check for memory issues
adb shell dumpsys meminfo info.dvkr.screenstream
```

## Network Information

Once streaming starts, you can find the stream URL:

```bash
# Get device IP
adb shell ip addr show wlan0 | grep "inet " | awk '{print $2}' | cut -d/ -f1

# Default ports:
# MJPEG: http://<device-ip>:8080
# RTSP: rtsp://<device-ip>:5540/stream
```

## Notes

- **First Run:** Screen capture permission must be granted manually the first time
- **WiFi Required:** App needs network connection to start streaming
- **Foreground Service:** Streaming runs as a foreground service (shows notification)
- **Auto-Reconnect:** With the fixes applied, the app will automatically reconnect when WiFi becomes available
- **Battery:** Streaming consumes battery; consider keeping device plugged in for long sessions
