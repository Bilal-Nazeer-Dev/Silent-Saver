package com.example.silentsaver

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.silentsaver.databinding.ActivitySchedulerBinding
import com.example.silentsaver.databinding.BottomSheetAddScheduleBinding
import com.example.silentsaver.databinding.ItemScheduleBinding
import com.example.silentsaver.db.AppDatabase
import com.example.silentsaver.db.ScheduleEntity
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

// --- ADAPTER WITH DELETE CLICK ---
class ScheduleAdapter(
    private var list: List<ScheduleEntity>,
    private val onDeleteClick: (ScheduleEntity) -> Unit // Callback function
) : RecyclerView.Adapter<ScheduleAdapter.Holder>() {

    class Holder(val binding: ItemScheduleBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemScheduleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = list[position]
        holder.binding.tvLabel.text = item.label
        holder.binding.tvTimeRange.text = item.timeRange
        holder.binding.tvDays.text = item.days
        holder.binding.switchSchedule.isChecked = item.isActive

        // Delete Button Click
        holder.binding.btnDelete.setOnClickListener {
            onDeleteClick(item)
        }
    }

    override fun getItemCount() = list.size

    fun updateList(newList: List<ScheduleEntity>) {
        list = newList
        notifyDataSetChanged()
    }
}

// --- MAIN ACTIVITY ---
class SchedulerActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySchedulerBinding
    private lateinit var adapter: ScheduleAdapter
    private lateinit var db: AppDatabase

    // Temp variables for creating new schedule
    private var startHour = 9; private var startMinute = 0
    private var endHour = 10; private var endMinute = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySchedulerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getDatabase(this)

        setupToolbar()
        setupList()
        checkAlarmPermission()

        // Observe Database
        lifecycleScope.launch {
            db.scheduleDao().getAllSchedules().collect { savedSchedules ->
                adapter.updateList(savedSchedules)
                updateEmptyState(savedSchedules)
            }
        }

        binding.fabAddSchedule.setOnClickListener {
            showAddScheduleSheet()
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupList() {
        // Initialize Adapter with Delete Logic
        adapter = ScheduleAdapter(emptyList()) { scheduleToDelete ->
            confirmDelete(scheduleToDelete)
        }
        binding.rvSchedules.layoutManager = LinearLayoutManager(this)
        binding.rvSchedules.adapter = adapter
    }

    private fun updateEmptyState(list: List<ScheduleEntity>) {
        binding.emptyStateView.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        binding.rvSchedules.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun checkAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                startActivity(intent)
            }
        }
    }

    // --- DELETE LOGIC ---
    private fun confirmDelete(schedule: ScheduleEntity) {
        AlertDialog.Builder(this)
            .setTitle("Delete Schedule")
            .setMessage("Are you sure you want to delete '${schedule.label}'?")
            .setPositiveButton("Delete") { _, _ ->
                performDelete(schedule)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performDelete(schedule: ScheduleEntity) {
        lifecycleScope.launch {
            // 1. Cancel Alarms (We loop 1-7 to ensure all possible days for this ID are killed)
            cancelAlarmsForSchedule(schedule.id)

            // 2. Remove from DB
            db.scheduleDao().deleteById(schedule.id)
            Toast.makeText(this@SchedulerActivity, "Schedule Deleted", Toast.LENGTH_SHORT).show()
        }
    }

    private fun cancelAlarmsForSchedule(scheduleId: Int) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Loop through all 7 days to ensure we catch any active alarm for this ID
        for (day in Calendar.SUNDAY..Calendar.SATURDAY) {
            val startReqCode = scheduleId * 100 + day
            val endReqCode = startReqCode + 50

            val startIntent = Intent(this, ScheduleBroadcastReceiver::class.java)
            val startPending = PendingIntent.getBroadcast(
                this, startReqCode, startIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(startPending)

            val endIntent = Intent(this, ScheduleBroadcastReceiver::class.java)
            val endPending = PendingIntent.getBroadcast(
                this, endReqCode, endIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(endPending)
        }
    }

    // --- ADD SCHEDULE LOGIC ---
    private fun showAddScheduleSheet() {
        val dialog = BottomSheetDialog(this)
        val sheetBinding = BottomSheetAddScheduleBinding.inflate(layoutInflater)
        dialog.setContentView(sheetBinding.root)

        // Reset
        startHour = 9; startMinute = 0
        endHour = 10; endMinute = 0

        sheetBinding.btnStartTime.setOnClickListener {
            val cal = Calendar.getInstance()
            TimePickerDialog(this, { _, h, m ->
                startHour = h; startMinute = m
                sheetBinding.btnStartTime.text = String.format("Start: %02d:%02d", h, m)
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show()
        }

        sheetBinding.btnEndTime.setOnClickListener {
            val cal = Calendar.getInstance()
            TimePickerDialog(this, { _, h, m ->
                endHour = h; endMinute = m
                sheetBinding.btnEndTime.text = String.format("End: %02d:%02d", h, m)
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show()
        }

        sheetBinding.btnSaveSchedule.setOnClickListener {
            val label = sheetBinding.etLabel.text.toString()

            // Collect Days
            val selectedDays = mutableListOf<Int>()
            val dayNames = mutableListOf<String>()

            if (sheetBinding.cbMon.isChecked) { selectedDays.add(Calendar.MONDAY); dayNames.add("Mon") }
            if (sheetBinding.cbTue.isChecked) { selectedDays.add(Calendar.TUESDAY); dayNames.add("Tue") }
            if (sheetBinding.cbWed.isChecked) { selectedDays.add(Calendar.WEDNESDAY); dayNames.add("Wed") }
            if (sheetBinding.cbThu.isChecked) { selectedDays.add(Calendar.THURSDAY); dayNames.add("Thu") }
            if (sheetBinding.cbFri.isChecked) { selectedDays.add(Calendar.FRIDAY); dayNames.add("Fri") }
            if (sheetBinding.cbSat.isChecked) { selectedDays.add(Calendar.SATURDAY); dayNames.add("Sat") }
            if (sheetBinding.cbSun.isChecked) { selectedDays.add(Calendar.SUNDAY); dayNames.add("Sun") }

            if (label.isNotEmpty() && selectedDays.isNotEmpty()) {
                val timeRange = String.format("%02d:%02d - %02d:%02d", startHour, startMinute, endHour, endMinute)
                val daysString = dayNames.joinToString(", ")

                val newSchedule = ScheduleEntity(
                    label = label,
                    timeRange = timeRange,
                    days = daysString,
                    isActive = true,
                    startHour = startHour, startMinute = startMinute,
                    endHour = endHour, endMinute = endMinute
                )

                lifecycleScope.launch {
                    val id = db.scheduleDao().insert(newSchedule)

                    // Register Alarms
                    for (day in selectedDays) {
                        scheduleWeeklyAlarm(day, startHour, startMinute, endHour, endMinute, id.toInt())
                    }

                    Toast.makeText(this@SchedulerActivity, "Saved!", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
            } else {
                Toast.makeText(this, "Add label & select days", Toast.LENGTH_SHORT).show()
            }
        }
        dialog.show()
    }

    @SuppressLint("ScheduleExactAlarm")
    private fun scheduleWeeklyAlarm(dayOfWeek: Int, sHour: Int, sMin: Int, eHour: Int, eMin: Int, scheduleId: Int) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val startReqCode = scheduleId * 100 + dayOfWeek
        val endReqCode = startReqCode + 50

        // Start Alarm
        val startIntent = Intent(this, ScheduleBroadcastReceiver::class.java).apply { action = "ACTION_START_SILENCE" }
        val startPending = PendingIntent.getBroadcast(this, startReqCode, startIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val startCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, sHour); set(Calendar.MINUTE, sMin); set(Calendar.SECOND, 0); set(Calendar.DAY_OF_WEEK, dayOfWeek)
            if (timeInMillis < System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 7)
        }

        // End Alarm
        val endIntent = Intent(this, ScheduleBroadcastReceiver::class.java).apply { action = "ACTION_END_SILENCE" }
        val endPending = PendingIntent.getBroadcast(this, endReqCode, endIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val endCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, eHour); set(Calendar.MINUTE, eMin); set(Calendar.SECOND, 0); set(Calendar.DAY_OF_WEEK, dayOfWeek)
            if (timeInMillis < startCal.timeInMillis) add(Calendar.DAY_OF_YEAR, 1)
            if (timeInMillis < System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 7)
        }

        val weekly = AlarmManager.INTERVAL_DAY * 7
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, startCal.timeInMillis, weekly, startPending)
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, endCal.timeInMillis, weekly, endPending)
    }
}