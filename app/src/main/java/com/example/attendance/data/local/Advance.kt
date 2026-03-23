package com.example.attendance.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "advances",
    foreignKeys = [
        ForeignKey(
            entity = Employee::class,
            parentColumns = ["id"],
            childColumns = ["employeeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["employeeId"])]
)
data class Advance(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val employeeId: Int,
    val amount: Double,
    val description: String,
    val timestamp: Long = System.currentTimeMillis()
)
