package com.example.silentsaver

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.silentsaver.databinding.ActivityOnboardingBinding

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private var isLocationGranted = false
    private var isDndGranted = false

    // 1. POPUP LAUNCHER
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocation = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocation = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineLocation || coarseLocation) {
            checkPermissionsState()
            Toast.makeText(this, "Permission Granted!", Toast.LENGTH_SHORT).show()
        } else {
            // If denied, we now guide them to settings manually
            Toast.makeText(this, "Permission Denied. Please enable it in Settings.", Toast.LENGTH_LONG).show()
            openAppSettings()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        checkPermissionsState()
    }

    private fun setupListeners() {
        // --- LOCATION BUTTON ---
        binding.btnAllowLocation.setOnClickListener {
            // DEBUG TOAST: This proves the button was clicked
            Toast.makeText(this, "Requesting Location...", Toast.LENGTH_SHORT).show()

            if (isLocationGranted) {
                Toast.makeText(this, "Already Allowed!", Toast.LENGTH_SHORT).show()
            } else {
                // Launch the popup
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }

        // --- DND BUTTON ---
        binding.btnAllowDnd.setOnClickListener {
            if (isDndGranted) {
                Toast.makeText(this, "Already Allowed!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Find 'Silent Saver' and turn ON", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                startActivity(intent)
            }
        }

        // --- FINISH BUTTON ---
        binding.btnFinishOnboarding.setOnClickListener {
            if (isLocationGranted && isDndGranted) {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, "Permissions Missing", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        startActivity(intent)
    }

    private fun checkPermissionsState() {
        // Check Location
        isLocationGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        updateButtonVisuals(binding.btnAllowLocation, isLocationGranted)

        // Check DND
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        isDndGranted = notificationManager.isNotificationPolicyAccessGranted

        updateButtonVisuals(binding.btnAllowDnd, isDndGranted)

        if (isLocationGranted && isDndGranted) {
            binding.btnFinishOnboarding.alpha = 1.0f
            binding.btnFinishOnboarding.text = "GET STARTED"
            binding.btnFinishOnboarding.isEnabled = true
        } else {
            binding.btnFinishOnboarding.alpha = 0.5f
        }
    }

    private fun updateButtonVisuals(button: com.google.android.material.button.MaterialButton, isGranted: Boolean) {
        if (isGranted) {
            button.text = "DONE"
            button.setBackgroundColor(ContextCompat.getColor(this, R.color.teal_accent))
            button.isEnabled = false
        } else {
            button.text = "ALLOW"
            button.setBackgroundColor(ContextCompat.getColor(this, R.color.teal_accent))
            button.isEnabled = true
        }
    }
}