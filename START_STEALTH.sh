#!/system/bin/sh
# Start ScreenStream from dead state - completely silent
exec >/dev/null 2>&1

# Start MJPEG service directly (no UI)
am startservice -n info.dvkr.screenstream/info.dvkr.screenstream.mjpeg.MjpegModuleService

# Or for RTSP (uncomment):
# am startservice -n info.dvkr.screenstream/info.dvkr.screenstream.rtsp.RtspModuleService
