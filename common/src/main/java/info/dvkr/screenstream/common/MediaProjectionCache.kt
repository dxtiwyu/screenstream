package info.dvkr.screenstream.common

import android.content.Context
import android.content.Intent
import android.os.Build
import com.elvishew.xlog.XLog

/**
 * Manages persistence of MediaProjection permission intent across app restarts and device reboots.
 * 
 * On Android 13 and below, the MediaProjection intent can be cached and reused, allowing
 * the app to start streaming after reboot without requiring the user to re-grant permission.
 * 
 * On Android 14+, Google removed this capability for security reasons, so permission must
 * be re-granted after each reboot.
 */
public object MediaProjectionCache {
    private const val PREFS_NAME = "media_projection_cache"
    private const val KEY_INTENT_URI = "cached_intent_uri"
    private const val KEY_TIMESTAMP = "cached_timestamp"
    
    /**
     * Save MediaProjection intent to persistent storage.
     * Only works on Android 13 and below.
     * 
     * @param context Application context
     * @param intent MediaProjection permission intent from onActivityResult
     */
    public fun saveIntent(context: Context, intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            XLog.d(getLog("MediaProjectionCache", "saveIntent: Android 14+ detected, caching disabled"))
            return
        }
        
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val intentUri = intent.toUri(Intent.URI_INTENT_SCHEME)
            prefs.edit()
                .putString(KEY_INTENT_URI, intentUri)
                .putLong(KEY_TIMESTAMP, System.currentTimeMillis())
                .apply()
            XLog.i(getLog("MediaProjectionCache", "saveIntent: Successfully cached MediaProjection intent"))
        } catch (e: Exception) {
            XLog.e(getLog("MediaProjectionCache", "saveIntent: Failed to cache intent: ${e.message}"), e)
        }
    }
    
    /**
     * Restore MediaProjection intent from persistent storage.
     * Only works on Android 13 and below.
     * 
     * @param context Application context
     * @return Cached intent if available and valid, null otherwise
     */
    public fun restoreIntent(context: Context): Intent? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            XLog.d(getLog("MediaProjectionCache", "restoreIntent: Android 14+ detected, caching disabled"))
            return null
        }
        
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val intentUri = prefs.getString(KEY_INTENT_URI, null)
            val timestamp = prefs.getLong(KEY_TIMESTAMP, 0)
            
            if (intentUri == null) {
                XLog.d(getLog("MediaProjectionCache", "restoreIntent: No cached intent found"))
                return null
            }
            
            val intent = Intent.parseUri(intentUri, Intent.URI_INTENT_SCHEME)
            val ageHours = (System.currentTimeMillis() - timestamp) / (1000 * 60 * 60)
            XLog.i(getLog("MediaProjectionCache", "restoreIntent: Found cached intent (age: ${ageHours}h)"))
            
            return intent
        } catch (e: Exception) {
            XLog.e(getLog("MediaProjectionCache", "restoreIntent: Failed to restore intent: ${e.message}"), e)
            clearIntent(context)
            return null
        }
    }
    
    /**
     * Clear cached MediaProjection intent.
     * Call this when the intent becomes invalid or permission is revoked.
     * 
     * @param context Application context
     */
    public fun clearIntent(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().clear().apply()
            XLog.i(getLog("MediaProjectionCache", "clearIntent: Cleared cached intent"))
        } catch (e: Exception) {
            XLog.e(getLog("MediaProjectionCache", "clearIntent: Failed: ${e.message}"), e)
        }
    }
    
    /**
     * Check if a cached intent is available.
     * 
     * @param context Application context
     * @return true if cached intent exists, false otherwise
     */
    public fun hasCachedIntent(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return false
        }
        
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.contains(KEY_INTENT_URI)
    }
}
