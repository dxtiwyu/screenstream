# Quick Start - Background Streaming

## ✅ Build Complete

**APK Location:** `app/build/outputs/apk/FDroid/debug/app-FDroid-debug.apk`

## Install

```bash
adb install -r app/build/outputs/apk/FDroid/debug/app-FDroid-debug.apk
```

## Setup (One-Time)

### 1. Disable Battery Optimization (REQUIRED)
```
Settings → Apps → ScreenStream → Battery → Unrestricted
```
**Without this, Android kills the app after 10-60 minutes.**

### 2. Start Streaming
- Open app
- Tap "Start"
- Grant screen capture permission ("Start now")

## What's Fixed

✅ **Streaming continues when app swiped from recents**
✅ **Streaming continues indefinitely in background**
✅ **Service auto-restarts if killed by system**
✅ **Auto-starts on device boot**
✅ **Screen capture permission persists on Android 13 and below**

## Screen Capture Permission

### Android 13 and Below
✅ **Grant once, works forever** (even after reboot!)

### Android 14+
⚠️ **Grant after each reboot** (Android security - cannot be bypassed)

**Why Device Admin doesn't help:**
- Device Admin can prevent force-stop and uninstall
- Device Admin CANNOT bypass MediaProjection permission
- This is an Android security requirement, not a bug

## Test It

1. Start streaming
2. Swipe app from recents → ✅ Streaming continues
3. Wait 1 hour → ✅ Streaming continues
4. Reboot device → ✅ Auto-starts (Android 13-) or one-tap (Android 14+)

## Troubleshooting

**Streaming stops after 10-60 minutes?**
→ Disable battery optimization (step 1 above)

**Streaming stops when swiped from recents?**
→ Update to this new APK

**Permission needed after reboot?**
→ Normal on Android 14+ (cannot be fixed)
→ Should NOT happen on Android 13 and below

## Documentation

- `BACKGROUND_STREAMING_COMPLETE.md` - Full technical details
- `BACKGROUND_STREAMING_SETUP.md` - User setup guide
- `SCREEN_CAPTURE_PERMISSION_SOLUTION.md` - Permission analysis
- `APK_AND_COMMANDS.md` - Build and shell commands

## Summary

**You now have maximum possible background persistence on stock Android.**

The only manual steps required:
1. One-time: Disable battery optimization
2. Android 14+ only: Grant permission after reboot (one tap)

Everything else is automatic!
