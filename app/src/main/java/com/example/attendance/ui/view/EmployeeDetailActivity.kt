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
import com.example.attendance.data.repository.AttendanceRepository
import com.example.attendance.databinding.ActivityEmployeeDetailBinding
import com.example.attendance.ui.adapter.AttendanceAdapter
import com.example.attendance.ui.viewmodel.EmployeeDetailViewModel
import com.example.attendance.ui.viewmodel.EmployeeDetailViewModelFactory
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

        val adapter = AttendanceAdapter()
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
            showTimePicker { calendar ->
                viewModel.checkIn(employeeId, calendar.timeInMillis)
                Toast.makeText(this, "Checked In at selected time", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnCheckOutAt.setOnClickListener {
            showTimePicker { calendar ->
                viewModel.checkOut(employeeId, calendar.timeInMillis)
                Toast.makeText(this, "Checked Out at selected time", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Shows a TimePickerDialog to select only the time.
     * The date component of the resulting Calendar will be the current day.
     */
    private fun showTimePicker(onTimeSelected: (Calendar) -> Unit) {
        val currentCalendar = Calendar.getInstance()
        TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                selectedCalendar.set(Calendar.MINUTE, minute)
                selectedCalendar.set(Calendar.SECOND, 0)
                selectedCalendar.set(Calendar.MILLISECOND, 0)
                onTimeSelected(selectedCalendar)
            },
            currentCalendar.get(Calendar.HOUR_OF_DAY),
            currentCalendar.get(Calendar.MINUTE),
            true // 24-hour format
        ).show()
    }
}
