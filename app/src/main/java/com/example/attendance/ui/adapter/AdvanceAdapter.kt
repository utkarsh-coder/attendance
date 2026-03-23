package com.example.attendance.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.attendance.data.local.Advance
import com.example.attendance.databinding.ItemAdvanceBinding
import com.example.attendance.util.TimeUtils
import java.util.Locale

class AdvanceAdapter(
    private val onEditClick: (Advance) -> Unit,
    private val onDeleteClick: (Advance) -> Unit
) : ListAdapter<Advance, AdvanceAdapter.AdvanceViewHolder>(AdvanceDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdvanceViewHolder {
        val binding = ItemAdvanceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AdvanceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AdvanceViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AdvanceViewHolder(private val binding: ItemAdvanceBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(advance: Advance) {
            binding.tvAdvanceAmount.text =
                String.format(Locale.getDefault(), "₹ %.2f", advance.amount)
            binding.tvAdvanceDescription.text = advance.description
            binding.tvAdvanceDate.text = TimeUtils.formatDate(advance.timestamp)
            binding.btnEditAdvance.setOnClickListener { onEditClick(advance) }
            binding.btnDeleteAdvance.setOnClickListener { onDeleteClick(advance) }
        }
    }

    class AdvanceDiffCallback : DiffUtil.ItemCallback<Advance>() {
        override fun areItemsTheSame(oldItem: Advance, newItem: Advance): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Advance, newItem: Advance): Boolean =
            oldItem == newItem
    }
}
