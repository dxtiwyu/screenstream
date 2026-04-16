#!/system/bin/sh
# ScreenStream - Silent Start and Auto-Stream Script
# This script starts the ScreenStream app and automatically begins streaming
# Works with the new background persistence implementation

PACKAGE="info.dvkr.screenstream"
ACTIVITY="info.dvkr.screenstream.SingleActivity"
MJPEG_SERVICE="info.dvkr.screenstream.mjpeg.MjpegModuleService"
RTSP_SERVICE="info.dvkr.screenstream.rtsp.RtspModuleService"

echo "=== ScreenStream Silent Auto-Start Script ==="
echo ""

# Check if app is installed
if ! pm list packages | grep -q "$PACKAGE"; then
    echo "ERROR: ScreenStream is not installed!"
    echo "Please install the APK first:"
    echo "  adb install -r app/build/outputs/apk/FDroid/debug/app-FDroid-debug.apk"
    exit 1
fi

echo "[1/5] Granting necessary permissions..."
# Grant required permissions (run as root or via adb)
pm grant $PACKAGE android.permission.SYSTEM_ALERT_WINDOW 2>/dev/null
pm grant $PACKAGE android.permission.POST_NOTIFICATIONS 2>/dev/null
pm grant $PACKAGE android.permission.RECORD_AUDIO 2>/dev/null

echo "[2/5] Starting ScreenStream app (silent)..."
# Start the app with AUTO_START_STREAMING flag
# This initializes the app and activates the streaming module
am start -n $ACTIVITY \
    --ez AUTO_START_STREAMING true \
    -a android.intent.action.MAIN \
    -c android.intent.category.LAUNCHER \
    >/dev/null 2>&1

# Wait for app to initialize and activate module
echo "      Waiting for app initialization..."
sleep 3

echo "[3/5] Starting MJPEG streaming service..."
# Directly start the MJPEG service (exported=true)
# The service will handle external start via handleExternalStart()
am startforegroundservice $MJPEG_SERVICE >/dev/null 2>&1
if [ $? -eq 0 ]; then
    echo "      ✓ MJPEG service started"
else
    echo "      ⚠ MJPEG service start failed (may not be active module)"
fi

sleep 1

echo "[4/5] Starting RTSP streaming service..."
# Directly start the RTSP service (exported=true)
am startforegroundservice $RTSP_SERVICE >/dev/null 2>&1
if [ $? -eq 0 ]; then
    echo "      ✓ RTSP service started"
else
    echo "      ⚠ RTSP service start failed (may not be active module)"
fi

sleep 1

echo "[5/5] Sending boot broadcast (fallback)..."
# Send boot broadcast as fallback to trigger BootReceiver
am broadcast -a android.intent.action.BOOT_COMPLETED -p $PACKAGE >/dev/null 2>&1

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "✓ ScreenStream started!"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "Status:"
echo "  • App is running in background"
echo "  • Streaming services are starting"
echo "  • Foreground notification should appear"
echo ""
echo "⚠ FIRST TIME SETUP:"
echo "  If this is the first time running, you MUST:"
echo "  1. Grant screen capture permission (tap 'Start now')"
echo "  2. This permission persists until device reboot"
echo ""
echo "📱 To grant permission:"
echo "  • The app will show a permission dialog"
echo "  • Tap 'Start now' to allow screen capture"
echo "  • On Android 13-, permission persists across reboots"
echo "  • On Android 14+, re-grant after each reboot"
echo ""
echo "🔍 Check if streaming is active:"
echo "  dumpsys activity services | grep -A 5 screenstream"
echo ""
echo "🛑 To stop streaming:"
echo "  am force-stop $PACKAGE"
echo ""
echo "📊 View logs:"
echo "  logcat | grep ScreenStream"
echo ""
echo "🌐 Default stream URLs (check your device IP):"
echo "  MJPEG: http://<device-ip>:8080"
echo "  RTSP:  rtsp://<device-ip>:5540/stream"
echo ""
