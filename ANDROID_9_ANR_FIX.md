# Android 9 ANR (App Not Responding) Fix

## Problem
The app was showing "App not responding" dialog and then crashing/disappearing on Android 9 after starting the streaming service.

## Root Cause
**ANR (Application Not Responding)** occurs when a foreground service on Android 8.0+ (API 26+) does not call `startForeground()` within 5 seconds of being started. The Android system kills the service to prevent abuse.

### Previous Flow (BROKEN):
1. Service `onStartCommand()` called
2. Process intent and parse event
3. Check if module is active
4. Call module's `onServiceStart()` method
5. Eventually call `startForeground()` (TOO LATE - could take >5 seconds)
6. **System kills service with ANR**

## Solution
Call `startForeground()` **immediately** at the beginning of `onStartCommand()`, before any other processing.

### New Flow (FIXED):
1. Service `onStartCommand()` called
2. **Immediately call `startForeground()` (within milliseconds)**
3. Process intent and parse event
4. Check if module is active
5. Call module's `onServiceStart()` method
6. Service continues running normally

## Files Modified
1. `mjpeg/src/main/java/info/dvkr/screenstream/mjpeg/MjpegModuleService.kt`
2. `rtsp/src/main/java/info/dvkr/screenstream/rtsp/RtspModuleService.kt`
3. `webrtc/src/main/java/info/dvkr/screenstream/webrtc/WebRtcModuleService.kt`

## Changes Made
Added immediate `startForeground()` call at the top of `onStartCommand()` in all three service classes:

```kotlin
override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    // CRITICAL: Call startForeground() immediately to avoid ANR on Android 8.0+ (API 26+)
    // Services must call startForeground() within 5 seconds or system kills them
    try {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val stopIntent = [Event].Intentable.StopStream("[Service]. User action: Notification").toIntent(this)
            val fgsType = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            } else {
                0
            }
            startForeground(stopIntent, fgsType)
            XLog.d(getLog("onStartCommand", "startForeground() called immediately to prevent ANR"))
        }
    } catch (e: Exception) {
        XLog.e(getLog("onStartCommand", "Failed to call startForeground(): ${e.message}"))
    }
    
    // ... rest of the original code
}
```

## Key Points
- **Timing is critical**: The 5-second window starts when `startService()` or `startForegroundService()` is called
- **Android 8.0+ requirement**: This is enforced on API 26 (Android 8.0) and above
- **Foreground service type**: On Android 10+ (API 29+), we specify `FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION`
- **Stealth notification**: The notification is already configured to look like a generic system service
- **Error handling**: Wrapped in try-catch to prevent crashes if notification permission is missing

## Testing
1. Install the new APK on Android 9 device
2. Start streaming (Local/RTSP mode)
3. Minimize the app or press home button
4. Service should continue running without ANR
5. Check notification bar for the stealth "System" notification

## Build Command
```bash
./gradlew :app:assembleFDroidDebug
```

## APK Location
```
C:\Users\FSOS\screenstream\app\build\outputs\apk\FDroid\debug\app-FDroid-debug.apk
```

## Related Issues Fixed
1. ✅ MediaCodec buffer leak (0xffffffe0 error)
2. ✅ WiFi auto-reconnect with infinite retry
3. ✅ Stealth notification (looks like system service)
4. ✅ Android 9 crash on service start (FOREGROUND_SERVICE_DEFERRED flag)
5. ✅ **Android 9 ANR (App Not Responding) - THIS FIX**

## References
- [Android Foreground Services Documentation](https://developer.android.com/develop/background-work/services/foreground-services)
- [Service.startForeground() API](https://developer.android.com/reference/android/app/Service#startForeground(int,%20android.app.Notification))
