package com.example.attendance.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.attendance.data.local.Attendance
import com.example.attendance.databinding.ItemAttendanceBinding
import com.example.attendance.util.TimeUtils

class AttendanceAdapter : ListAdapter<Attendance, AttendanceAdapter.AttendanceViewHolder>(AttendanceDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AttendanceViewHolder {
        val binding = ItemAttendanceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AttendanceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AttendanceViewHolder, position: Int) {
        val attendance = getItem(position)
        holder.bind(attendance)
    }

    inner class AttendanceViewHolder(private val binding: ItemAttendanceBinding) :
        RecyclerView.ViewHolder(binding.root) {
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

    class AttendanceDiffCallback : DiffUtil.ItemCallback<Attendance>() {
        override fun areItemsTheSame(oldItem: Attendance, newItem: Attendance): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Attendance, newItem: Attendance): Boolean =
            oldItem == newItem
    }
}
