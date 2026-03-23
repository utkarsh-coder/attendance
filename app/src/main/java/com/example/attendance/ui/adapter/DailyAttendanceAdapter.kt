package com.example.attendance.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.attendance.databinding.ItemAttendanceBinding
import com.example.attendance.data.local.Attendance
import com.example.attendance.util.TimeUtils

class DailyAttendanceAdapter(
    private val onEditClick: (Attendance) -> Unit,
    private val onDeleteClick: (Attendance) -> Unit
) : ListAdapter<Attendance, DailyAttendanceAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAttendanceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding, onEditClick, onDeleteClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemAttendanceBinding,
        private val onEditClick: (Attendance) -> Unit,
        private val onDeleteClick: (Attendance) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(attendance: Attendance) {
            binding.tvAttendanceDate.text = TimeUtils.formatDate(attendance.checkInTime)
            binding.tvCheckInTime.text = "In: ${TimeUtils.formatTime(attendance.checkInTime)}"
            binding.tvCheckOutTime.text = if (attendance.checkOutTime != null) {
                "Out: ${TimeUtils.formatTime(attendance.checkOutTime!!)}"
            } else {
                "Out: --"
            }
            binding.tvDuration.text = "Duration: ${TimeUtils.calculateDuration(attendance.checkInTime, attendance.checkOutTime)}"
            
            binding.btnEditAttendance.setOnClickListener {
                onEditClick(attendance)
            }
            
            binding.btnDeleteAttendance.setOnClickListener {
                onDeleteClick(attendance)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Attendance>() {
        override fun areItemsTheSame(oldItem: Attendance, newItem: Attendance): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Attendance, newItem: Attendance): Boolean = oldItem == newItem
    }
}
