package com.example.silentsaver

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.silentsaver.databinding.ActivityHistoryBinding
import com.example.silentsaver.db.AppDatabase
import com.example.silentsaver.db.HistoryEntity
import kotlinx.coroutines.launch

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private lateinit var adapter: HistoryAdapter
    private var historyList = listOf<HistoryEntity>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupList()
        observeDatabase()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    // 1. Inflate the Menu (Show Trash Icon)
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_history, menu)
        return true
    }

    // 2. Handle Menu Clicks
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_clear_history -> {
                showClearConfirmation()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showClearConfirmation() {
        if (historyList.isEmpty()) {
            Toast.makeText(this, "History is already empty", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Clear History")
            .setMessage("Are you sure you want to delete all logs?")
            .setPositiveButton("Delete") { _, _ ->
                clearAllHistory()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun clearAllHistory() {
        val db = AppDatabase.getDatabase(this)
        lifecycleScope.launch {
            db.historyDao().deleteAll()
            Toast.makeText(this@HistoryActivity, "History Cleared", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupList() {
        adapter = HistoryAdapter(emptyList())
        binding.rvHistory.layoutManager = LinearLayoutManager(this)
        binding.rvHistory.adapter = adapter
    }

    private fun observeDatabase() {
        val db = AppDatabase.getDatabase(this)

        lifecycleScope.launch {
            db.historyDao().getAllHistory().collect { list ->
                historyList = list
                adapter.updateData(list)
                updateStats(list)
            }
        }
    }

    private fun updateStats(list: List<HistoryEntity>) {
        val totalEvents = list.size
        // Count events from last 7 days
        val oneWeekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
        val recentEvents = list.count { it.timestamp > oneWeekAgo }

        binding.tvWeeklyCount.text = recentEvents.toString()
        binding.tvTotalCount.text = totalEvents.toString()
    }
}