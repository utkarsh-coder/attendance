package com.example.attendance.ui.viewmodel

import androidx.lifecycle.*
import com.example.attendance.data.local.Advance
import com.example.attendance.data.repository.AdvanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Calendar

enum class AdvanceFilter { ALL, DAILY, MONTHLY, CUSTOM }

class AdvanceViewModel(private val repository: AdvanceRepository) : ViewModel() {

    private val filter = MutableStateFlow(AdvanceFilter.ALL)
    private val customStart = MutableStateFlow<Long?>(null)
    private val customEnd = MutableStateFlow<Long?>(null)

    fun setFilter(newFilter: AdvanceFilter) {
        filter.value = newFilter
    }

    fun setCustomRange(start: Long, end: Long) {
        customStart.value = start
        customEnd.value = end
        filter.value = AdvanceFilter.CUSTOM
    }

    /** Returns a LiveData of filtered advances for a specific employee. */
    fun getAdvancesForEmployee(employeeId: Int): LiveData<List<Advance>> =
        combine(
            repository.getAdvancesForEmployee(employeeId),
            filter,
            customStart,
            customEnd
        ) { advances, currentFilter, start, end ->
            when (currentFilter) {
                AdvanceFilter.ALL -> advances

                AdvanceFilter.DAILY -> {
                    val startOfDay = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    advances.filter { it.timestamp >= startOfDay }
                }

                AdvanceFilter.MONTHLY -> {
                    val startOfMonth = Calendar.getInstance().apply {
                        set(Calendar.DAY_OF_MONTH, 1)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    advances.filter { it.timestamp >= startOfMonth }
                }

                AdvanceFilter.CUSTOM -> {
                    if (start != null && end != null) {
                        advances.filter { it.timestamp in start..end }
                    } else {
                        advances
                    }
                }
            }
        }.asLiveData()

    fun addAdvance(employeeId: Int, amount: Double, description: String) = viewModelScope.launch {
        // timestamp defaults to System.currentTimeMillis() in the Advance entity
        repository.insertAdvance(
            Advance(
                employeeId = employeeId,
                amount = amount,
                description = description
            )
        )
    }

    fun updateAdvance(advance: Advance) = viewModelScope.launch {
        repository.updateAdvance(advance)
    }

    fun deleteAdvance(advance: Advance) = viewModelScope.launch {
        repository.deleteAdvance(advance)
    }
}

class AdvanceViewModelFactory(private val repository: AdvanceRepository) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AdvanceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AdvanceViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
