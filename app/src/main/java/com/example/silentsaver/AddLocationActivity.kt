package com.example.silentsaver

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Geocoder
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.example.silentsaver.databinding.ActivityAddLocationBinding
import com.example.silentsaver.db.AppDatabase
import com.example.silentsaver.db.LocationEntity
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.Locale

class AddLocationActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityAddLocationBinding
    private lateinit var map: GoogleMap
    private lateinit var geofencingClient: GeofencingClient
    private lateinit var geofenceHelper: GeofenceHelper

    private var currentRadius = 50.0
    private var currentMarker: Circle? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddLocationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        geofencingClient = LocationServices.getGeofencingClient(this)
        geofenceHelper = GeofenceHelper(this)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        setupUI()
    }

    private fun setupUI() {
        // Radius Slider Logic
        binding.sliderRadius.addOnChangeListener { _, value, _ ->
            currentRadius = value.toDouble()
            binding.tvRadiusValue.text = "${currentRadius.toInt()}m"
            if (::map.isInitialized) {
                updateMapCircle(map.cameraPosition.target)
            }
        }

        // --- NEW: SEARCH BUTTON LOGIC ---
        binding.btnSearch.setOnClickListener {
            performSearch()
        }

        // --- NEW: KEYBOARD "ENTER" LOGIC ---
        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                true
            } else {
                false
            }
        }

        // Save Button Logic
        binding.btnSave.setOnClickListener {
            val name = binding.etName.text.toString()
            if (name.isNotEmpty()) {
                val center = map.cameraPosition.target
                saveLocationToDb(name, center, currentRadius.toFloat())
            } else {
                binding.tilName.error = "Enter a name (e.g. Office)"
            }
        }
    }

    private fun performSearch() {
        val searchText = binding.etSearch.text.toString()
        if (searchText.isEmpty()) return

        val geocoder = Geocoder(this, Locale.getDefault())

        try {
            // Find max 1 result
            val addressList = geocoder.getFromLocationName(searchText, 1)

            if (addressList != null && addressList.isNotEmpty()) {
                val address = addressList[0]
                val latLng = LatLng(address.latitude, address.longitude)

                // 1. Move Camera
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))

                // 2. Update the Blue Circle
                updateMapCircle(latLng)

                // 3. Auto-fill the Name box (User convenience)
                if (binding.etName.text.isNullOrEmpty()) {
                    // Use "Feature Name" (e.g., 'Badshahi Mosque') or fallback to search text
                    val placeName = if (!address.featureName.isNullOrEmpty()) address.featureName else searchText
                    binding.etName.setText(placeName)
                }

                Toast.makeText(this, "Found: ${address.getAddressLine(0)}", Toast.LENGTH_SHORT).show()

            } else {
                Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            Toast.makeText(this, "Network error: Can't search", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.uiSettings.isZoomControlsEnabled = false // Hide default buttons to look cleaner

        // Show Blue Dot if permitted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            map.isMyLocationEnabled = true

            // Zoom to user initially
            LocationServices.getFusedLocationProviderClient(this).lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    val latLng = LatLng(loc.latitude, loc.longitude)
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))
                    updateMapCircle(latLng)
                }
            }
        }

        // Sync Circle with Camera Movement
        map.setOnCameraMoveListener {
            val center = map.cameraPosition.target
            updateMapCircle(center)
        }
    }

    private fun updateMapCircle(center: LatLng) {
        currentMarker?.remove()
        val circleOptions = CircleOptions()
            .center(center)
            .radius(currentRadius)
            .strokeWidth(2f)
            .strokeColor(Color.parseColor("#1A237E"))
            .fillColor(Color.parseColor("#4000BFA5"))
        currentMarker = map.addCircle(circleOptions)
    }

    private fun saveLocationToDb(name: String, latLng: LatLng, radius: Float) {
        val db = AppDatabase.getDatabase(this)
        val newLocation = LocationEntity(
            name = name,
            latitude = latLng.latitude,
            longitude = latLng.longitude,
            radius = radius,
            isActive = true
        )

        lifecycleScope.launch {
            db.locationDao().insert(newLocation)
            addGeofence(latLng, radius, name)
        }
    }

    private fun addGeofence(latLng: LatLng, radius: Float, id: String) {
        // Reuse your existing Geofence Logic here...
        // (Copy the exact code from your previous version for addGeofence function)
        // Basic check for permissions before adding
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val geofence = geofenceHelper.getGeofence(id, latLng, radius, Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
            val request = geofenceHelper.getGeofencingRequest(geofence)
            val pendingIntent = geofenceHelper.pendingIntent

            geofencingClient.addGeofences(request, pendingIntent)
                .addOnSuccessListener {
                    Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}