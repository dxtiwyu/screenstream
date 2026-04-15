#!/system/bin/sh
# ScreenStream - Silent Start and Auto-Stream Script
# This script starts the ScreenStream app and automatically begins streaming

PACKAGE="info.dvkr.screenstream"
ACTIVITY="info.dvkr.screenstream.SingleActivity"

echo "=== ScreenStream Auto-Start Script ==="
echo ""

# Check if app is installed
if ! pm list packages | grep -q "$PACKAGE"; then
    echo "ERROR: ScreenStream is not installed!"
    echo "Please install the APK first:"
    echo "  adb install app-FDroid-debug.apk"
    exit 1
fi

echo "[1/4] Granting necessary permissions..."
# Grant required permissions (run as root or via adb)
pm grant $PACKAGE android.permission.SYSTEM_ALERT_WINDOW 2>/dev/null
pm grant $PACKAGE android.permission.POST_NOTIFICATIONS 2>/dev/null

echo "[2/4] Starting ScreenStream app..."
# Start the app with AUTO_START_STREAMING flag
am start -n $ACTIVITY \
    --ez AUTO_START_STREAMING true \
    -a android.intent.action.MAIN \
    -c android.intent.category.LAUNCHER \
    >/dev/null 2>&1

# Wait for app to initialize
sleep 2

echo "[3/4] Triggering Quick Settings Tile (if available)..."
# Try to trigger the tile service to start streaming
# This simulates clicking the Quick Settings tile
am startservice -n $PACKAGE/info.dvkr.screenstream.tile.TileActionService >/dev/null 2>&1

sleep 1

echo "[4/4] Sending broadcast to start streaming..."
# Send a broadcast that might trigger streaming (for RTSP/MJPEG auto-start)
am broadcast -a android.intent.action.BOOT_COMPLETED -p $PACKAGE >/dev/null 2>&1

echo ""
echo "✓ ScreenStream started!"
echo ""
echo "The app should now be running and attempting to start streaming."
echo "If streaming doesn't start automatically, you may need to:"
echo "  1. Grant screen capture permission manually (first time only)"
echo "  2. Ensure WiFi is connected"
echo "  3. Check app settings for auto-start configuration"
echo ""
echo "To stop streaming:"
echo "  am force-stop $PACKAGE"
