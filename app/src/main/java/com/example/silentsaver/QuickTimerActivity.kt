package com.example.silentsaver

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import android.os.CountDownTimer
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.silentsaver.databinding.ActivityQuickTimerBinding
import com.example.silentsaver.db.AppDatabase
import com.example.silentsaver.db.HistoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

class QuickTimerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityQuickTimerBinding
    private var countDownTimer: CountDownTimer? = null
    private var isTimerRunning = false
    private var timeLeftInMillis: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQuickTimerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupButtons()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupButtons() {
        binding.btn15Min.setOnClickListener { startTimer(15) }
        binding.btn30Min.setOnClickListener { startTimer(30) }
        binding.btn1Hour.setOnClickListener { startTimer(60) }

        binding.btnCustomTime.setOnClickListener {
            showCustomTimeDialog()
        }

        binding.btnCancelTimer.setOnClickListener {
            cancelTimer()
        }
    }

    private fun showCustomTimeDialog() {
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_NUMBER
        input.hint = "Enter minutes (e.g. 5)"
        input.setPadding(50, 30, 50, 30)

        AlertDialog.Builder(this)
            .setTitle("Set Custom Time")
            .setMessage("Enter duration in minutes:")
            .setView(input)
            .setPositiveButton("Start") { _, _ ->
                val text = input.text.toString()
                if (text.isNotEmpty()) {
                    val minutes = text.toLongOrNull()
                    if (minutes != null && minutes > 0) {
                        startTimer(minutes)
                    } else {
                        Toast.makeText(this, "Please enter a valid number", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun startTimer(minutes: Long) {
        if (isTimerRunning) {
            cancelTimer()
        }

        val durationMillis = minutes * 60 * 1000

        // 1. Silence Phone
        silencePhone()

        // 2. Log to History
        logHistory("Quick Timer Started ($minutes min)")

        timeLeftInMillis = durationMillis
        isTimerRunning = true

        binding.tvTimerState.text = "Silent Mode Active"
        binding.btnCancelTimer.visibility = View.VISIBLE
        binding.layoutOptions.visibility = View.GONE

        countDownTimer = object : CountDownTimer(timeLeftInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftInMillis = millisUntilFinished
                updateCountDownText()
                val progress = (millisUntilFinished.toFloat() / durationMillis.toFloat() * 100).toInt()
                binding.progressBar.progress = progress
            }

            override fun onFinish() {
                isTimerRunning = false
                restorePhone()
                logHistory("Quick Timer Finished")

                binding.tvTimerState.text = "Finished"
                binding.tvCountdown.text = "00:00:00"
                binding.btnCancelTimer.visibility = View.GONE
                binding.layoutOptions.visibility = View.VISIBLE
                binding.progressBar.progress = 0

                Toast.makeText(this@QuickTimerActivity, "Timer Done! Normal Mode Restored.", Toast.LENGTH_LONG).show()
            }
        }.start()
    }

    private fun cancelTimer() {
        countDownTimer?.cancel()
        isTimerRunning = false
        restorePhone()
        logHistory("Quick Timer Cancelled")

        binding.tvTimerState.text = "Select Duration"
        binding.tvCountdown.text = "00:00:00"
        binding.btnCancelTimer.visibility = View.GONE
        binding.layoutOptions.visibility = View.VISIBLE
        binding.progressBar.progress = 0

        Toast.makeText(this, "Timer Cancelled", Toast.LENGTH_SHORT).show()
    }

    private fun updateCountDownText() {
        val hours = (timeLeftInMillis / 1000) / 3600
        val minutes = ((timeLeftInMillis / 1000) % 3600) / 60
        val seconds = (timeLeftInMillis / 1000) % 60
        val timeFormatted = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
        binding.tvCountdown.text = timeFormatted
    }

    private fun silencePhone() {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (notificationManager.isNotificationPolicyAccessGranted) {
            try {
                audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            Toast.makeText(this, "Grant DND Permission in Settings first", Toast.LENGTH_SHORT).show()
        }
    }

    private fun restorePhone() {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (notificationManager.isNotificationPolicyAccessGranted) {
            audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)
            audioManager.setStreamVolume(AudioManager.STREAM_RING, (maxVol * 0.8).toInt(), 0)
        }
    }

    private fun logHistory(title: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(this@QuickTimerActivity)
            val log = HistoryEntity(
                title = title,
                timestamp = System.currentTimeMillis(),
                duration = "Timer",
                type = "TIMER"
            )
            db.historyDao().insert(log)
        }
    }

    private val context: Context
        get() = this
}