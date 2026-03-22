package com.example.attendance.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.attendance.databinding.ItemDailyReportBinding
import com.example.attendance.ui.viewmodel.DailyAttendance
import com.example.attendance.util.TimeUtils

class DailyReportAdapter : ListAdapter<DailyAttendance, DailyReportAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDailyReportBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(private val binding: ItemDailyReportBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(daily: DailyAttendance) {
            binding.tvReportDate.text = daily.date
            binding.tvTotalDuration.text = "Total: ${daily.totalDuration}"
            
            val recordsText = daily.records.joinToString("\n") { 
                "In: ${TimeUtils.formatTime(it.checkInTime)} - Out: ${it.checkOutTime?.let { t -> TimeUtils.formatTime(t) } ?: "--"}"
            }
            binding.tvDailyRecords.text = recordsText
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<DailyAttendance>() {
        override fun areItemsTheSame(oldItem: DailyAttendance, newItem: DailyAttendance): Boolean = oldItem.date == newItem.date
        override fun areContentsTheSame(oldItem: DailyAttendance, newItem: DailyAttendance): Boolean = oldItem == newItem
    }
}
