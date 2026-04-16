package info.dvkr.screenstream.mjpeg

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import com.elvishew.xlog.XLog
import info.dvkr.screenstream.common.getLog
import info.dvkr.screenstream.common.module.StreamingModuleService
import info.dvkr.screenstream.mjpeg.internal.MjpegEvent
import info.dvkr.screenstream.mjpeg.ui.MjpegError
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.inject

public class MjpegModuleService : StreamingModuleService() {

    internal companion object {
        internal fun getIntent(context: Context): Intent = Intent(context, MjpegModuleService::class.java).addIntentId()

        internal fun startService(context: Context, intent: Intent) {
            XLog.d(getLog("MjpegModuleService.startService", "Run intent: ${intent.extras}"))
            val importance = ActivityManager.RunningAppProcessInfo().also { ActivityManager.getMyMemoryState(it) }.importance
            XLog.i(getLog("MjpegModuleService.startService", "RunningAppProcessInfo.importance: $importance"))
            context.startService(intent)
        }

        internal fun startProjection(context: Context, permissionIntent: Intent, source: String = "ui_permission") {
            val intent = MjpegEvent.Intentable.StartProjection(permissionIntent).toIntent(context)
            XLog.d(getLog("MjpegModuleService.startProjection", "Run intent: ${intent.extras}"))
            val importance = ActivityManager.RunningAppProcessInfo().also { ActivityManager.getMyMemoryState(it) }.importance
            XLog.i(getLog("MjpegModuleService.startProjection", "RunningAppProcessInfo.importance: $importance"))
            XLog.i(getLog("MjpegModuleService.startProjection", "SP_TRACE route=preflight_v1 stage=service_command source=$source importance=$importance"))
            context.startService(intent)
        }
    }

    override val notificationIdForeground: Int = 100
    override val notificationIdError: Int = 110

    private val mjpegStreamingModule: MjpegStreamingModule by inject(MjpegKoinQualifier, LazyThreadSafetyMode.NONE)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // CRITICAL: Call startForeground() immediately to avoid ANR on Android 8.0+ (API 26+)
        // Services must call startForeground() within 5 seconds or system kills them
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val stopIntent = MjpegEvent.Intentable.StopStream("MjpegModuleService. User action: Notification").toIntent(this)
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

        XLog.d(getLog("onStartCommand", "MjpegModuleService.INTENT_ID: ${intent.getStringExtra(INTENT_ID)}"))

        val mjpegEvent = MjpegEvent.Intentable.fromIntent(intent)
        if (mjpegEvent == null) {
            XLog.w(getLog("onStartCommand", "MjpegEvent is null - treating as external start request"))
            // Handle external shell command (am startservice)
            handleExternalStart(startId)
            return START_STICKY
        }

        XLog.d(getLog("onStartCommand", "MjpegEvent: $mjpegEvent, startId: $startId"))

        val shouldDedupe = mjpegEvent is MjpegEvent.Intentable.StartService
        if (shouldDedupe && isDuplicateIntent(intent)) {
            XLog.i(getLog("onStartCommand", "Duplicate intent for $mjpegEvent. Ignoring. startId: $startId"))
            return START_STICKY
        }

        if ((flags and START_FLAG_REDELIVERY) != 0) {
            XLog.e(getLog("onStartCommand"), IllegalArgumentException("MjpegModuleService.onStartCommand: redelivered intent, MjpegEvent: $mjpegEvent, startId: $startId, $intent"))
            return START_STICKY
        }

        if (streamingModuleManager.isActive(MjpegStreamingModule.Id)) {
            when (mjpegEvent) {
                is MjpegEvent.Intentable.StartService -> mjpegStreamingModule.onServiceStart(this, mjpegEvent.token)
                is MjpegEvent.Intentable.StartProjection -> {
                    XLog.i(getLog("onStartCommand", "SP_TRACE route=preflight_v1 stage=service_dispatch event=StartProjection startId=$startId"))
                    mjpegStreamingModule.startProjection(mjpegEvent.intent)
                }
                is MjpegEvent.Intentable.StopStream -> mjpegStreamingModule.sendEvent(mjpegEvent)
                MjpegEvent.Intentable.RecoverError -> mjpegStreamingModule.sendEvent(mjpegEvent)
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
        if (!streamingModuleManager.isActive(MjpegStreamingModule.Id)) {
            XLog.w(getLog("handleExternalStart", "Module not active. Service will remain in foreground but not stream until module is activated via UI."))
            // Keep service alive in foreground, but don't try to stream
            // User needs to open app at least once to initialize and grant screen capture permission
            return
        }
        
        // Module is active, start streaming
        try {
            mjpegStreamingModule.onServiceStart(this, "external_shell_start")
            XLog.i(getLog("handleExternalStart", "Successfully started streaming from external call"))
        } catch (e: Exception) {
            XLog.e(getLog("handleExternalStart", "Failed to start streaming: ${e.message}"), e)
        }
    }

    override fun onDestroy() {
        XLog.d(getLog("onDestroy"))
        runBlocking { streamingModuleManager.stopModule(MjpegStreamingModule.Id) }
        super.onDestroy()
    }

    @Throws(MjpegError.NotificationPermissionRequired::class, IllegalStateException::class)
    internal fun startForeground(fgsType: Int) {
        XLog.d(getLog("startForeground", "foregroundNotificationsEnabled: ${notificationHelper.foregroundNotificationsEnabled()}"))

        if (notificationHelper.notificationPermissionGranted(this).not()) throw MjpegError.NotificationPermissionRequired()

        startForeground(
            MjpegEvent.Intentable.StopStream("MjpegModuleService. User action: Notification").toIntent(this),
            fgsType
        )
    }

    internal fun showErrorNotification(error: MjpegError) {
        if (error is MjpegError.NotificationPermissionRequired) return

        if (error is MjpegError.AddressNotFoundException || error is MjpegError.AddressInUseException) {
            XLog.i(getLog("showErrorNotification", "${error.javaClass.simpleName} ${error.cause}"))
        } else {
            XLog.e(getLog("showErrorNotification"), error)
        }

        showErrorNotification(
            message = error.toString(this),
            recoverIntent = MjpegEvent.Intentable.RecoverError.toIntent(this)
        )
    }
}
