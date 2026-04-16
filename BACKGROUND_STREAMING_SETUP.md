# Background Streaming - Quick Setup Guide

## TL;DR - What You Need to Do

### 1. Disable Battery Optimization (ONE TIME - REQUIRED)
**Without this, Android WILL kill the app after 10-60 minutes in background.**

1. Open ScreenStream app
2. When prompted, tap "Allow" for battery optimization exemption
3. Or manually: Settings → Apps → ScreenStream → Battery → Unrestricted

### 2. Grant Screen Capture Permission (AFTER EACH REBOOT)
**Android security requirement - cannot be bypassed.**

1. Start streaming
2. Tap "Start now" in the permission dialog
3. Done! Permission lasts until next reboot

### 3. Manufacturer Settings (ONE TIME - IF NEEDED)
**Only if streaming still stops after 10+ minutes.**

See manufacturer-specific instructions below.

---

## How to Test It's Working

1. ✅ Start streaming
2. ✅ Press Home button → Check stream still works
3. ✅ Open other apps → Check stream still works
4. ✅ Wait 10 minutes → Check stream still works
5. ✅ Swipe ScreenStream from recents → Stream continues!
6. ✅ Wait 1 hour → Stream still works (if battery optimization disabled)

---

## Manufacturer-Specific Settings

### Samsung
```
Settings → Apps → ScreenStream → Battery
  → Allow background activity: ON

Settings → Device care → Battery → Background usage limits
  → Never sleeping apps → Add ScreenStream
```

### Xiaomi / MIUI / Redmi / Poco
```
Settings → Apps → Manage apps → ScreenStream
  → Battery saver: No restrictions
  → Autostart: ON

Settings → Battery & performance → App battery saver
  → ScreenStream: No restrictions
```

### Huawei / Honor / EMUI
```
Settings → Apps → ScreenStream → Battery
  → App launch: Manage manually
  → Auto-launch: ON
  → Secondary launch: ON  
  → Run in background: ON
```

### OnePlus / Oppo / Realme
```
Settings → Apps → ScreenStream → Battery
  → Battery optimization: Don't optimize

Settings → Apps → ScreenStream → Advanced
  → Background activity: Allow
```

### Vivo
```
Settings → Battery → Background power consumption management
  → Add ScreenStream to whitelist

Settings → More settings → Applications → Autostart
  → ScreenStream: ON
```

### Google Pixel / Stock Android
```
Settings → Apps → ScreenStream → Battery
  → Battery optimization: Not optimized

(Usually works without additional settings)
```

---

## Troubleshooting

### Stream stops after 10-60 minutes
**Cause:** Battery optimization is still enabled
**Fix:** Follow step 1 above + manufacturer settings

### Stream stops when app is swiped from recents
**Cause:** Old version or manufacturer restriction
**Fix:** Update app + check manufacturer settings

### Stream stops after device reboot
**Cause:** Screen capture permission expired
**Fix:** Start streaming and grant permission again (normal behavior)

### Stream never starts
**Cause:** Screen capture permission not granted
**Fix:** Tap "Start now" when permission dialog appears

---

## What Changed in This Update

### Before (Old Behavior)
- ❌ Stream stopped when app swiped from recents
- ❌ Stream stopped after 10-60 minutes in background
- ❌ Low priority notification → Android killed service easily
- ❌ No automatic restart mechanisms

### After (New Behavior)
- ✅ Stream continues when app swiped from recents
- ✅ Stream continues indefinitely in background (with battery optimization disabled)
- ✅ High priority notification → Android keeps service alive
- ✅ Multiple automatic restart mechanisms
- ✅ Wake lock prevents CPU sleep
- ✅ Auto-start on device boot

---

## Technical Details (For Advanced Users)

### What Keeps the Stream Running
1. **Foreground Service** - Highest priority service type
2. **PARTIAL_WAKE_LOCK** - Prevents CPU sleep
3. **High Priority Notification** - Makes Android less likely to kill service
4. **START_STICKY** - Android recreates service if killed
5. **onTaskRemoved() restart** - Service restarts when app swiped from recents
6. **onDestroy() restart** - Service restarts when killed by system

### Permissions Used
- `FOREGROUND_SERVICE` - Run as foreground service
- `FOREGROUND_SERVICE_MEDIA_PROJECTION` - Screen capture
- `WAKE_LOCK` - Keep CPU running
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` - Exempt from battery optimization
- `RECEIVE_BOOT_COMPLETED` - Auto-start on boot

### Limitations (Android Security)
- Screen capture permission must be granted after each reboot
- Notification must be visible (required for foreground services)
- Battery optimization must be disabled by user
- Some manufacturers require additional settings

---

## Still Having Issues?

1. **Check battery optimization is disabled** (most common issue)
2. **Check manufacturer-specific settings** (second most common)
3. **Reboot device** after changing settings
4. **Update to latest version** of ScreenStream
5. **Check logs** in app settings for error messages

---

## Summary

**Required (one-time):**
- Disable battery optimization

**Required (after each reboot):**
- Grant screen capture permission

**Optional (if still having issues):**
- Configure manufacturer-specific settings

**Result:**
- Stream runs forever in background
- No manual intervention needed
- Survives app being swiped from recents
- Auto-restarts if killed by system
