package com.example.appmind.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.appmind.data.dao.AppLogDao
import com.example.appmind.data.dao.MonitoredAppDao
import com.example.appmind.data.entity.AppLog
import com.example.appmind.data.entity.MonitoredApp

@Database(
    entities = [MonitoredApp::class, AppLog::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun monitoredAppDao(): MonitoredAppDao
    abstract fun appLogDao(): AppLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "appmind_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
