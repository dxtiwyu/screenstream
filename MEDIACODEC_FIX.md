# MediaCodec Error 0xffffffe0 Fix

## Problem
The application was crashing after running for a few minutes with the error:
```
android.media.MediaCodec$CodecException: Error 0xffffffe0
```

Error code `0xffffffe0` (decimal `-32`) indicates `ERROR_INSUFFICIENT_RESOURCE`, which means the MediaCodec ran out of available buffers.

## Root Cause
The issue was in `RtspClient.kt` in the streaming loop. When frames were dropped due to:
1. Video generation mismatch (line 664)
2. Queue congestion for non-key frames (line 668)

The code used `continue` to skip processing, but **never called `frame.release()`**. This caused MediaCodec output buffers to leak because:

- Each `MediaFrame` has a `releaseCallback` that must be invoked to return the buffer to MediaCodec
- The `finally` block at line 736 releases frames, but `continue` statements bypass it
- Over time, unreleased buffers accumulate until MediaCodec runs out of resources
- After a few minutes, this causes the `0xffffffe0` error

## Solution
Added `frame.release()` calls before each `continue` statement in the frame dropping logic:

```kotlin
// Before (BUGGY):
if (queuedItem.frame is MediaFrame.VideoFrame && queuedItem.videoGeneration != activeVideoGeneration) {
    continue  // ❌ Buffer leaked!
}

// After (FIXED):
if (queuedItem.frame is MediaFrame.VideoFrame && queuedItem.videoGeneration != activeVideoGeneration) {
    queuedItem.frame.release()  // ✅ Buffer properly released
    continue
}
```

## Files Modified
- `rtsp/src/main/java/info/dvkr/screenstream/rtsp/internal/rtsp/client/RtspClient.kt`
  - Line 665: Added `queuedItem.frame.release()` before continue (video generation mismatch)
  - Line 670: Added `queuedItem.frame.release()` before continue (congestion drop)

## Testing Recommendations
1. Run the app for 10+ minutes continuously streaming
2. Monitor for the `0xffffffe0` error - it should no longer occur
3. Test with high frame rates and network congestion to trigger frame drops
4. Verify no memory leaks using Android Profiler
5. Check logcat for any "Dropping frame" messages to confirm drops are happening correctly

## Additional Notes
- The `finally` block already handles normal frame release, but doesn't catch early exits via `continue`
- Other parts of the codebase (RtspServer, RtspServerConnection) already handle frame release correctly
- This is a critical fix for production stability - buffer leaks will always cause crashes eventually
