package com.example.attendance.ui.view

import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.attendance.data.local.AppDatabase
import com.example.attendance.data.repository.AdvanceRepository
import com.example.attendance.data.repository.AttendanceRepository
import com.example.attendance.databinding.ActivityReportsBinding
import com.example.attendance.ui.adapter.ReportsAdapter
import com.example.attendance.ui.viewmodel.EmployeeReport
import com.example.attendance.ui.viewmodel.ReportFilter
import com.example.attendance.ui.viewmodel.ReportsViewModel
import com.example.attendance.ui.viewmodel.ReportsViewModelFactory
import com.example.attendance.util.TimeUtils
import com.google.android.material.datepicker.MaterialDatePicker
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

class ReportsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReportsBinding
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
        binding = ActivityReportsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
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

        binding.btnSendReport.setOnClickListener {
            val reports = viewModel.employeeReports.value
            if (!reports.isNullOrEmpty()) {
                generateAndSendPdf(reports)
            } else {
                Toast.makeText(this, "No data to send", Toast.LENGTH_SHORT).show()
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

    private fun generateAndSendPdf(reports: List<EmployeeReport>) {
        val pdfDocument = PdfDocument()
        val titlePaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 18f
            color = Color.BLACK
        }
        val headerPaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 14f
            color = Color.BLACK
        }
        val textPaint = Paint().apply {
            textSize = 12f
            color = Color.BLACK
        }

        var pageNumber = 1
        var myPageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
        var myPage = pdfDocument.startPage(myPageInfo)
        var canvas: Canvas = myPage.canvas
        var y = 40f

        canvas.drawText("Attendance Report - ${binding.tvFilterDescription.text}", 40f, y, titlePaint)
        y += 40f

        for (report in reports) {
            if (y > 700) {
                pdfDocument.finishPage(myPage)
                pageNumber++
                myPageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                myPage = pdfDocument.startPage(myPageInfo)
                canvas = myPage.canvas
                y = 40f
            }

            canvas.drawText("Employee: ${report.employee.name}", 40f, y, headerPaint)
            y += 20f

            for (daily in report.dailyAttendance) {
                if (y > 780) {
                    pdfDocument.finishPage(myPage)
                    pageNumber++
                    myPageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                    myPage = pdfDocument.startPage(myPageInfo)
                    canvas = myPage.canvas
                    y = 40f
                }
                canvas.drawText("${daily.date}: ${daily.totalDuration}", 60f, y, textPaint)
                y += 15f
            }

            val totalHours = report.totalWorkingMillis / (1000 * 60 * 60)
            val totalMinutes = (report.totalWorkingMillis / (1000 * 60)) % 60
            val totalDurationStr = String.format(Locale.getDefault(), "%02d hrs %02d mins", totalHours, totalMinutes)
            
            canvas.drawText("Total Working Hours: $totalDurationStr", 40f, y, textPaint)
            y += 15f
            canvas.drawText("Total Advance: ${report.totalAdvance}", 40f, y, textPaint)
            y += 30f
        }

        pdfDocument.finishPage(myPage)

        val file = File(getExternalFilesDir(null), "Attendance_Report.pdf")
        try {
            pdfDocument.writeTo(FileOutputStream(file))
            sharePdf(file)
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Error generating PDF", Toast.LENGTH_SHORT).show()
        } finally {
            pdfDocument.close()
        }
    }

    private fun sharePdf(file: File) {
        val uri: Uri = FileProvider.getUriForFile(this, "$packageName.provider", file)
        
        // Remove specific package to allow system to show all apps if WhatsApp isn't preferred
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        val chooserIntent = Intent.createChooser(intent, "Share Attendance Report via")
        startActivity(chooserIntent)
    }
}
