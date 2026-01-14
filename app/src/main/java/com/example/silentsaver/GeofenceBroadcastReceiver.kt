package com.example.silentsaver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.util.Log
import android.widget.Toast
import androidx.preference.PreferenceManager
import com.example.silentsaver.db.AppDatabase
import com.example.silentsaver.db.HistoryEntity
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: return

        if (geofencingEvent.hasError()) {
            Log.e("GeofenceReceiver", "Error receiving geofence event: ${geofencingEvent.errorCode}")
            return
        }

        // 1. READ SETTINGS: Check user preferences
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val shouldVibrate = prefs.getBoolean("vibrate_on_silence", true)
        val showNotifications = prefs.getBoolean("notifications", true)

        val geofenceTransition = geofencingEvent.geofenceTransition
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        // 2. Use goAsync() to keep receiver alive for DB operations
        val pendingResult = goAsync()

        when (geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> {
                // --- ENTERING SILENT ZONE ---

                if (showNotifications) {
                    Toast.makeText(context, "Entering Silent Zone", Toast.LENGTH_LONG).show()
                }

                try {
                    // Logic: If user wants vibrate, use VIBRATE mode. Else use strict SILENT mode.
                    if (shouldVibrate) {
                        audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                    } else {
                        audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                    }

                    // Log to History
                    logEvent(context, "Entered Silent Zone", pendingResult)

                } catch (e: SecurityException) {
                    Log.e("GeofenceReceiver", "Permission missing for DND access")
                    pendingResult.finish()
                }
            }

            Geofence.GEOFENCE_TRANSITION_EXIT -> {
                // --- LEAVING SILENT ZONE ---

                if (showNotifications) {
                    Toast.makeText(context, "Leaving Silent Zone", Toast.LENGTH_LONG).show()
                }

                try {
                    // Restore to Normal (Ring)
                    audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL

                    // Log to History
                    logEvent(context, "Left Silent Zone", pendingResult)

                } catch (e: SecurityException) {
                    Log.e("GeofenceReceiver", "Permission missing for DND access")
                    pendingResult.finish()
                }
            }

            else -> {
                // Unknown transition
                pendingResult.finish()
            }
        }
    }

    private fun logEvent(context: Context, title: String, pendingResult: PendingResult) {
        // Run DB write in background thread
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val log = HistoryEntity(
                    title = title,
                    timestamp = System.currentTimeMillis(),
                    duration = "Auto",
                    type = "GEOFENCE"
                )
                db.historyDao().insert(log)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                // CRITICAL: Must finish pendingResult or the app might crash/ANR
                pendingResult.finish()
            }
        }
    }
}