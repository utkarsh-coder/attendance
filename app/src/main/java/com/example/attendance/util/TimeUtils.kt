package com.example.attendance.util

import java.text.SimpleDateFormat
import java.util.*

object TimeUtils {
    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun calculateDuration(startTime: Long, endTime: Long?): String {
        if (endTime == null) return "Check-out pending"
        val diff = endTime - startTime
        val hours = diff / (1000 * 60 * 60)
        val minutes = (diff / (1000 * 60)) % 60
        return String.format("%02d hrs %02d mins", hours, minutes)
    }

    fun calculateDurationMillis(startTime: Long, endTime: Long?): Long {
        if (endTime == null) return 0
        return endTime - startTime
    }
}
