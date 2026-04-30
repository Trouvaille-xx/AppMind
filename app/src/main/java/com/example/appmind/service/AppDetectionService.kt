package com.example.appmind.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import com.example.appmind.data.entity.AppLog
import com.example.appmind.data.entity.MonitoredApp
import com.example.appmind.data.repository.AppRepository
import kotlinx.coroutines.*

class AppDetectionService : AccessibilityService() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val mainHandler = Handler(Looper.getMainLooper())
    private var overlayWindow: OverlayWindow? = null
    private var isShowing = false
    private var lastShownPackage: String? = null

    // Fast-path: in-memory cache of monitored packages
    private val monitoredCache = mutableMapOf<String, MonitoredApp>()

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        }
        refreshCache()
    }

    private fun refreshCache() {
        scope.launch {
            try {
                val repo = AppRepository(applicationContext)
                val apps = repo.getMonitoredAppList()
                monitoredCache.clear()
                apps.forEach { monitoredCache[it.packageName] = it }
            } catch (_: Exception) {}
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val eventPackage = event.packageName?.toString()?.takeIf { it.isNotEmpty() } ?: return
        if (eventPackage == packageName) return
        if (isShowing && eventPackage == lastShownPackage) return

        // Fast path: check in-memory cache synchronously
        val cached = monitoredCache[eventPackage]
        if (cached != null && cached.isMonitored) {
            val defaultQ = cached.customQuestion?.takeIf { it.isNotBlank() }
                ?: AppRepository(applicationContext).getDefaultQuestion(applicationContext)
            mainHandler.post {
                showOverlay(cached.appName, eventPackage, defaultQ)
            }
            return
        }

        // Slow path: DB fallback for any missed packages
        scope.launch {
            try {
                val repo = AppRepository(applicationContext)
                val app = repo.getByPackageName(eventPackage) ?: return@launch
                if (!app.isMonitored) return@launch
                monitoredCache[eventPackage] = app

                val defaultQ = repo.getDefaultQuestion(applicationContext)
                val q = app.customQuestion?.takeIf { it.isNotBlank() } ?: defaultQ

                withContext(Dispatchers.Main) {
                    if (isShowing) return@withContext
                    showOverlay(app.appName, eventPackage, q)
                }
            } catch (_: Exception) {}
        }
    }

    private fun showOverlay(appName: String, packageName: String, question: String) {
        if (isShowing) return

        isShowing = true
        lastShownPackage = packageName

        overlayWindow = OverlayWindow(
            context = applicationContext,
            appName = appName,
            question = question,
            onConfirm = { answer ->
                scope.launch {
                    try {
                        AppRepository(applicationContext).insertLog(
                            AppLog(
                                packageName = packageName,
                                appName = appName,
                                question = question,
                                answer = answer,
                                action = "confirmed"
                            )
                        )
                    } catch (_: Exception) {}
                }
                dismissAndReset()
            },
            onCancel = {
                scope.launch {
                    try {
                        AppRepository(applicationContext).insertLog(
                            AppLog(
                                packageName = packageName,
                                appName = appName,
                                question = question,
                                answer = "",
                                action = "cancelled"
                            )
                        )
                    } catch (_: Exception) {}
                }
                dismissAndReset()
                mainHandler.postDelayed({
                    try {
                        performGlobalAction(GLOBAL_ACTION_HOME)
                    } catch (_: Exception) {}
                }, 300)
            }
        )
        overlayWindow?.show()
    }

    fun notifyMonitoredChanged() {
        refreshCache()
    }

    private fun dismissAndReset() {
        overlayWindow?.dismiss()
        overlayWindow = null
        isShowing = false
        lastShownPackage = null
    }

    override fun onInterrupt() {
        dismissAndReset()
    }

    override fun onDestroy() {
        scope.cancel()
        dismissAndReset()
        super.onDestroy()
    }
}
