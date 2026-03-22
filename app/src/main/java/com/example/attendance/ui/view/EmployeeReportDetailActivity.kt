package com.example.attendance.ui.view

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.attendance.data.local.AppDatabase
import com.example.attendance.data.repository.AttendanceRepository
import com.example.attendance.databinding.ActivityEmployeeReportDetailBinding
import com.example.attendance.ui.adapter.DailyReportAdapter
import com.example.attendance.ui.viewmodel.ReportFilter
import com.example.attendance.ui.viewmodel.ReportsViewModel
import com.example.attendance.ui.viewmodel.ReportsViewModelFactory
import com.example.attendance.util.TimeUtils
import com.google.android.material.datepicker.MaterialDatePicker
import java.util.*

class EmployeeReportDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEmployeeReportDetailBinding
    private val viewModel: ReportsViewModel by viewModels {
        ReportsViewModelFactory(AttendanceRepository(AppDatabase.getDatabase(this).attendanceDao()))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEmployeeReportDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val employeeId = intent.getIntExtra("EMPLOYEE_ID", -1)
        val employeeName = intent.getStringExtra("EMPLOYEE_NAME") ?: "Employee"

        binding.tvDetailTitle.text = "$employeeName's Report"

        val adapter = DailyReportAdapter()
        binding.rvEmployeeDailyReports.layoutManager = LinearLayoutManager(this)
        binding.rvEmployeeDailyReports.adapter = adapter

        // Observe the same employeeReports but find the specific employee
        viewModel.employeeReports.observe(this) { reports ->
            val myReport = reports.find { it.employee.id == employeeId }
            myReport?.let {
                adapter.submitList(it.dailyAttendance)
            }
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
