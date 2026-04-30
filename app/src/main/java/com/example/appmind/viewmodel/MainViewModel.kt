package com.example.appmind.viewmodel

import android.app.Application
import android.content.Context
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.appmind.data.entity.AppLog
import com.example.appmind.data.entity.MonitoredApp
import com.example.appmind.data.repository.AppInfo
import com.example.appmind.data.repository.AppRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppRepository(application)

    // ========================
    // 服务状态
    // ========================

    val isAccessibilityEnabled: Boolean
        get() {
            val enabledServices = Settings.Secure.getString(
                getApplication<Application>().contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            return enabledServices.contains(getApplication<Application>().packageName)
        }

    val isOverlayPermissionGranted: Boolean
        get() = android.provider.Settings.canDrawOverlays(getApplication())

    // ========================
    // 默认问题
    // ========================

    private val _defaultQuestion = MutableStateFlow(
        repository.getDefaultQuestion(getApplication())
    )
    val defaultQuestion: StateFlow<String> = _defaultQuestion.asStateFlow()

    fun setDefaultQuestion(question: String) {
        _defaultQuestion.value = question
        repository.setDefaultQuestion(getApplication(), question)
    }

    // ========================
    // 已安装应用 & 监控状态
    // ========================

    private val _installedApps = MutableStateFlow<List<AppWithStatus>>(emptyList())
    val installedApps: StateFlow<List<AppWithStatus>> = _installedApps.asStateFlow()

    fun loadInstalledApps() {
        viewModelScope.launch {
            val apps = repository.getInstalledApps()
            val monitoredMap = mutableMapOf<String, MonitoredApp>()

            // Get all monitored apps from DB
            repository.getAllMonitoredApps().first().forEach {
                monitoredMap[it.packageName] = it
            }

            // First time? Sync apps to DB
            if (monitoredMap.isEmpty()) {
                repository.syncInstalledApps(apps)
                repository.getAllMonitoredApps().first().forEach {
                    monitoredMap[it.packageName] = it
                }
            }

            _installedApps.value = apps.map { app ->
                val monitored = monitoredMap[app.packageName]
                AppWithStatus(
                    appInfo = app,
                    isMonitored = monitored?.isMonitored ?: false,
                    customQuestion = monitored?.customQuestion
                )
            }
        }
    }

    fun toggleMonitor(packageName: String, monitored: Boolean) {
        viewModelScope.launch {
            repository.setMonitored(packageName, monitored)
            _installedApps.value = _installedApps.value.map {
                if (it.appInfo.packageName == packageName) it.copy(isMonitored = monitored)
                else it
            }
        }
    }

    fun setCustomQuestion(packageName: String, question: String?) {
        viewModelScope.launch {
            repository.setCustomQuestion(packageName, question)
            _installedApps.value = _installedApps.value.map {
                if (it.appInfo.packageName == packageName) it.copy(customQuestion = question)
                else it
            }
        }
    }

    // ========================
    // 日志
    // ========================

    private val _logs = MutableStateFlow<List<AppLog>>(emptyList())
    val logs: StateFlow<List<AppLog>> = _logs.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAllLogs().collect {
                _logs.value = it
            }
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            repository.clearAllLogs()
        }
    }
}

data class AppWithStatus(
    val appInfo: AppInfo,
    val isMonitored: Boolean,
    val customQuestion: String?
)
