package com.example.appmind.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.appmind.data.entity.MonitoredApp
import kotlinx.coroutines.flow.Flow

@Dao
interface MonitoredAppDao {

    @Query("SELECT * FROM monitored_apps")
    fun getAllApps(): Flow<List<MonitoredApp>>

    @Query("SELECT * FROM monitored_apps WHERE is_monitored = 1")
    fun getMonitoredApps(): Flow<List<MonitoredApp>>

    @Query("SELECT * FROM monitored_apps WHERE is_monitored = 1")
    suspend fun getMonitoredAppsList(): List<MonitoredApp>

    @Query("SELECT * FROM monitored_apps WHERE package_name = :packageName")
    suspend fun getByPackageName(packageName: String): MonitoredApp?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApps(apps: List<MonitoredApp>)

    @Update
    suspend fun updateApp(app: MonitoredApp)

    @Query("UPDATE monitored_apps SET is_monitored = :monitored WHERE package_name = :packageName")
    suspend fun setMonitored(packageName: String, monitored: Boolean)

    @Query("UPDATE monitored_apps SET custom_question = :question WHERE package_name = :packageName")
    suspend fun setCustomQuestion(packageName: String, question: String?)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApp(app: MonitoredApp)
}
