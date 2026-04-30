package com.example.appmind.data.repository

import android.content.Context
import android.content.pm.PackageManager
import com.example.appmind.data.AppDatabase
import com.example.appmind.data.entity.AppLog
import com.example.appmind.data.entity.MonitoredApp
import kotlinx.coroutines.flow.Flow

class AppRepository(context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val monitoredAppDao = db.monitoredAppDao()
    private val appLogDao = db.appLogDao()
    private val packageManager = context.packageManager

    // ========================
    // 已安装应用
    // ========================

    fun getInstalledApps(): List<AppInfo> {
        return packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { it.packageName != context.packageName }
            .filter { packageManager.getLaunchIntentForPackage(it.packageName) != null }
            .map {
                AppInfo(
                    packageName = it.packageName,
                    appName = packageManager.getApplicationLabel(it).toString(),
                    icon = packageManager.getApplicationIcon(it)
                )
            }
            .sortedBy { it.appName.lowercase() }
    }

    // ========================
    // 监控应用管理
    // ========================

    fun getAllMonitoredApps(): Flow<List<MonitoredApp>> = monitoredAppDao.getAllApps()

    fun getMonitoredApps(): Flow<List<MonitoredApp>> = monitoredAppDao.getMonitoredApps()

    suspend fun getByPackageName(packageName: String): MonitoredApp? =
        monitoredAppDao.getByPackageName(packageName)

    suspend fun syncInstalledApps(apps: List<AppInfo>) {
        val monitoredList = apps.map { app ->
            MonitoredApp(
                packageName = app.packageName,
                appName = app.appName,
                isMonitored = false,
                customQuestion = null
            )
        }
        monitoredAppDao.insertApps(monitoredList)
    }

    suspend fun setMonitored(packageName: String, monitored: Boolean) {
        monitoredAppDao.setMonitored(packageName, monitored)
    }

    suspend fun setCustomQuestion(packageName: String, question: String?) {
        monitoredAppDao.setCustomQuestion(packageName, question)
    }

    suspend fun getQuestionForPackage(packageName: String, defaultQuestion: String): String {
        val app = monitoredAppDao.getByPackageName(packageName)
        return app?.customQuestion?.takeIf { it.isNotBlank() } ?: defaultQuestion
    }

    // ========================
    // 日志管理
    // ========================

    fun getAllLogs(): Flow<List<AppLog>> = appLogDao.getAllLogs()

    suspend fun insertLog(log: AppLog) {
        appLogDao.insertLog(log)
    }

    suspend fun clearAllLogs() {
        appLogDao.clearAll()
    }

    // ========================
    // 设置（简单 SharedPreferences）
    // ========================

    fun getDefaultQuestion(context: Context): String {
        val prefs = context.getSharedPreferences("appmind_prefs", Context.MODE_PRIVATE)
        return prefs.getString("default_question", "你为什么想打开这个应用？") ?: "你为什么想打开这个应用？"
    }

    fun setDefaultQuestion(context: Context, question: String) {
        val prefs = context.getSharedPreferences("appmind_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("default_question", question).apply()
    }
}

data class AppInfo(
    val packageName: String,
    val appName: String,
    val icon: android.graphics.drawable.Drawable
)
