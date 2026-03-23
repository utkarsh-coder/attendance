package com.example.attendance.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AdvanceDao {
    @Insert
    suspend fun insertAdvance(advance: Advance)

    @Update
    suspend fun updateAdvance(advance: Advance)

    @Delete
    suspend fun deleteAdvance(advance: Advance)

    @Query("SELECT * FROM advances WHERE employeeId = :employeeId ORDER BY timestamp DESC")
    fun getAdvancesForEmployee(employeeId: Int): Flow<List<Advance>>

    @Query("SELECT * FROM advances ORDER BY timestamp DESC")
    fun getAllAdvances(): Flow<List<Advance>>

    @Query("SELECT SUM(amount) FROM advances WHERE employeeId = :employeeId")
    suspend fun getTotalAdvanceForEmployee(employeeId: Int): Double?
}
