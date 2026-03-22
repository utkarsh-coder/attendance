package com.example.attendance.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.attendance.databinding.ItemAttendanceBinding
import com.example.attendance.data.local.Attendance
import com.example.attendance.util.TimeUtils

class DailyAttendanceAdapter : ListAdapter<Attendance, DailyAttendanceAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAttendanceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(private val binding: ItemAttendanceBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(attendance: Attendance) {
            binding.tvAttendanceDate.text = TimeUtils.formatDate(attendance.checkInTime)
            binding.tvCheckInTime.text = "In: ${TimeUtils.formatTime(attendance.checkInTime)}"
            binding.tvCheckOutTime.text = if (attendance.checkOutTime != null) {
                "Out: ${TimeUtils.formatTime(attendance.checkOutTime!!)}"
            } else {
                "Out: --"
            }
            binding.tvDuration.text = "Duration: ${TimeUtils.calculateDuration(attendance.checkInTime, attendance.checkOutTime)}"
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Attendance>() {
        override fun areItemsTheSame(oldItem: Attendance, newItem: Attendance): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Attendance, newItem: Attendance): Boolean = oldItem == newItem
    }
}
