package com.example.attendance.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.attendance.data.local.Attendance
import com.example.attendance.databinding.ItemDailyReportBinding
import com.example.attendance.ui.viewmodel.DailyAttendance

class DailyReportAdapter(
    private val onEditClick: (Attendance) -> Unit,
    private val onDeleteClick: (Attendance) -> Unit
) : ListAdapter<DailyAttendance, DailyReportAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDailyReportBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding, onEditClick, onDeleteClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemDailyReportBinding,
        private val onEditClick: (Attendance) -> Unit,
        private val onDeleteClick: (Attendance) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        
        private var innerAdapter: DailyAttendanceAdapter? = null

        fun bind(daily: DailyAttendance) {
            binding.tvReportDate.text = daily.date
            binding.tvTotalDuration.text = "Total: ${daily.totalDuration}"
            
            if (innerAdapter == null) {
                innerAdapter = DailyAttendanceAdapter(onEditClick, onDeleteClick)
                binding.rvDailyRecords.layoutManager = LinearLayoutManager(binding.root.context)
                binding.rvDailyRecords.adapter = innerAdapter
            }
            
            // Explicitly pass a fresh list instance to ensure the inner adapter updates instantly
            innerAdapter?.submitList(daily.records.toList())
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<DailyAttendance>() {
        override fun areItemsTheSame(oldItem: DailyAttendance, newItem: DailyAttendance): Boolean = 
            oldItem.date == newItem.date
            
        override fun areContentsTheSame(oldItem: DailyAttendance, newItem: DailyAttendance): Boolean = 
            oldItem == newItem
    }
}
