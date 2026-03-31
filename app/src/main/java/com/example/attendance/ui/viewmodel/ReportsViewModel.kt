package com.example.attendance.ui.viewmodel

import androidx.lifecycle.*
import com.example.attendance.data.local.Advance
import com.example.attendance.data.local.Attendance
import com.example.attendance.data.local.Employee
import com.example.attendance.data.repository.AdvanceRepository
import com.example.attendance.data.repository.AttendanceRepository
import com.example.attendance.util.TimeUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.*

data class DailyAttendance(
    val date: String,
    val records: List<Attendance>,
    val totalDuration: String,
    val totalMillis: Long
)

data class EmployeeReport(
    val employee: Employee,
    val dailyAttendance: List<DailyAttendance>,
    val totalWorkingMillis: Long,
    val totalAdvance: Double
)

enum class ReportFilter {
    ALL, DAILY, MONTHLY, CUSTOM
}

class ReportsViewModel(
    private val repository: AttendanceRepository,
    private val advanceRepository: AdvanceRepository
) : ViewModel() {

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
        advanceRepository.allAdvances,
        filter,
        startDate,
        endDate
    ) { args: Array<Any?> ->
        val employees = args[0] as List<Employee>
        val allAttendance = args[1] as List<Attendance>
        val allAdvances = args[2] as List<Advance>
        val currentFilter = args[3] as ReportFilter
        val start = args[4] as Long?
        val end = args[5] as Long?
        
        val dateRange: LongRange? = when (currentFilter) {
            ReportFilter.ALL -> null
            ReportFilter.DAILY -> {
                val startOfDay = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                startOfDay..Long.MAX_VALUE
            }
            ReportFilter.MONTHLY -> {
                val firstDayOfMonth = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                firstDayOfMonth..Long.MAX_VALUE
            }
            ReportFilter.CUSTOM -> {
                if (start != null && end != null) {
                    start..end
                } else {
                    null
                }
            }
        }

        val filteredAttendance = if (dateRange != null) {
            allAttendance.filter { it.checkInTime in dateRange }
        } else {
            allAttendance
        }

        val filteredAdvances = if (dateRange != null) {
            allAdvances.filter { it.timestamp in dateRange }
        } else {
            allAdvances
        }

        employees.map { emp ->
            val empAttendance = filteredAttendance.filter { it.employeeId == emp.id }
            val empAdvances = filteredAdvances.filter { it.employeeId == emp.id }
            
            val groupedByDate = empAttendance.groupBy { TimeUtils.formatDate(it.checkInTime) }
            
            var totalWorkingMillis = 0L
            val dailyReports = groupedByDate.map { (date, records) ->
                val dayMillis = records.sumOf { TimeUtils.calculateDurationMillis(it.checkInTime, it.checkOutTime) }
                totalWorkingMillis += dayMillis
                val hours = dayMillis / (1000 * 60 * 60)
                val minutes = (dayMillis / (1000 * 60)) % 60
                DailyAttendance(
                    date = date,
                    records = records.sortedByDescending { it.checkInTime },
                    totalDuration = String.format(Locale.getDefault(), "%02d hrs %02d mins", hours, minutes),
                    totalMillis = dayMillis
                )
            }.sortedByDescending { it.records.firstOrNull()?.checkInTime ?: 0L }

            EmployeeReport(
                employee = emp,
                dailyAttendance = dailyReports,
                totalWorkingMillis = totalWorkingMillis,
                totalAdvance = empAdvances.sumOf { it.amount }
            )
        }.filter { it.dailyAttendance.isNotEmpty() || it.totalAdvance > 0 }
    }.asLiveData()
}

class ReportsViewModelFactory(
    private val repository: AttendanceRepository,
    private val advanceRepository: AdvanceRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReportsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ReportsViewModel(repository, advanceRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
