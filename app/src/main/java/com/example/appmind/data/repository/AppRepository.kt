package com.example.appmind.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.example.appmind.data.AppDatabase
import com.example.appmind.data.entity.AppLog
import com.example.appmind.data.entity.MonitoredApp
import kotlinx.coroutines.flow.Flow

class AppRepository(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val monitoredAppDao = db.monitoredAppDao()
    private val appLogDao = db.appLogDao()
    private val packageManager = context.packageManager

    // ========================
    // 已安装应用
    // ========================

    fun getInstalledApps(): List<AppInfo> {
        val apps = getInstalledAppsByQuery() + getInstalledAppsByPackageManager()
        return apps
            .distinctBy { it.packageName }
            .sortedBy { it.appName.lowercase() }
    }

    private fun getInstalledAppsByQuery(): List<AppInfo> {
        val mainIntent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
        return try {
            val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                @Suppress("InlinedApi")
                packageManager.queryIntentActivities(
                    mainIntent,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.queryIntentActivities(mainIntent, PackageManager.MATCH_ALL)
            }
            resolveInfos
                .filter { it.activityInfo.packageName != context.packageName }
                .map { info ->
                    AppInfo(
                        packageName = info.activityInfo.packageName,
                        appName = info.loadLabel(packageManager).toString(),
                        icon = info.loadIcon(packageManager)
                    )
                }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun getInstalledAppsByPackageManager(): List<AppInfo> {
        val flags = PackageManager.GET_META_DATA or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) PackageManager.MATCH_ALL else 0
        return try {
            packageManager.getInstalledApplications(flags)
                .filter { it.packageName != context.packageName }
                .map {
                    AppInfo(
                        packageName = it.packageName,
                        appName = packageManager.getApplicationLabel(it).toString(),
                        icon = packageManager.getApplicationIcon(it)
                    )
                }
        } catch (_: Exception) {
            emptyList()
        }
    }

    // ========================
    // 监控应用管理
    // ========================

    fun getAllMonitoredApps(): Flow<List<MonitoredApp>> = monitoredAppDao.getAllApps()

    fun getMonitoredApps(): Flow<List<MonitoredApp>> = monitoredAppDao.getMonitoredApps()

    suspend fun getMonitoredAppList(): List<MonitoredApp> = monitoredAppDao.getMonitoredAppsList()

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
