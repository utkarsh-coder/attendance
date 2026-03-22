package com.example.attendance.data.repository

import com.example.attendance.data.local.Attendance
import com.example.attendance.data.local.AttendanceDao
import com.example.attendance.data.local.Employee
import kotlinx.coroutines.flow.Flow

class AttendanceRepository(private val attendanceDao: AttendanceDao) {

    val allEmployees: Flow<List<Employee>> = attendanceDao.getAllEmployees()

    suspend fun insertEmployee(employee: Employee) {
        attendanceDao.insertEmployee(employee)
    }

    suspend fun deleteEmployee(employee: Employee) {
        attendanceDao.deleteEmployee(employee)
    }

    suspend fun insertAttendance(attendance: Attendance) {
        attendanceDao.insertAttendance(attendance)
    }

    suspend fun updateAttendance(attendance: Attendance) {
        attendanceDao.updateAttendance(attendance)
    }

    suspend fun getLastAttendance(employeeId: Int): Attendance? {
        return attendanceDao.getLastAttendance(employeeId)
    }

    fun getAttendanceForEmployee(employeeId: Int): Flow<List<Attendance>> {
        return attendanceDao.getAttendanceForEmployee(employeeId)
    }

    val allAttendance: Flow<List<Attendance>> = attendanceDao.getAllAttendance()
}
