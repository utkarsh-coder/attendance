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
import com.example.attendance.data.repository.AttendanceRepository
import com.example.attendance.databinding.ActivityEmployeeDetailBinding
import com.example.attendance.databinding.DialogEditAttendanceBinding
import com.example.attendance.databinding.DialogPasswordBinding
import com.example.attendance.ui.adapter.AttendanceAdapter
import com.example.attendance.ui.viewmodel.EmployeeDetailViewModel
import com.example.attendance.ui.viewmodel.EmployeeDetailViewModelFactory
import com.example.attendance.util.TimeUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.*

class EmployeeDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEmployeeDetailBinding
    private val viewModel: EmployeeDetailViewModel by viewModels {
        EmployeeDetailViewModelFactory(AttendanceRepository(AppDatabase.getDatabase(this).attendanceDao()))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityEmployeeDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Handle Edge-to-Edge window insets
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val employeeId = intent.getIntExtra("EMPLOYEE_ID", -1)
        val employeeName = intent.getStringExtra("EMPLOYEE_NAME") ?: "Unknown"

        if (employeeId == -1) {
            Toast.makeText(this, "Invalid Employee ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.tvDetailEmployeeName.text = employeeName

        val adapter = AttendanceAdapter(
            onEditClick = { attendance -> showEditDialog(attendance) },
            onDeleteClick = { attendance -> showDeleteConfirmation(attendance) }
        )
        binding.rvEmployeeAttendance.layoutManager = LinearLayoutManager(this)
        binding.rvEmployeeAttendance.adapter = adapter

        viewModel.getAttendanceFlow(employeeId).observe(this) { attendanceList ->
            adapter.submitList(attendanceList)
        }

        // Current time actions
        binding.btnCheckIn.setOnClickListener {
            viewModel.checkIn(employeeId)
            Toast.makeText(this, "Checked In", Toast.LENGTH_SHORT).show()
        }

        binding.btnCheckOut.setOnClickListener {
            viewModel.checkOut(employeeId)
            Toast.makeText(this, "Checked Out", Toast.LENGTH_SHORT).show()
        }

        // Custom time actions (Selecting only time, date defaults to today)
        binding.btnCheckInAt.setOnClickListener {
            showTimePicker(System.currentTimeMillis()) { calendar ->
                viewModel.checkIn(employeeId, calendar.timeInMillis)
                Toast.makeText(this, "Checked In at selected time", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnCheckOutAt.setOnClickListener {
            showTimePicker(System.currentTimeMillis()) { calendar ->
                viewModel.checkOut(employeeId, calendar.timeInMillis)
                Toast.makeText(this, "Checked Out at selected time", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showEditDialog(attendance: Attendance) {
        val dialogBinding = DialogEditAttendanceBinding.inflate(layoutInflater)
        
        var selectedCheckIn = attendance.checkInTime
        var selectedCheckOut = attendance.checkOutTime

        dialogBinding.btnEditCheckIn.text = "In: ${TimeUtils.formatTime(selectedCheckIn)}"
        dialogBinding.btnEditCheckOut.text = "Out: ${selectedCheckOut?.let { TimeUtils.formatTime(it) } ?: "--"}"

        dialogBinding.btnEditCheckIn.setOnClickListener {
            showTimePicker(selectedCheckIn) { calendar ->
                selectedCheckIn = calendar.timeInMillis
                dialogBinding.btnEditCheckIn.text = "In: ${TimeUtils.formatTime(selectedCheckIn)}"
            }
        }

        dialogBinding.btnEditCheckOut.setOnClickListener {
            showTimePicker(selectedCheckOut ?: System.currentTimeMillis()) { calendar ->
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

    private fun showTimePicker(initialTime: Long, onTimeSelected: (Calendar) -> Unit) {
        val calendar = Calendar.getInstance().apply { timeInMillis = initialTime }
        TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                val result = Calendar.getInstance().apply {
                    timeInMillis = initialTime
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
}
