package com.example.silentsaver

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.silentsaver.databinding.ActivityWhitelistBinding
import com.example.silentsaver.db.AppDatabase
import com.example.silentsaver.db.ContactEntity
import kotlinx.coroutines.launch

class WhitelistActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWhitelistBinding
    private var vipContacts = listOf<ContactEntity>()
    private lateinit var adapter: WhitelistAdapter

    // 1. Define the Permissions we need
    private val requiredPermissions = arrayOf(
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.READ_CALL_LOG
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWhitelistBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupList()
        observeDatabase()

        binding.fabAddContact.setOnClickListener {
            checkPermissionsAndOpenPicker()
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupList() {
        adapter = WhitelistAdapter(vipContacts.toMutableList()) { contact, _ ->
            deleteContact(contact)
        }
        binding.rvContacts.layoutManager = LinearLayoutManager(this)
        binding.rvContacts.adapter = adapter
    }

    private fun observeDatabase() {
        val db = AppDatabase.getDatabase(this)
        lifecycleScope.launch {
            db.contactDao().getAllContacts().collect { list ->
                vipContacts = list
                adapter.updateData(list)
                checkEmptyState()
            }
        }
    }

    private fun deleteContact(contact: ContactEntity) {
        val db = AppDatabase.getDatabase(this)
        lifecycleScope.launch {
            db.contactDao().delete(contact)
            Toast.makeText(this@WhitelistActivity, "Removed from VIP", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkEmptyState() {
        if (vipContacts.isEmpty()) {
            binding.rvContacts.visibility = View.GONE
            binding.emptyStateView.visibility = View.VISIBLE
        } else {
            binding.rvContacts.visibility = View.VISIBLE
            binding.emptyStateView.visibility = View.GONE
        }
    }

    // --- PERMISSION LOGIC ---

    private fun checkPermissionsAndOpenPicker() {
        // Check if all permissions are granted
        val allGranted = requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) {
            launchContactPicker()
        } else {
            // Request missing permissions
            requestPermissionLauncher.launch(requiredPermissions)
        }
    }

    // This replaces the old launcher. It handles MULTIPLE permissions.
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Check if all requested permissions were granted
        val allGranted = permissions.entries.all { it.value }

        if (allGranted) {
            launchContactPicker()
        } else {
            Toast.makeText(this, "Permissions required to detect VIP calls", Toast.LENGTH_LONG).show()
        }
    }

    // --- CONTACT PICKER LOGIC ---

    private fun launchContactPicker() {
        val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
        contactPickerLauncher.launch(intent)
    }

    private val contactPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                processContactUri(uri)
            }
        }
    }

    @SuppressLint("Range")
    private fun processContactUri(contactUri: Uri) {
        val cursor: Cursor? = contentResolver.query(contactUri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val name = it.getString(it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                // Clean the number (remove spaces, dashes)
                var number = it.getString(it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
                number = number.replace("[^0-9+]".toRegex(), "") // Keep only digits and +

                saveContactToDb(name, number)
            }
        }
    }

    private fun saveContactToDb(name: String, number: String) {
        val db = AppDatabase.getDatabase(this)
        lifecycleScope.launch {
            val newContact = ContactEntity(name = name, phoneNumber = number)
            db.contactDao().insert(newContact)
            Toast.makeText(this@WhitelistActivity, "$name Added to VIP", Toast.LENGTH_SHORT).show()
        }
    }
}