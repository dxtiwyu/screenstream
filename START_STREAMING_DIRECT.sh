#!/system/bin/sh
# ScreenStream - Direct Service Start (No UI)
# Starts streaming service directly without launching the app UI
# Requires: App opened at least once to initialize and grant permissions

PACKAGE="info.dvkr.screenstream"
MJPEG_SERVICE="info.dvkr.screenstream.mjpeg.MjpegModuleService"
RTSP_SERVICE="info.dvkr.screenstream.rtsp.RtspModuleService"

echo "=== ScreenStream Direct Service Start ==="
echo ""

# Check if app is installed
if ! pm list packages | grep -q "$PACKAGE"; then
    echo "❌ ERROR: ScreenStream is not installed!"
    exit 1
fi

echo "Starting streaming services directly (no UI)..."
echo ""

# Start MJPEG service
echo "[1/2] Starting MJPEG service..."
am startforegroundservice $MJPEG_SERVICE
if [ $? -eq 0 ]; then
    echo "      ✓ MJPEG service started"
else
    echo "      ✗ Failed (module may not be active)"
fi

sleep 1

# Start RTSP service
echo "[2/2] Starting RTSP service..."
am startforegroundservice $RTSP_SERVICE
if [ $? -eq 0 ]; then
    echo "      ✓ RTSP service started"
else
    echo "      ✗ Failed (module may not be active)"
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "✓ Services started!"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "⚠ IMPORTANT:"
echo "  • If this is the first run, services will stay in foreground"
echo "    but won't stream until you open the app and grant permission"
echo "  • After first setup, services will auto-stream on start"
echo ""
echo "🔍 Check service status:"
echo "  dumpsys activity services | grep screenstream"
echo ""
echo "🛑 Stop services:"
echo "  am force-stop $PACKAGE"
echo ""
