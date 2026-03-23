package com.example.attendance.ui.viewmodel

import androidx.lifecycle.*
import com.example.attendance.data.local.Advance
import com.example.attendance.data.repository.AdvanceRepository
import kotlinx.coroutines.launch

class AdvanceViewModel(private val repository: AdvanceRepository) : ViewModel() {

    fun getAdvancesForEmployee(employeeId: Int): LiveData<List<Advance>> =
        repository.getAdvancesForEmployee(employeeId).asLiveData()

    fun addAdvance(employeeId: Int, amount: Double, description: String) = viewModelScope.launch {
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

class AdvanceViewModelFactory(private val repository: AdvanceRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AdvanceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AdvanceViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
