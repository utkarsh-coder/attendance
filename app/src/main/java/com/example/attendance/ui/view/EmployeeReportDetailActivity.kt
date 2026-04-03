package com.example.attendance.ui.view

import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.attendance.data.local.AppDatabase
import com.example.attendance.data.local.Attendance
import com.example.attendance.data.repository.AdvanceRepository
import com.example.attendance.data.repository.AttendanceRepository
import com.example.attendance.databinding.ActivityEmployeeReportDetailBinding
import com.example.attendance.databinding.DialogEditAttendanceBinding
import com.example.attendance.databinding.DialogPasswordBinding
import com.example.attendance.ui.adapter.DailyReportAdapter
import com.example.attendance.ui.viewmodel.ReportFilter
import com.example.attendance.ui.viewmodel.ReportsViewModel
import com.example.attendance.ui.viewmodel.ReportsViewModelFactory
import com.example.attendance.util.TimeUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.datepicker.MaterialDatePicker
import java.util.*

class EmployeeReportDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEmployeeReportDetailBinding
    private val viewModel: ReportsViewModel by viewModels {
        val database = AppDatabase.getDatabase(this)
        ReportsViewModelFactory(
            AttendanceRepository(database.attendanceDao()),
            AdvanceRepository(database.advanceDao())
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityEmployeeReportDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Handle Edge-to-Edge window insets for safe area
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val employeeId = intent.getIntExtra("EMPLOYEE_ID", -1)
        val employeeName = intent.getStringExtra("EMPLOYEE_NAME") ?: "Employee"

        binding.tvDetailTitle.text = "$employeeName's Report"

        val adapter = DailyReportAdapter(
            onEditClick = { attendance -> showEditDialog(attendance) },
            onDeleteClick = { attendance -> showDeleteConfirmation(attendance) }
        )
        binding.rvEmployeeDailyReports.layoutManager = LinearLayoutManager(this)
        binding.rvEmployeeDailyReports.adapter = adapter

        // Observe reports and update the list instantly
        viewModel.employeeReports.observe(this) { reports ->
            val myReport = reports.find { it.employee.id == employeeId }
            adapter.submitList(myReport?.dailyAttendance ?: emptyList())
        }

        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
            
            when (checkedIds.first()) {
                binding.chipAll.id -> {
                    viewModel.setFilter(ReportFilter.ALL)
                    binding.tvFilterDescription.text = "Showing all records"
                }
                binding.chipDaily.id -> {
                    viewModel.setFilter(ReportFilter.DAILY)
                    binding.tvFilterDescription.text = "Showing today's records"
                }
                binding.chipMonthly.id -> {
                    viewModel.setFilter(ReportFilter.MONTHLY)
                    binding.tvFilterDescription.text = "Showing this month's records"
                }
                binding.chipCustom.id -> {
                    showDateRangePicker()
                }
            }
        }
    }

    private fun showDeleteConfirmation(attendance: Attendance) {
        val dialogBinding = DialogPasswordBinding.inflate(layoutInflater)
        
        MaterialAlertDialogBuilder(this)
            .setTitle("Confirm Delete")
            .setMessage("Please enter password to delete this record.")
            .setView(dialogBinding.root)
            .setPositiveButton("Delete") { _, _ ->
                val password = dialogBinding.etPassword.text.toString()
                if (password == "1234") {
                    viewModel.deleteAttendance(attendance)
                    Toast.makeText(this, "Record deleted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Incorrect password", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditDialog(attendance: Attendance) {
        val dialogBinding = DialogEditAttendanceBinding.inflate(layoutInflater)
        
        var selectedCheckIn = attendance.checkInTime
        var selectedCheckOut = attendance.checkOutTime

        dialogBinding.btnEditCheckIn.text = "In: ${TimeUtils.formatTime(selectedCheckIn)}"
        dialogBinding.btnEditCheckOut.text = "Out: ${selectedCheckOut?.let { TimeUtils.formatTime(it) } ?: "--"}"

        dialogBinding.btnEditCheckIn.setOnClickListener {
            // When editing check-in, keep it on its original date
            showTimePicker(selectedCheckIn, selectedCheckIn) { calendar ->
                selectedCheckIn = calendar.timeInMillis
                dialogBinding.btnEditCheckIn.text = "In: ${TimeUtils.formatTime(selectedCheckIn)}"
            }
        }

        dialogBinding.btnEditCheckOut.setOnClickListener {
            // When editing check-out, always force it to be on the SAME DATE as the current check-in
            val baseTimeForCheckOut = selectedCheckIn
            showTimePicker(baseTimeForCheckOut, selectedCheckOut ?: selectedCheckIn) { calendar ->
                selectedCheckOut = calendar.timeInMillis
                dialogBinding.btnEditCheckOut.text = "Out: ${TimeUtils.formatTime(selectedCheckOut!!)}"
            }
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Edit Attendance")
            .setView(dialogBinding.root)
            .setPositiveButton("Update") { _, _ ->
                if (selectedCheckOut != null && selectedCheckOut!! < selectedCheckIn) {
                    Toast.makeText(this, "Check-out cannot be before check-in", Toast.LENGTH_SHORT).show()
                } else {
                    val updatedAttendance = attendance.copy(
                        checkInTime = selectedCheckIn,
                        checkOutTime = selectedCheckOut
                    )
                    viewModel.updateAttendance(updatedAttendance)
                    Toast.makeText(this, "Updated successfully", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showTimePicker(dateSourceTime: Long, initialTime: Long, onTimeSelected: (Calendar) -> Unit) {
        val calendar = Calendar.getInstance().apply { timeInMillis = initialTime }
        TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                val result = Calendar.getInstance().apply {
                    // Force the date to be the SAME as dateSourceTime (the original record's date)
                    timeInMillis = dateSourceTime
                    set(Calendar.HOUR_OF_DAY, hourOfDay)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                onTimeSelected(result)
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        ).show()
    }

    private fun showDateRangePicker() {
        val dateRangePicker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText("Select date range")
            .build()

        dateRangePicker.addOnPositiveButtonClickListener { selection ->
            val start = selection.first
            val end = selection.second
            if (start != null && end != null) {
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = end
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)

                viewModel.setCustomRange(start, calendar.timeInMillis)
                binding.tvFilterDescription.text = "Filtered from ${TimeUtils.formatDate(start)} to ${TimeUtils.formatDate(end)}"
            }
        }
        dateRangePicker.addOnNegativeButtonClickListener { binding.chipAll.isChecked = true }
        dateRangePicker.show(supportFragmentManager, "DATE_RANGE_PICKER")
    }
}
