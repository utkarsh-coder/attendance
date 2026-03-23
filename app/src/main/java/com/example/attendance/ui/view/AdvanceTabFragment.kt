package com.example.attendance.ui.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.attendance.data.local.Advance
import com.example.attendance.data.local.AppDatabase
import com.example.attendance.data.repository.AdvanceRepository
import com.example.attendance.databinding.DialogAddAdvanceBinding
import com.example.attendance.databinding.DialogPasswordBinding
import com.example.attendance.databinding.FragmentAdvanceTabBinding
import com.example.attendance.ui.adapter.AdvanceAdapter
import com.example.attendance.ui.viewmodel.AdvanceFilter
import com.example.attendance.ui.viewmodel.AdvanceViewModel
import com.example.attendance.ui.viewmodel.AdvanceViewModelFactory
import com.example.attendance.util.TimeUtils
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Calendar
import java.util.Locale

class AdvanceTabFragment : Fragment() {

    private var _binding: FragmentAdvanceTabBinding? = null
    private val binding get() = _binding!!

    private val advanceViewModel: AdvanceViewModel by viewModels {
        AdvanceViewModelFactory(
            AdvanceRepository(AppDatabase.getDatabase(requireContext()).advanceDao())
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdvanceTabBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val employeeId = arguments?.getInt(ARG_EMPLOYEE_ID) ?: return

        val adapter = AdvanceAdapter(
            onEditClick = { showEditAdvanceDialog(it) },
            onDeleteClick = { showDeleteAdvanceConfirmation(it) }
        )
        binding.rvAdvancesTab.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAdvancesTab.adapter = adapter

        // Observe filtered list
        advanceViewModel.getAdvancesForEmployee(employeeId).observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)

            val total = list.sumOf { it.amount }
            binding.tvTotalAdvance.text =
                String.format(Locale.getDefault(), "₹ %.2f", total)
            binding.tvAdvanceCount.text =
                "${list.size} record${if (list.size == 1) "" else "s"}"

            if (list.isEmpty()) {
                binding.rvAdvancesTab.visibility = View.GONE
                binding.layoutEmpty.visibility = View.VISIBLE
            } else {
                binding.rvAdvancesTab.visibility = View.VISIBLE
                binding.layoutEmpty.visibility = View.GONE
            }
        }

        // Filter chips
        binding.chipGroupAdvanceFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
            when (checkedIds.first()) {
                binding.chipAdvanceAll.id -> {
                    advanceViewModel.setFilter(AdvanceFilter.ALL)
                    binding.tvAdvanceFilterDesc.text = "Showing all records"
                }
                binding.chipAdvanceDaily.id -> {
                    advanceViewModel.setFilter(AdvanceFilter.DAILY)
                    binding.tvAdvanceFilterDesc.text = "Showing today's records"
                }
                binding.chipAdvanceMonthly.id -> {
                    advanceViewModel.setFilter(AdvanceFilter.MONTHLY)
                    binding.tvAdvanceFilterDesc.text = "Showing this month's records"
                }
                binding.chipAdvanceCustom.id -> {
                    showDateRangePicker()
                }
            }
        }

        binding.fabAddAdvance.setOnClickListener {
            showAddAdvanceDialog(employeeId)
        }
    }

    private fun showDateRangePicker() {
        val picker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText("Select date range")
            .setSelection(
                androidx.core.util.Pair(
                    MaterialDatePicker.todayInUtcMilliseconds(),
                    MaterialDatePicker.todayInUtcMilliseconds()
                )
            )
            .build()

        picker.addOnPositiveButtonClickListener { selection ->
            val start = selection.first
            val end = selection.second
            if (start != null && end != null) {
                // Set end to 23:59:59 so the full day is included
                val endOfDay = Calendar.getInstance().apply {
                    timeInMillis = end
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }.timeInMillis

                advanceViewModel.setCustomRange(start, endOfDay)
                binding.tvAdvanceFilterDesc.text =
                    "From ${TimeUtils.formatDate(start)} to ${TimeUtils.formatDate(end)}"
            }
        }
        // Revert chip to All if picker is cancelled
        picker.addOnNegativeButtonClickListener {
            binding.chipAdvanceAll.isChecked = true
        }
        picker.addOnCancelListener {
            binding.chipAdvanceAll.isChecked = true
        }

        picker.show(parentFragmentManager, "ADVANCE_DATE_PICKER")
    }

    private fun showAddAdvanceDialog(employeeId: Int) {
        val dialogBinding = DialogAddAdvanceBinding.inflate(layoutInflater)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add Advance Payment")
            .setView(dialogBinding.root)
            .setPositiveButton("Add") { _, _ ->
                val amountText = dialogBinding.etAmount.text.toString().trim()
                val description = dialogBinding.etDescription.text.toString().trim()
                when {
                    amountText.isEmpty() ->
                        Toast.makeText(requireContext(), "Enter an amount", Toast.LENGTH_SHORT).show()
                    description.isEmpty() ->
                        Toast.makeText(requireContext(), "Enter a description", Toast.LENGTH_SHORT).show()
                    else -> {
                        val amount = amountText.toDoubleOrNull()
                        if (amount == null || amount <= 0) {
                            Toast.makeText(requireContext(), "Enter a valid amount", Toast.LENGTH_SHORT).show()
                        } else {
                            // timestamp is auto-set to System.currentTimeMillis() in the entity
                            advanceViewModel.addAdvance(employeeId, amount, description)
                            Toast.makeText(requireContext(), "Advance recorded", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditAdvanceDialog(advance: Advance) {
        val dialogBinding = DialogAddAdvanceBinding.inflate(layoutInflater)
        dialogBinding.etAmount.setText(advance.amount.toString())
        dialogBinding.etDescription.setText(advance.description)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Edit Advance Payment")
            .setView(dialogBinding.root)
            .setPositiveButton("Update") { _, _ ->
                val amountText = dialogBinding.etAmount.text.toString().trim()
                val description = dialogBinding.etDescription.text.toString().trim()
                when {
                    amountText.isEmpty() ->
                        Toast.makeText(requireContext(), "Enter an amount", Toast.LENGTH_SHORT).show()
                    description.isEmpty() ->
                        Toast.makeText(requireContext(), "Enter a description", Toast.LENGTH_SHORT).show()
                    else -> {
                        val amount = amountText.toDoubleOrNull()
                        if (amount == null || amount <= 0) {
                            Toast.makeText(requireContext(), "Enter a valid amount", Toast.LENGTH_SHORT).show()
                        } else {
                            // Preserve the original timestamp on edit
                            advanceViewModel.updateAdvance(
                                advance.copy(amount = amount, description = description)
                            )
                            Toast.makeText(requireContext(), "Advance updated", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteAdvanceConfirmation(advance: Advance) {
        val dialogBinding = DialogPasswordBinding.inflate(layoutInflater)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Advance")
            .setMessage("Enter password to delete this advance record.")
            .setView(dialogBinding.root)
            .setPositiveButton("Delete") { _, _ ->
                if (dialogBinding.etPassword.text.toString() == "1234") {
                    advanceViewModel.deleteAdvance(advance)
                    Toast.makeText(requireContext(), "Advance deleted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Incorrect password", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_EMPLOYEE_ID = "employee_id"
        fun newInstance(employeeId: Int) = AdvanceTabFragment().apply {
            arguments = Bundle().apply { putInt(ARG_EMPLOYEE_ID, employeeId) }
        }
    }
}
