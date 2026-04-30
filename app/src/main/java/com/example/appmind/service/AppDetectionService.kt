package com.example.appmind.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.example.appmind.data.entity.AppLog
import com.example.appmind.data.repository.AppRepository
import kotlinx.coroutines.*

class AppDetectionService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var overlayWindow: OverlayWindow? = null
    private var currentPackage: String? = null
    private var isOverlayShowing = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        }
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return
        if (packageName == this@AppDetectionService.packageName) return // ignore own app

        // Avoid processing same package repeatedly while overlay is showing
        if (packageName == currentPackage && isOverlayShowing) return

        serviceScope.launch {
            checkAndShowOverlay(packageName)
        }
    }

    private suspend fun checkAndShowOverlay(packageName: String) {
        val repository = AppRepository(applicationContext)
        val monitoredApp = repository.getByPackageName(packageName) ?: return

        if (!monitoredApp.isMonitored) return
        if (isOverlayShowing) return

        val defaultQuestion = repository.getDefaultQuestion(applicationContext)
        val question = monitoredApp.customQuestion?.takeIf { it.isNotBlank() } ?: defaultQuestion

        currentPackage = packageName
        isOverlayShowing = true

        withContext(Dispatchers.Main) {
            showOverlay(monitoredApp.appName, packageName, question)
        }
    }

    private fun showOverlay(appName: String, packageName: String, question: String) {
        overlayWindow = OverlayWindow(
            context = this,
            appName = appName,
            question = question,
            onConfirm = { answer ->
                serviceScope.launch {
                    val repository = AppRepository(applicationContext)
                    repository.insertLog(
                        AppLog(
                            packageName = packageName,
                            appName = appName,
                            question = question,
                            answer = answer,
                            action = "confirmed"
                        )
                    )
                }
                dismissOverlay()
            },
            onCancel = {
                serviceScope.launch {
                    val repository = AppRepository(applicationContext)
                    repository.insertLog(
                        AppLog(
                            packageName = packageName,
                            appName = appName,
                            question = question,
                            answer = "",
                            action = "cancelled"
                        )
                    )
                }
                dismissOverlay()
                performGlobalAction(GLOBAL_ACTION_BACK)
                serviceScope.launch {
                    delay(200)
                    withContext(Dispatchers.Main) {
                        performGlobalAction(GLOBAL_ACTION_HOME)
                    }
                }
            }
        )
        overlayWindow?.show()
    }

    private fun dismissOverlay() {
        overlayWindow?.dismiss()
        overlayWindow = null
        currentPackage = null
        isOverlayShowing = false
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        serviceScope.cancel()
        overlayWindow?.dismiss()
        super.onDestroy()
    }
}
