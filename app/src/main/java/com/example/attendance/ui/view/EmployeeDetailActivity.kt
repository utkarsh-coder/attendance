package com.example.attendance.ui.view

import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.attendance.data.local.AppDatabase
import com.example.attendance.data.repository.AttendanceRepository
import com.example.attendance.databinding.ActivityEmployeeDetailBinding
import com.example.attendance.ui.adapter.EmployeeDetailPagerAdapter
import com.example.attendance.ui.viewmodel.EmployeeDetailViewModel
import com.example.attendance.ui.viewmodel.EmployeeDetailViewModelFactory
import com.google.android.material.tabs.TabLayoutMediator
import java.util.Calendar

class EmployeeDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEmployeeDetailBinding

    private val viewModel: EmployeeDetailViewModel by viewModels {
        EmployeeDetailViewModelFactory(
            AttendanceRepository(AppDatabase.getDatabase(this).attendanceDao())
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityEmployeeDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

        // ViewPager2 + TabLayout
        val pagerAdapter = EmployeeDetailPagerAdapter(this, employeeId)
        binding.viewPager.adapter = pagerAdapter
        // Disable swipe so scroll within each tab works cleanly
        binding.viewPager.isUserInputEnabled = false

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Attendance"
                1 -> "Advances"
                else -> ""
            }
        }.attach()

        // Check-in / Check-out buttons (always visible regardless of active tab)
        binding.btnCheckIn.setOnClickListener {
            viewModel.checkIn(employeeId)
            Toast.makeText(this, "Checked In", Toast.LENGTH_SHORT).show()
        }

        binding.btnCheckOut.setOnClickListener {
            viewModel.checkOut(employeeId)
            Toast.makeText(this, "Checked Out", Toast.LENGTH_SHORT).show()
        }

        binding.btnCheckInAt.setOnClickListener {
            showTimePicker(System.currentTimeMillis()) { cal ->
                viewModel.checkIn(employeeId, cal.timeInMillis)
                Toast.makeText(this, "Checked In at selected time", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnCheckOutAt.setOnClickListener {
            showTimePicker(System.currentTimeMillis()) { cal ->
                viewModel.checkOut(employeeId, cal.timeInMillis)
                Toast.makeText(this, "Checked Out at selected time", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showTimePicker(initialTime: Long, onSelected: (Calendar) -> Unit) {
        val cal = Calendar.getInstance().apply { timeInMillis = initialTime }
        TimePickerDialog(this, { _, h, m ->
            onSelected(Calendar.getInstance().apply {
                timeInMillis = initialTime
                set(Calendar.HOUR_OF_DAY, h)
                set(Calendar.MINUTE, m)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            })
        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
    }
}
