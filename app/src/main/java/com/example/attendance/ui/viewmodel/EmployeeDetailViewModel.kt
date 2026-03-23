package com.example.attendance.ui.viewmodel

import androidx.lifecycle.*
import com.example.attendance.data.local.Attendance
import com.example.attendance.data.repository.AttendanceRepository
import kotlinx.coroutines.launch

class EmployeeDetailViewModel(private val repository: AttendanceRepository) : ViewModel() {

    fun checkIn(employeeId: Int, time: Long = System.currentTimeMillis()) = viewModelScope.launch {
        val lastAttendance = repository.getLastAttendance(employeeId)
        if (lastAttendance == null || lastAttendance.checkOutTime != null) {
            val attendance = Attendance(
                employeeId = employeeId,
                checkInTime = time
            )
            repository.insertAttendance(attendance)
        }
    }

    fun checkOut(employeeId: Int, time: Long = System.currentTimeMillis()) = viewModelScope.launch {
        val lastAttendance = repository.getLastAttendance(employeeId)
        if (lastAttendance != null && lastAttendance.checkOutTime == null) {
            lastAttendance.checkOutTime = time
            repository.updateAttendance(lastAttendance)
        }
    }

    fun updateAttendance(attendance: Attendance) = viewModelScope.launch {
        repository.updateAttendance(attendance)
    }

    fun deleteAttendance(attendance: Attendance) = viewModelScope.launch {
        repository.deleteAttendance(attendance)
    }

    fun getAttendanceFlow(employeeId: Int) = repository.getAttendanceForEmployee(employeeId).asLiveData()
}

class EmployeeDetailViewModelFactory(private val repository: AttendanceRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EmployeeDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EmployeeDetailViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
