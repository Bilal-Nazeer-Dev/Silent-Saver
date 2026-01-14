package com.example.silentsaver

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.silentsaver.databinding.ActivityLocationsBinding
import com.example.silentsaver.db.AppDatabase
import com.example.silentsaver.db.LocationEntity
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch

class LocationsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLocationsBinding
    private lateinit var adapter: LocationAdapter
    private var locationsList = listOf<LocationEntity>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLocationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupList()
        observeDatabase()

        binding.fabAdd.setOnClickListener {
            startActivity(Intent(this, AddLocationActivity::class.java))
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupList() {
        adapter = LocationAdapter(locationsList) { locationToDelete ->
            deleteLocation(locationToDelete)
        }
        binding.rvLocations.layoutManager = LinearLayoutManager(this)
        binding.rvLocations.adapter = adapter
    }

    private fun observeDatabase() {
        val db = AppDatabase.getDatabase(this)

        // Collect the Flow (Real-time updates)
        lifecycleScope.launch {
            db.locationDao().getAllLocations().collect { entities ->
                locationsList = entities
                adapter.updateData(locationsList)
                updateEmptyState()
            }
        }
    }

    private fun deleteLocation(location: LocationEntity) {
        val db = AppDatabase.getDatabase(this)
        val geofencingClient = LocationServices.getGeofencingClient(this)

        lifecycleScope.launch {
            // 1. Remove from Database
            db.locationDao().delete(location)

            // 2. Remove Geofence from Android System (Stop Silencing)
            // We use the location 'name' as the Request ID (as set in AddLocationActivity)
            geofencingClient.removeGeofences(listOf(location.name))
                .addOnSuccessListener {
                    Toast.makeText(this@LocationsActivity, "Location & Geofence Removed", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    // Even if geofence removal fails (e.g. not found), the DB delete is what matters visually
                    Toast.makeText(this@LocationsActivity, "Removed from list", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun updateEmptyState() {
        if (locationsList.isEmpty()) {
            binding.rvLocations.visibility = View.GONE
            binding.emptyStateView.visibility = View.VISIBLE
        } else {
            binding.rvLocations.visibility = View.VISIBLE
            binding.emptyStateView.visibility = View.GONE
        }
    }
}