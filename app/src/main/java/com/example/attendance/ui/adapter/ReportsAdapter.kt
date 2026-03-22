package com.example.attendance.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.attendance.databinding.ItemReportBinding
import com.example.attendance.ui.viewmodel.EmployeeReport
import java.util.*

class ReportsAdapter(private val onClick: (EmployeeReport) -> Unit) : ListAdapter<EmployeeReport, ReportsAdapter.ReportViewHolder>(ReportDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val binding = ItemReportBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ReportViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        val report = getItem(position)
        holder.bind(report)
        holder.itemView.setOnClickListener { onClick(report) }
    }

    class ReportViewHolder(private val binding: ItemReportBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(report: EmployeeReport) {
            binding.tvReportEmployeeName.text = report.employee.name
            
            // Calculate total hours across all daily reports in this filtered view
            var totalMillis = 0L
            report.dailyAttendance.forEach { daily ->
                daily.records.forEach { record ->
                    val diff = (record.checkOutTime ?: 0L) - record.checkInTime
                    if (diff > 0) totalMillis += diff
                }
            }
            
            val hours = totalMillis / (1000 * 60 * 60)
            val minutes = (totalMillis / (1000 * 60)) % 60
            binding.tvTotalHours.text = String.format(Locale.getDefault(), "%02d:%02d hrs", hours, minutes)
        }
    }

    class ReportDiffCallback : DiffUtil.ItemCallback<EmployeeReport>() {
        override fun areItemsTheSame(oldItem: EmployeeReport, newItem: EmployeeReport): Boolean =
            oldItem.employee.id == newItem.employee.id

        override fun areContentsTheSame(oldItem: EmployeeReport, newItem: EmployeeReport): Boolean =
            oldItem == newItem
    }
}
