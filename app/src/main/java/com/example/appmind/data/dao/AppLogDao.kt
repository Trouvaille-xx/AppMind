package com.example.appmind.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.appmind.data.entity.AppLog
import kotlinx.coroutines.flow.Flow

@Dao
interface AppLogDao {

    @Query("SELECT * FROM app_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<AppLog>>

    @Insert
    suspend fun insertLog(log: AppLog)

    @Query("DELETE FROM app_logs")
    suspend fun clearAll()
}
