package info.dvkr.screenstream.rtsp

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import com.elvishew.xlog.XLog
import info.dvkr.screenstream.common.getLog
import info.dvkr.screenstream.common.module.StreamingModuleService
import info.dvkr.screenstream.rtsp.internal.RtspEvent
import info.dvkr.screenstream.rtsp.ui.RtspError
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.inject

public class RtspModuleService : StreamingModuleService() {

    internal companion object {
        internal fun getIntent(context: Context): Intent = Intent(context, RtspModuleService::class.java).addIntentId()

        internal fun startService(context: Context, intent: Intent) {
            XLog.d(getLog("RtspModuleService.startService", "Run intent: ${intent.extras}"))
            val importance = ActivityManager.RunningAppProcessInfo().also { ActivityManager.getMyMemoryState(it) }.importance
            XLog.i(getLog("RtspModuleService.startService", "RunningAppProcessInfo.importance: $importance"))
            context.startService(intent)
        }

        internal fun startProjection(context: Context, permissionIntent: Intent, source: String = "ui_permission") {
            val intent = RtspEvent.Intentable.StartProjection(permissionIntent).toIntent(context)
            XLog.d(getLog("RtspModuleService.startProjection", "Run intent: ${intent.extras}"))
            val importance = ActivityManager.RunningAppProcessInfo().also { ActivityManager.getMyMemoryState(it) }.importance
            XLog.i(getLog("RtspModuleService.startProjection", "RunningAppProcessInfo.importance: $importance"))
            XLog.i(getLog("RtspModuleService.startProjection", "SP_TRACE route=preflight_v1 stage=service_command source=$source importance=$importance"))
            context.startService(intent)
        }
    }

    override val notificationIdForeground: Int = 300
    override val notificationIdError: Int = 310

    private val rtspStreamingModule: RtspStreamingModule by inject(RtspKoinQualifier, LazyThreadSafetyMode.NONE)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // CRITICAL: Call startForeground() immediately to avoid ANR on Android 8.0+ (API 26+)
        // Services must call startForeground() within 5 seconds or system kills them
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val stopIntent = RtspEvent.Intentable.StopStream("RtspModuleService. User action: Notification").toIntent(this)
                val fgsType = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                } else {
                    0
                }
                startForeground(stopIntent, fgsType)
                XLog.d(getLog("onStartCommand", "startForeground() called immediately to prevent ANR"))
            }
        } catch (e: Exception) {
            XLog.e(getLog("onStartCommand", "Failed to call startForeground(): ${e.message}"))
        }

        // Handle external/shell calls with no intent or invalid intent
        if (intent == null) {
            XLog.w(getLog("onStartCommand", "Intent is null - treating as external start request"))
            // Create a synthetic StartService event for external calls
            handleExternalStart(startId)
            return START_STICKY
        }

        XLog.d(getLog("onStartCommand", "RtspModuleService.INTENT_ID: ${intent.getStringExtra(INTENT_ID)}"))

        val rtspEvent = RtspEvent.Intentable.fromIntent(intent)
        if (rtspEvent == null) {
            XLog.w(getLog("onStartCommand", "RtspEvent is null - treating as external start request"))
            // Handle external shell command (am startservice)
            handleExternalStart(startId)
            return START_STICKY
        }

        XLog.d(getLog("onStartCommand", "RtspEvent: $rtspEvent, startId: $startId"))

        val shouldDedupe = rtspEvent is RtspEvent.Intentable.StartService
        if (shouldDedupe && isDuplicateIntent(intent)) {
            XLog.i(getLog("onStartCommand", "Duplicate intent for $rtspEvent. Ignoring. startId: $startId"))
            return START_STICKY
        }

        if ((flags and START_FLAG_REDELIVERY) != 0) {
            XLog.e(
                getLog("onStartCommand"),
                IllegalArgumentException("RtspModuleService.onStartCommand: redelivered intent, RtspEvent: $rtspEvent, startId: $startId, $intent")
            )
            return START_STICKY
        }

        if (streamingModuleManager.isActive(RtspStreamingModule.Id)) {
            when (rtspEvent) {
                is RtspEvent.Intentable.StartService -> rtspStreamingModule.onServiceStart(this, rtspEvent.token)
                is RtspEvent.Intentable.StartProjection -> {
                    XLog.i(getLog("onStartCommand", "SP_TRACE route=preflight_v1 stage=service_dispatch event=StartProjection startId=$startId"))
                    rtspStreamingModule.startProjection(rtspEvent.intent)
                }
                is RtspEvent.Intentable.StopStream -> rtspStreamingModule.sendEvent(rtspEvent)
                RtspEvent.Intentable.RecoverError -> rtspStreamingModule.sendEvent(rtspEvent)
            }
        } else {
            XLog.w(getLog("onStartCommand", "Not active module. Stop self, startId: $startId"))
            stopSelf(startId)
        }

        return START_STICKY
    }

    private fun handleExternalStart(startId: Int) {
        XLog.i(getLog("handleExternalStart", "External start detected (shell/adb). Activating module and starting service."))
        
        // Check if module is already active
        if (!streamingModuleManager.isActive(RtspStreamingModule.Id)) {
            XLog.w(getLog("handleExternalStart", "Module not active. Service will remain in foreground but not stream until module is activated via UI."))
            // Keep service alive in foreground, but don't try to stream
            // User needs to open app at least once to initialize and grant screen capture permission
            return
        }
        
        // Module is active, start streaming
        try {
            rtspStreamingModule.onServiceStart(this, "external_shell_start")
            XLog.i(getLog("handleExternalStart", "Successfully started streaming from external call"))
        } catch (e: Exception) {
            XLog.e(getLog("handleExternalStart", "Failed to start streaming: ${e.message}"), e)
        }
    }

    override fun onDestroy() {
        XLog.d(getLog("onDestroy"))
        runBlocking { streamingModuleManager.stopModule(RtspStreamingModule.Id) }
        super.onDestroy()
    }

    @Throws(RtspError.NotificationPermissionRequired::class, IllegalStateException::class)
    internal fun startForeground(fgsType: Int) {
        XLog.d(getLog("startForeground", "foregroundNotificationsEnabled: ${notificationHelper.foregroundNotificationsEnabled()}"))

        if (notificationHelper.notificationPermissionGranted(this).not()) throw RtspError.NotificationPermissionRequired()

        startForeground(
            RtspEvent.Intentable.StopStream("RtspModuleService. User action: Notification").toIntent(this),
            fgsType
        )
    }

    internal fun showErrorNotification(error: RtspError) {
        if (error is RtspError.NotificationPermissionRequired) return

        if (error is RtspError.ServerError.AddressNotFoundException) {
            XLog.w(getLog("showErrorNotification", error.toString(this)))
        } else {
            XLog.e(getLog("showErrorNotification"), error)
        }

        showErrorNotification(
            message = error.toString(this),
            recoverIntent = RtspEvent.Intentable.RecoverError.toIntent(this)
        )
    }
}
