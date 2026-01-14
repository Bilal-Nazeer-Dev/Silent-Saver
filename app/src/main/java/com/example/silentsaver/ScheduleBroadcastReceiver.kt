package com.example.silentsaver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.widget.Toast
import com.example.silentsaver.db.AppDatabase
import com.example.silentsaver.db.HistoryEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ScheduleBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Check DND Permission first
        if (!notificationManager.isNotificationPolicyAccessGranted) {
            return // Cannot change settings without permission
        }

        val pendingResult = goAsync() // Keep alive for DB operations

        if (action == "ACTION_START_SILENCE") {
            // --- START TIME REACHED ---
            try {
                // Set to Vibrate (safer than full silent for tests)
                audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                Toast.makeText(context, "Scheduler: Silent Mode ON", Toast.LENGTH_LONG).show()
                logEvent(context, "Scheduled Silence Started", pendingResult)
            } catch (e: Exception) {
                pendingResult.finish()
            }
        }
        else if (action == "ACTION_END_SILENCE") {
            // --- END TIME REACHED ---
            try {
                audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                Toast.makeText(context, "Scheduler: Normal Mode Restored", Toast.LENGTH_LONG).show()
                logEvent(context, "Scheduled Silence Ended", pendingResult)
            } catch (e: Exception) {
                pendingResult.finish()
            }
        } else {
            pendingResult.finish()
        }
    }

    private fun logEvent(context: Context, title: String, pendingResult: PendingResult) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val log = HistoryEntity(
                    title = title,
                    timestamp = System.currentTimeMillis(),
                    duration = "Auto",
                    type = "SCHEDULE"
                )
                db.historyDao().insert(log)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }
}