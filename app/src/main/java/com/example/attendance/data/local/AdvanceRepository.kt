package com.example.attendance.data.repository

import com.example.attendance.data.local.Advance
import com.example.attendance.data.local.AdvanceDao
import kotlinx.coroutines.flow.Flow

class AdvanceRepository(private val advanceDao: AdvanceDao) {

    fun getAdvancesForEmployee(employeeId: Int): Flow<List<Advance>> =
        advanceDao.getAdvancesForEmployee(employeeId)

    val allAdvances: Flow<List<Advance>> = advanceDao.getAllAdvances()

    suspend fun insertAdvance(advance: Advance) = advanceDao.insertAdvance(advance)

    suspend fun updateAdvance(advance: Advance) = advanceDao.updateAdvance(advance)

    suspend fun deleteAdvance(advance: Advance) = advanceDao.deleteAdvance(advance)

    suspend fun getTotalAdvanceForEmployee(employeeId: Int): Double =
        advanceDao.getTotalAdvanceForEmployee(employeeId) ?: 0.0
}
