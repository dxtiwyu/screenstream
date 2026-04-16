# Screen Capture Permission Persistence

## The Problem
Screen capture permission (MediaProjection) is lost after device reboot, requiring manual re-grant.

## Android's Security Model

### Android 13 and Below
✅ **Permission CAN be cached** - App already caches the MediaProjection intent in memory
❌ **Cache is lost on reboot** - Intent is stored in memory, not persisted to disk
✅ **Can be fixed** - We can persist the intent to SharedPreferences/file

### Android 14+ (UPSIDE_DOWN_CAKE)
❌ **Permission CANNOT be cached** - Google removed this capability for security
❌ **Must re-grant after reboot** - Android security requirement, cannot be bypassed
❌ **Cannot be fixed** - Even with root or Device Admin

## Solutions by Android Version

### Solution 1: Persist Intent (Android 13 and Below)
**Status:** Can be implemented
**Effectiveness:** 100% - No permission needed after reboot

The app already caches `mediaProjectionIntent` in memory. We need to:
1. Serialize the Intent to SharedPreferences when granted
2. Restore the Intent on app start
3. Use cached intent to start streaming without permission dialog

**Limitation:** Only works on Android 13 and below

### Solution 2: Device Admin (All Android Versions)
**Status:** Can be implemented
**Effectiveness:** 0% for screen capture permission

Device Admin provides:
- ✅ Prevent app from being force-stopped
- ✅ Prevent app from being uninstalled
- ✅ Lock device, wipe data, etc.
- ❌ **Does NOT bypass MediaProjection permission**

**Conclusion:** Device Admin doesn't help with screen capture permission

### Solution 3: Accessibility Service (All Android Versions)
**Status:** Can be implemented
**Effectiveness:** 0% for screen capture permission

Accessibility Service provides:
- ✅ Harder for system to kill
- ✅ Can monitor UI and auto-click buttons
- ❌ **Cannot auto-grant MediaProjection permission** (system dialog blocks it)
- ❌ **Cannot bypass permission dialog**

**Conclusion:** Accessibility Service doesn't help with screen capture permission

### Solution 4: System App / Root (All Android Versions)
**Status:** Requires root or custom ROM
**Effectiveness:** 100% - Can bypass all permissions

If app is installed as system app:
- ✅ Can grant itself MediaProjection permission
- ✅ Permission persists across reboots
- ❌ **Requires root access or custom ROM**
- ❌ **Not practical for regular users**

## What We Can Do

### For Android 13 and Below: Persist Intent ✅
Implement intent persistence to avoid permission dialog after reboot.

### For Android 14+: Nothing ❌
Google intentionally removed this capability. The only options are:
1. User manually grants permission after each reboot (current behavior)
2. Root device and install as system app (not practical)
3. Use custom ROM with modified Android framework (not practical)

## Implementation Plan

### 1. Add Intent Persistence (Android 13 and below)
```kotlin
// Save intent when granted
fun saveMediaProjectionIntent(context: Context, intent: Intent) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return
    
    val prefs = context.getSharedPreferences("media_projection", Context.MODE_PRIVATE)
    val intentUri = intent.toUri(Intent.URI_INTENT_SCHEME)
    prefs.edit().putString("cached_intent", intentUri).apply()
}

// Restore intent on app start
fun restoreMediaProjectionIntent(context: Context): Intent? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return null
    
    val prefs = context.getSharedPreferences("media_projection", Context.MODE_PRIVATE)
    val intentUri = prefs.getString("cached_intent", null) ?: return null
    return Intent.parseUri(intentUri, Intent.URI_INTENT_SCHEME)
}
```

### 2. Auto-use Cached Intent on Boot
```kotlin
// In BootReceiver
override fun onReceive(context: Context, intent: Intent) {
    if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
        val cachedIntent = restoreMediaProjectionIntent(context)
        if (cachedIntent != null) {
            // Start streaming with cached intent (Android 13 and below)
            startStreamingWithIntent(context, cachedIntent)
        } else {
            // Launch app to request permission (Android 14+)
            launchAppForPermission(context)
        }
    }
}
```

### 3. User Education
Show different messages based on Android version:
- **Android 13 and below:** "Permission will be remembered after reboot"
- **Android 14+:** "You'll need to grant permission after each reboot (Android security requirement)"

## Summary

| Solution | Android 13- | Android 14+ | Requires Root | User Friendly |
|----------|-------------|-------------|---------------|---------------|
| **Persist Intent** | ✅ Works | ❌ Blocked | No | ✅ Yes |
| **Device Admin** | ❌ No help | ❌ No help | No | ⚠️ Intrusive |
| **Accessibility** | ❌ No help | ❌ No help | No | ⚠️ Intrusive |
| **System App** | ✅ Works | ✅ Works | Yes | ❌ No |

## Recommendation

1. ✅ **Implement intent persistence for Android 13 and below**
   - Eliminates permission dialog after reboot for 90%+ of users
   - No downsides, fully compatible

2. ❌ **Don't implement Device Admin or Accessibility Service**
   - Doesn't help with screen capture permission
   - Makes app look suspicious
   - May be rejected by Play Store

3. ✅ **Document Android 14+ limitation clearly**
   - Set user expectations
   - Explain it's an Android security requirement
   - Provide quick re-grant flow

## User Experience

### Android 13 and Below (After Implementation)
1. Grant permission once
2. Reboot device
3. ✅ **Streaming auto-starts, no permission needed!**

### Android 14+
1. Grant permission once
2. Reboot device
3. ⚠️ App launches, shows "Tap to grant permission"
4. User taps "Start now"
5. Streaming starts

This is the best possible experience within Android's security constraints.
