package com.example.attendance.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Employee::class, Attendance::class, Advance::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun attendanceDao(): AttendanceDao
    abstract fun advanceDao(): AdvanceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Migration from version 1 to 2: adds the advances table
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `advances` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `employeeId` INTEGER NOT NULL,
                        `amount` REAL NOT NULL,
                        `description` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        FOREIGN KEY(`employeeId`) REFERENCES `employees`(`id`) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_advances_employeeId` ON `advances` (`employeeId`)"
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "attendance_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
