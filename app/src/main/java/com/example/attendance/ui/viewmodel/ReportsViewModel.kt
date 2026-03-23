package com.example.attendance.ui.viewmodel

import androidx.lifecycle.*
import com.example.attendance.data.local.Attendance
import com.example.attendance.data.local.Employee
import com.example.attendance.data.repository.AttendanceRepository
import com.example.attendance.util.TimeUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.*

data class DailyAttendance(
    val date: String,
    val records: List<Attendance>,
    val totalDuration: String
)

data class EmployeeReport(
    val employee: Employee,
    val dailyAttendance: List<DailyAttendance>
)

enum class ReportFilter {
    ALL, DAILY, MONTHLY, CUSTOM
}

class ReportsViewModel(private val repository: AttendanceRepository) : ViewModel() {

    private val filter = MutableStateFlow(ReportFilter.ALL)
    private val startDate = MutableStateFlow<Long?>(null)
    private val endDate = MutableStateFlow<Long?>(null)

    fun setFilter(newFilter: ReportFilter) {
        filter.value = newFilter
    }

    fun setCustomRange(start: Long, end: Long) {
        startDate.value = start
        endDate.value = end
        filter.value = ReportFilter.CUSTOM
    }

    fun updateAttendance(attendance: Attendance) {
        viewModelScope.launch {
            repository.updateAttendance(attendance)
        }
    }

    fun deleteAttendance(attendance: Attendance) {
        viewModelScope.launch {
            repository.deleteAttendance(attendance)
        }
    }

    val employeeReports: LiveData<List<EmployeeReport>> = combine(
        repository.allEmployees,
        repository.allAttendance,
        filter,
        startDate,
        endDate
    ) { employees: List<Employee>, allAttendance: List<Attendance>, currentFilter: ReportFilter, start: Long?, end: Long? ->
        
        val filteredAttendance = when (currentFilter) {
            ReportFilter.ALL -> allAttendance
            ReportFilter.DAILY -> {
                val today = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                allAttendance.filter { it.checkInTime >= today }
            }
            ReportFilter.MONTHLY -> {
                val firstDayOfMonth = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                allAttendance.filter { it.checkInTime >= firstDayOfMonth }
            }
            ReportFilter.CUSTOM -> {
                if (start != null && end != null) {
                    allAttendance.filter { it.checkInTime in start..end }
                } else {
                    allAttendance
                }
            }
        }

        employees.map { emp ->
            val empAttendance = filteredAttendance.filter { it.employeeId == emp.id }
            val groupedByDate = empAttendance.groupBy { TimeUtils.formatDate(it.checkInTime) }
            
            val dailyReports = groupedByDate.map { (date, records) ->
                val totalMillis = records.sumOf { TimeUtils.calculateDurationMillis(it.checkInTime, it.checkOutTime) }
                val hours = totalMillis / (1000 * 60 * 60)
                val minutes = (totalMillis / (1000 * 60)) % 60
                DailyAttendance(
                    date = date,
                    records = records.sortedByDescending { it.checkInTime },
                    totalDuration = String.format(Locale.getDefault(), "%02d hrs %02d mins", hours, minutes)
                )
            }.sortedByDescending { it.records.firstOrNull()?.checkInTime ?: 0L }

            EmployeeReport(emp, dailyReports)
        }.filter { it.dailyAttendance.isNotEmpty() }
    }.asLiveData()
}

class ReportsViewModelFactory(private val repository: AttendanceRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReportsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ReportsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
