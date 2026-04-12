package info.dvkr.screenstream.mjpeg

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.elvishew.xlog.XLog
import info.dvkr.screenstream.common.getLog

public class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON" ||
            intent.action == "com.htc.intent.action.QUICKBOOT_POWERON"
        ) {
            XLog.i(getLog("BootReceiver", "Boot completed. Launching app for auto-start streaming."))

            val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                launchIntent.putExtra("AUTO_START_STREAMING", true)
                context.startActivity(launchIntent)
            } else {
                XLog.e(getLog("BootReceiver", "Could not find launch intent for package"))
            }
        }
    }
}
