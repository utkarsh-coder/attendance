package com.example.attendance.ui.viewmodel

import androidx.lifecycle.*
import com.example.attendance.data.local.Employee
import com.example.attendance.data.repository.AttendanceRepository
import kotlinx.coroutines.launch

class MainViewModel(private val repository: AttendanceRepository) : ViewModel() {

    val allEmployees: LiveData<List<Employee>> = repository.allEmployees.asLiveData()

    fun addEmployee(name: String) = viewModelScope.launch {
        repository.insertEmployee(Employee(name = name))
    }

    fun deleteEmployee(employee: Employee) = viewModelScope.launch {
        repository.deleteEmployee(employee)
    }
}

class MainViewModelFactory(private val repository: AttendanceRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
