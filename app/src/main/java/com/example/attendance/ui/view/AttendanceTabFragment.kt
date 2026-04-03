package com.example.attendance.ui.view

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.attendance.data.local.AppDatabase
import com.example.attendance.data.local.Attendance
import com.example.attendance.data.repository.AttendanceRepository
import com.example.attendance.databinding.DialogEditAttendanceBinding
import com.example.attendance.databinding.DialogPasswordBinding
import com.example.attendance.databinding.FragmentAttendanceTabBinding
import com.example.attendance.ui.adapter.AttendanceAdapter
import com.example.attendance.ui.viewmodel.EmployeeDetailViewModel
import com.example.attendance.ui.viewmodel.EmployeeDetailViewModelFactory
import com.example.attendance.util.TimeUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Calendar

class AttendanceTabFragment : Fragment() {

    private var _binding: FragmentAttendanceTabBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EmployeeDetailViewModel by activityViewModels {
        EmployeeDetailViewModelFactory(
            AttendanceRepository(AppDatabase.getDatabase(requireContext()).attendanceDao())
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAttendanceTabBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val employeeId = arguments?.getInt(ARG_EMPLOYEE_ID) ?: return

        val adapter = AttendanceAdapter(
            onEditClick = { showEditDialog(it) },
            onDeleteClick = { showDeleteConfirmation(it) }
        )
        binding.rvAttendanceTab.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAttendanceTab.adapter = adapter

        viewModel.getAttendanceFlow(employeeId).observe(viewLifecycleOwner) {
            adapter.submitList(it)
        }
    }

    private fun showEditDialog(attendance: Attendance) {
        val dialogBinding = DialogEditAttendanceBinding.inflate(layoutInflater)
        
        // We use the original check-in time as the anchor for the date
        val dateAnchor = attendance.checkInTime
        
        var selectedCheckIn = attendance.checkInTime
        var selectedCheckOut = attendance.checkOutTime

        dialogBinding.btnEditCheckIn.text = "In: ${TimeUtils.formatTime(selectedCheckIn)}"
        dialogBinding.btnEditCheckOut.text = "Out: ${selectedCheckOut?.let { TimeUtils.formatTime(it) } ?: "--"}"

        dialogBinding.btnEditCheckIn.setOnClickListener {
            showTimePicker(dateAnchor, selectedCheckIn) { cal ->
                selectedCheckIn = cal.timeInMillis
                dialogBinding.btnEditCheckIn.text = "In: ${TimeUtils.formatTime(selectedCheckIn)}"
            }
        }
        dialogBinding.btnEditCheckOut.setOnClickListener {
            // Force the checkout to be on the same date as the check-in anchor
            showTimePicker(dateAnchor, selectedCheckOut ?: selectedCheckIn) { cal ->
                selectedCheckOut = cal.timeInMillis
                dialogBinding.btnEditCheckOut.text = "Out: ${TimeUtils.formatTime(selectedCheckOut!!)}"
            }
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Edit Attendance")
            .setView(dialogBinding.root)
            .setPositiveButton("Update") { _, _ ->
                if (selectedCheckOut != null && selectedCheckOut!! < selectedCheckIn) {
                    Toast.makeText(requireContext(), "Check-out cannot be before check-in", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.updateAttendance(attendance.copy(checkInTime = selectedCheckIn, checkOutTime = selectedCheckOut))
                    Toast.makeText(requireContext(), "Updated successfully", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteConfirmation(attendance: Attendance) {
        val dialogBinding = DialogPasswordBinding.inflate(layoutInflater)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Confirm Delete")
            .setMessage("Enter password to delete this record.")
            .setView(dialogBinding.root)
            .setPositiveButton("Delete") { _, _ ->
                if (dialogBinding.etPassword.text.toString() == "1234") {
                    viewModel.deleteAttendance(attendance)
                    Toast.makeText(requireContext(), "Record deleted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Incorrect password", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showTimePicker(dateAnchor: Long, initialTime: Long, onSelected: (Calendar) -> Unit) {
        val cal = Calendar.getInstance().apply { timeInMillis = initialTime }
        TimePickerDialog(requireContext(), { _, h, m ->
            val result = Calendar.getInstance().apply {
                // Pin to the date provided by dateAnchor
                timeInMillis = dateAnchor
                set(Calendar.HOUR_OF_DAY, h)
                set(Calendar.MINUTE, m)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            onSelected(result)
        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_EMPLOYEE_ID = "employee_id"
        fun newInstance(employeeId: Int) = AttendanceTabFragment().apply {
            arguments = Bundle().apply { putInt(ARG_EMPLOYEE_ID, employeeId) }
        }
    }
}
