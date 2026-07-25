package info.dvkr.screenstream.common.module

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.ServiceCompat
import com.elvishew.xlog.XLog
import info.dvkr.screenstream.common.getLog
import info.dvkr.screenstream.common.notification.NotificationHelper
import org.koin.android.ext.android.inject
import java.util.UUID

public abstract class StreamingModuleService : Service() {

    protected abstract val notificationIdForeground: Int
    protected abstract val notificationIdError: Int

    protected val streamingModuleManager: StreamingModuleManager by inject(mode = LazyThreadSafetyMode.NONE)
    protected val notificationHelper: NotificationHelper by inject(mode = LazyThreadSafetyMode.NONE)

    protected val processedIntents: MutableSet<String> = mutableSetOf()
    private var wakeLock: PowerManager.WakeLock? = null

    @Suppress("RedundantVisibilityModifier")
    protected companion object {
        public const val INTENT_ID: String = "info.dvkr.screenstream.intent.ID"

        public fun Intent.addIntentId(): Intent = putExtra(INTENT_ID, UUID.randomUUID().toString())
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        XLog.d(getLog("onCreate"))
        acquireWakeLock()
    }

    @SuppressLint("WakelockTimeout")
    private fun acquireWakeLock() {
        try {
            val pm = getSystemService(PowerManager::class.java)
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ScreenStream::ServiceWake").apply {
                acquire()
            }
            XLog.d(getLog("acquireWakeLock", "WakeLock acquired"))
        } catch (e: Exception) {
            XLog.e(getLog("acquireWakeLock", "Failed: ${e.message}"))
        }
    }

    override fun onDestroy() {
        XLog.w(getLog("onDestroy", "Service being destroyed - attempting restart"))
        
        // Don't stop foreground or release wake lock yet - keep service alive as long as possible
        hideErrorNotification()
        
        // Self-restart for persistence across SystemUI restarts and kills
        // This is CRITICAL for background streaming persistence
        try {
            val restartIntent = Intent(this, this::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(restartIntent)
                XLog.i(getLog("onDestroy", "Scheduled foreground service restart"))
            } else {
                startService(restartIntent)
                XLog.i(getLog("onDestroy", "Scheduled service restart"))
            }
        } catch (e: Exception) {
            XLog.e(getLog("onDestroy", "Self-restart failed: ${e.message}"), e)
        }
        
        // Now clean up
        stopForeground()
        releaseWakeLock()
        super.onDestroy()
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
                XLog.d(getLog("releaseWakeLock", "WakeLock released"))
            }
        } catch (e: Exception) {
            XLog.e(getLog("releaseWakeLock", "Failed: ${e.message}"))
        }
        wakeLock = null
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Restart service when app is swiped from recents
        // This is CRITICAL for background streaming persistence
        XLog.w(getLog("onTaskRemoved", "Task removed from recents - restarting service to maintain streaming"))
        
        super.onTaskRemoved(rootIntent)
        
        try {
            val restartIntent = Intent(this, this::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(restartIntent)
                XLog.i(getLog("onTaskRemoved", "Scheduled foreground service restart"))
            } else {
                startService(restartIntent)
                XLog.i(getLog("onTaskRemoved", "Scheduled service restart"))
            }
        } catch (e: Exception) {
            XLog.e(getLog("onTaskRemoved", "Restart failed: ${e.message}"), e)
        }
    }

    protected fun isDuplicateIntent(intent: Intent): Boolean {
        val id = intent.getStringExtra(INTENT_ID)
        return when {
            id == null -> {
                XLog.w(getLog("isDuplicateIntent", "No intent ID provided"))
                false
            }
            processedIntents.contains(id) -> {
                XLog.w(getLog("isDuplicateIntent", "Duplicate intent ID: $id"))
                true
            }
            else -> {
                processedIntents.add(id)
                false
            }
        }
    }

    @SuppressLint("InlinedApi")
    protected fun startForeground(stopIntent: Intent, serviceType: Int) {
        val notification = notificationHelper.createForegroundNotification(this, stopIntent)
        ServiceCompat.startForeground(this, notificationIdForeground, notification, serviceType)
    }

    public fun stopForeground() {
        XLog.d(getLog("stopForeground"))

        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
    }

    protected fun showErrorNotification(message: String, recoverIntent: Intent?) {
        hideErrorNotification()

        if (notificationHelper.notificationPermissionGranted(this).not()) {
            XLog.e(getLog("showErrorNotification", "No permission granted. Ignoring."))
            return
        }

        if (notificationHelper.errorNotificationsEnabled().not()) {
            XLog.e(getLog("showErrorNotification", "Notifications disabled. Ignoring."))
            return
        }

        val notification = notificationHelper.getErrorNotification(this, message, recoverIntent)
        notificationHelper.showNotification(notificationIdError, notification)
    }

    public fun hideErrorNotification() {
        XLog.d(getLog("hideErrorNotification"))

        notificationHelper.cancelNotification(notificationIdError)
    }
}
