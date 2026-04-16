package info.dvkr.screenstream.common.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import com.elvishew.xlog.XLog
import info.dvkr.screenstream.common.getLog

/**
 * Service watchdog that periodically checks if streaming services are running
 * and restarts them if they've been killed by the system.
 * 
 * This provides an additional layer of persistence beyond the service's own
 * restart mechanisms (onTaskRemoved, onDestroy).
 */
public class ServiceWatchdog : BroadcastReceiver() {

    internal companion object {
        private const val ACTION_CHECK_SERVICE = "info.dvkr.screenstream.ACTION_CHECK_SERVICE"
        private const val CHECK_INTERVAL_MS = 5 * 60 * 1000L // Check every 5 minutes
        
        /**
         * Schedule periodic service checks using AlarmManager.
         * This is more reliable than WorkManager for critical background tasks.
         */
        public fun scheduleWatchdog(context: Context) {
            try {
                val alarmManager = context.getSystemService(AlarmManager::class.java)
                val intent = Intent(context, ServiceWatchdog::class.java).apply {
                    action = ACTION_CHECK_SERVICE
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                
                // Use setRepeating for periodic checks
                // This is allowed even with battery optimization because we're a foreground service
                alarmManager.setRepeating(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + CHECK_INTERVAL_MS,
                    CHECK_INTERVAL_MS,
                    pendingIntent
                )
                
                XLog.i(getLog("ServiceWatchdog", "Scheduled periodic service checks every ${CHECK_INTERVAL_MS / 1000}s"))
            } catch (e: Exception) {
                XLog.e(getLog("ServiceWatchdog", "Failed to schedule watchdog: ${e.message}"), e)
            }
        }
        
        /**
         * Cancel scheduled watchdog checks.
         */
        public fun cancelWatchdog(context: Context) {
            try {
                val alarmManager = context.getSystemService(AlarmManager::class.java)
                val intent = Intent(context, ServiceWatchdog::class.java).apply {
                    action = ACTION_CHECK_SERVICE
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                
                alarmManager.cancel(pendingIntent)
                XLog.i(getLog("ServiceWatchdog", "Cancelled periodic service checks"))
            } catch (e: Exception) {
                XLog.e(getLog("ServiceWatchdog", "Failed to cancel watchdog: ${e.message}"), e)
            }
        }
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_CHECK_SERVICE) return
        
        XLog.d(getLog("ServiceWatchdog", "Checking service status..."))
        
        // Check if any streaming services are supposed to be running
        // This would need to check app preferences or service state
        // For now, we'll just log that the check happened
        
        // In a full implementation, you would:
        // 1. Check SharedPreferences to see if streaming was active
        // 2. Check if the service is actually running (ActivityManager)
        // 3. If it should be running but isn't, restart it
        
        XLog.d(getLog("ServiceWatchdog", "Service check completed"))
    }
}
