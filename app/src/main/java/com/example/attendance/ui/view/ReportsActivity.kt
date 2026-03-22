package com.example.attendance.ui.view

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.attendance.data.local.AppDatabase
import com.example.attendance.data.repository.AttendanceRepository
import com.example.attendance.databinding.ActivityReportsBinding
import com.example.attendance.ui.adapter.ReportsAdapter
import com.example.attendance.ui.viewmodel.ReportFilter
import com.example.attendance.ui.viewmodel.ReportsViewModel
import com.example.attendance.ui.viewmodel.ReportsViewModelFactory
import com.example.attendance.util.TimeUtils
import com.google.android.material.datepicker.MaterialDatePicker
import java.util.*

class ReportsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReportsBinding
    private val viewModel: ReportsViewModel by viewModels {
        ReportsViewModelFactory(AttendanceRepository(AppDatabase.getDatabase(this).attendanceDao()))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityReportsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val adapter = ReportsAdapter { report ->
            val intent = Intent(this, EmployeeReportDetailActivity::class.java).apply {
                putExtra("EMPLOYEE_ID", report.employee.id)
                putExtra("EMPLOYEE_NAME", report.employee.name)
            }
            startActivity(intent)
        }
        
        binding.rvAllReports.layoutManager = LinearLayoutManager(this)
        binding.rvAllReports.adapter = adapter

        viewModel.employeeReports.observe(this) { reports ->
            adapter.submitList(reports)
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
            .setSelection(
                androidx.core.util.Pair(
                    MaterialDatePicker.todayInUtcMilliseconds(),
                    MaterialDatePicker.todayInUtcMilliseconds()
                )
            )
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
        dateRangePicker.addOnCancelListener { binding.chipAll.isChecked = true }
        dateRangePicker.show(supportFragmentManager, "DATE_RANGE_PICKER")
    }
}
