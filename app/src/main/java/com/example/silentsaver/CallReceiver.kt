package com.example.silentsaver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.telephony.TelephonyManager
import android.util.Log
import android.widget.Toast
import com.example.silentsaver.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)

            if (state == TelephonyManager.EXTRA_STATE_RINGING) {
                val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

                if (incomingNumber != null) {
                    Log.d("SilentSaver", "Ringing! Incoming: $incomingNumber")
                    checkVipAndRing(context, incomingNumber)
                } else {
                    Log.e("SilentSaver", "Incoming number is NULL. Permission might be missing.")
                }
            }
        }
    }

    private fun checkVipAndRing(context: Context, incomingRaw: String) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)

                // 1. Normalize Incoming Number: Take last 10 digits (e.g. "3001234567")
                // This removes +92, 0, -, space, etc.
                val cleanIncoming = incomingRaw.replace("[^0-9]".toRegex(), "")
                val matchIncoming = if (cleanIncoming.length > 10) cleanIncoming.takeLast(10) else cleanIncoming

                // 2. Fetch all VIPs and check manually (Safest way)
                val vipList = db.contactDao().getAllContactsList() // We need to add this query to DAO

                var isVip = false
                for (contact in vipList) {
                    val cleanStored = contact.phoneNumber.replace("[^0-9]".toRegex(), "")
                    val matchStored = if (cleanStored.length > 10) cleanStored.takeLast(10) else cleanStored

                    if (matchIncoming == matchStored) {
                        isVip = true
                        break
                    }
                }

                if (isVip) {
                    Log.d("SilentSaver", "VIP FOUND! Unsilencing...")
                    disableDndAndRing(context)
                }
            } catch (e: Exception) {
                Log.e("SilentSaver", "Error checking VIP", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun disableDndAndRing(context: Context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 1. Turn off DND (Interruption Filter) if it's on
        if (notificationManager.isNotificationPolicyAccessGranted) {
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
        }

        // 2. Force Ringer Mode to Normal
        if (audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL) {
            audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
        }

        // 3. Max Volume
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)
        audioManager.setStreamVolume(AudioManager.STREAM_RING, maxVol, 0)
    }
}