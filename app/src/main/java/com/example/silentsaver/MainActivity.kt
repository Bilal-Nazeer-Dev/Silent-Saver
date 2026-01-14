package com.example.silentsaver

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import com.example.silentsaver.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    // 1. Declare the binding variable
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 2. Initialize View Binding
        binding = ActivityMainBinding.inflate(layoutInflater)

        // 3. CRITICAL FIX: Use binding.root here (not R.layout...)
        setContentView(binding.root)

        setupTheme()
        setupClickListeners()
        setupMasterSwitch()
    }

    private fun setupClickListeners() {
        // --- Navigation Logic for Dashboard Cards ---

        binding.cardActionLocations.setOnClickListener {
            startActivity(Intent(this, LocationsActivity::class.java))
        }

        binding.cardActionSchedule.setOnClickListener {
            startActivity(Intent(this, SchedulerActivity::class.java))
        }

        binding.cardActionTimer.setOnClickListener {
            startActivity(Intent(this, QuickTimerActivity::class.java))
        }

        binding.cardActionWhitelist.setOnClickListener {
            startActivity(Intent(this, WhitelistActivity::class.java))
        }

        binding.cardActionHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        binding.cardActionSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun setupMasterSwitch() {
        // Load saved state (default true)
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val isMasterActive = prefs.getBoolean("master_switch", true)

        binding.switchMaster.isChecked = isMasterActive
        updateStatusUI(isMasterActive)

        binding.switchMaster.setOnCheckedChangeListener { _, isChecked ->
            // Save new state
            prefs.edit().putBoolean("master_switch", isChecked).apply()
            updateStatusUI(isChecked)

            val message = if (isChecked) "SilentSaver Active" else "SilentSaver Paused"
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateStatusUI(isActive: Boolean) {
        if (isActive) {
            binding.tvStatusTitle.text = "All Systems Active"
            binding.tvStatusSubtitle.text = "Silent Saver is monitoring locations."
            binding.cardStatus.setCardBackgroundColor(getColor(R.color.primary_indigo))
        } else {
            binding.tvStatusTitle.text = "System Paused"
            binding.tvStatusSubtitle.text = "Automatic silencing is OFF."
            binding.cardStatus.setCardBackgroundColor(getColor(android.R.color.darker_gray))
        }
    }

    private fun setupTheme() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val isDarkMode = prefs.getBoolean("dark_mode", false)
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }
}