# Stealth Start - Reality Check

## The Problem
You **CANNOT** start the streaming services directly from shell/adb because:

1. **Services are NOT exported** (`android:exported="false"`)
   - `MjpegModuleService` and `RtspModuleService` cannot be called externally
   - `am startservice` will fail with "Permission Denial"

2. **Services require specific intent data**
   - Need `MjpegEvent.Intentable` objects serialized in intent extras
   - Need `INTENT_ID` UUID for deduplication
   - Empty intents are rejected and service stops itself

3. **Services require app initialization**
   - Koin dependency injection must be initialized
   - `streamingModuleManager` must mark module as "active"
   - Without this, service immediately calls `stopSelf()`

4. **Screen capture permission**
   - MediaProjection requires user to click "Start now" dialog
   - Cannot be bypassed even with root (Android security model)
   - Permission must be granted through the UI

## Current "Stealth" Options

### Option 1: Start Activity (NOT TRULY STEALTH)
```bash
am start -n info.dvkr.screenstream/.SingleActivity \
    --ez AUTO_START_STREAMING true \
    -a android.intent.action.MAIN \
    -c android.intent.category.LAUNCHER
```
**Issues:**
- Shows the app UI on screen
- User sees the activity launch
- Requires manual screen capture permission grant (first time)

### Option 2: Trigger Boot Receiver (NOT TRULY STEALTH)
```bash
am broadcast -a android.intent.action.BOOT_COMPLETED \
    -p info.dvkr.screenstream
```
**Issues:**
- Also launches the activity (see BootReceiver.kt)
- Same as Option 1

### Option 3: Quick Settings Tile (SEMI-STEALTH)
```bash
# User must manually add tile to Quick Settings first
# Then can be triggered, but still shows permission dialog
```
**Issues:**
- Requires manual setup
- Still shows screen capture permission dialog

## What Would Make It Truly Stealth?

### Code Changes Needed:

1. **Export the service** (security risk!)
```xml
<service
    android:name=".MjpegModuleService"
    android:exported="true"  <!-- DANGEROUS -->
```

2. **Handle empty/external intents**
```kotlin
override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    // Accept external calls without MjpegEvent
    if (intent == null || MjpegEvent.Intentable.fromIntent(intent) == null) {
        // Create default start event
        val defaultEvent = MjpegEvent.Intentable.StartService(token = "external")
        // Process it...
    }
}
```

3. **Auto-activate module**
```kotlin
// In service onCreate() or onStartCommand()
if (!streamingModuleManager.isActive(MjpegStreamingModule.Id)) {
    streamingModuleManager.activateModule(MjpegStreamingModule.Id)
}
```

4. **Bypass screen capture permission** (IMPOSSIBLE without system modifications)
   - Requires custom ROM or Xposed framework
   - Not possible on stock Android

## Current Best "Stealth" Approach

The notification is already stealth (looks like "System"), but starting requires:

```bash
# 1. Start app (shows UI briefly)
am start -n info.dvkr.screenstream/.SingleActivity \
    --ez AUTO_START_STREAMING true >/dev/null 2>&1

# 2. Grant screen capture permission (MANUAL - user must click "Start now")

# 3. Minimize app
input keyevent KEYCODE_HOME

# 4. App continues streaming in background with stealth notification
```

## Conclusion

**I was NOT 100% sure, and I was WRONG.** The simple `am startservice` command will **NOT work** because:
- Services are not exported
- Services need proper initialization
- Screen capture permission requires user interaction

The app can run stealthily AFTER it's started, but the initial start cannot be completely invisible on stock Android.
