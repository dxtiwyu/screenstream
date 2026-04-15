# WiFi Auto-Reconnect Fix

## Problem
When the app is not connected to WiFi or cannot find an IP address, it would:
1. Try 3-4 times to discover network interfaces
2. Give up and show "IP address not found" error
3. Crash or require manual user intervention to restart
4. Not automatically start when WiFi becomes available

## Root Cause
Both MJPEG and RTSP streaming services had a hard limit on retry attempts:
- **MJPEG**: Stopped after 3 attempts (3 seconds total)
- **RTSP**: Stopped after 4 attempts (4 seconds total)

After reaching the limit, the services would:
- Set an error state (`AddressNotFoundException`)
- Stop trying to discover network interfaces
- Wait for manual user action to restart

This was problematic because:
- WiFi connection can take longer than 3-4 seconds
- Network interfaces may not be immediately available after WiFi connects
- Users had to manually restart the stream every time WiFi reconnected

## Solution
Implemented **infinite retry with exponential backoff** for both modules:

### Retry Strategy
1. **First attempts (0-3/4)**: Retry every 1 second (fast initial discovery)
2. **Medium attempts (4-6/8)**: Retry every 3 seconds (moderate backoff)
3. **Later attempts (7+/9+)**: Retry every 5 seconds (steady state polling)

### User Experience
- Error message is shown after initial failures (3-4 attempts) to inform the user
- Service continues retrying in the background indefinitely
- When WiFi/IP becomes available, streaming automatically starts
- No manual intervention required

### Technical Details

**MJPEG Module** (`MjpegStreamingService.kt`):
```kotlin
// Before: Gave up after 3 attempts
if (event.attempt < 3) {
    sendEvent(InternalEvent.DiscoverAddress(event.reason, event.attempt + 1), 1000)
} else {
    currentError = MjpegError.AddressNotFoundException()
}

// After: Infinite retry with exponential backoff
val delay = when {
    event.attempt < 3 -> 1000L
    event.attempt < 6 -> 3000L
    else -> 5000L
}
sendEvent(InternalEvent.DiscoverAddress(event.reason, event.attempt + 1), delay)

// Show error only once, but keep retrying
if (event.attempt == 3 && currentError == null) {
    currentError = MjpegError.AddressNotFoundException()
}
```

**RTSP Module** (`RtspStreamingService.kt`):
```kotlin
// Before: Gave up after 4 attempts
if (event.attempt < 4) {
    sendEvent(InternalEvent.RtspServer.DiscoverAddress(...), 1000)
} else {
    stopStream(true)
    currentError = RtspError.ServerError.AddressNotFoundException()
}

// After: Infinite retry with exponential backoff
val delay = when {
    event.attempt < 4 -> 1000L
    event.attempt < 8 -> 3000L
    else -> 5000L
}
sendEvent(InternalEvent.RtspServer.DiscoverAddress(...), delay)

// Show error only once, but keep retrying
if (event.attempt == 4 && currentError == null) {
    stopStream(true)
    currentError = RtspError.ServerError.AddressNotFoundException()
}
```

## Benefits
1. **No crashes**: Service gracefully handles missing network connectivity
2. **Auto-recovery**: Automatically starts streaming when WiFi becomes available
3. **No manual intervention**: Users don't need to click anything
4. **Battery efficient**: Exponential backoff reduces unnecessary polling
5. **User-friendly**: Error message informs user, but service keeps trying

## Testing Recommendations
1. Start the app without WiFi connection
2. Verify error message appears after 3-4 seconds
3. Turn on WiFi
4. Verify streaming starts automatically within 5 seconds
5. Toggle WiFi on/off multiple times
6. Verify app never crashes and always auto-recovers
7. Check battery usage to ensure backoff is working correctly

## Files Modified
- `mjpeg/src/main/java/info/dvkr/screenstream/mjpeg/internal/MjpegStreamingService.kt`
  - Modified `DiscoverAddress` event handler (lines 353-377)
  
- `rtsp/src/main/java/info/dvkr/screenstream/rtsp/internal/RtspStreamingService.kt`
  - Modified `DiscoverAddress` event handler (lines 310-340)

## Backward Compatibility
- Fully backward compatible
- No API changes
- No settings changes required
- Existing behavior improved without breaking changes
