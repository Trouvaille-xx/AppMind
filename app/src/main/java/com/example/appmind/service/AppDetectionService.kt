package com.example.appmind.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import com.example.appmind.data.entity.AppLog
import com.example.appmind.data.repository.AppRepository
import kotlinx.coroutines.*

class AppDetectionService : AccessibilityService() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val mainHandler = Handler(Looper.getMainLooper())
    private var overlayWindow: OverlayWindow? = null
    private var isShowing = false
    private var lastShownPackage: String? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val eventPackage = event.packageName?.toString()?.takeIf { it.isNotEmpty() } ?: return

        // Ignore events from our own app
        if (eventPackage == packageName) return

        // Debounce: don't fire for same package while overlay is showing
        if (isShowing && eventPackage == lastShownPackage) return

        // Check on IO thread (DB query), then show on Main thread
        scope.launch {
            try {
                val repository = AppRepository(applicationContext)
                val app = repository.getByPackageName(eventPackage) ?: return@launch
                if (!app.isMonitored) return@launch

                val defaultQ = repository.getDefaultQuestion(applicationContext)
                val q = app.customQuestion?.takeIf { it.isNotBlank() } ?: defaultQ

                // Switch to main thread for UI
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
            context = this,
            appName = appName,
            question = question,
            onConfirm = { answer ->
                scope.launch {
                    try {
                        val repo = AppRepository(applicationContext)
                        repo.insertLog(
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
                        val repo = AppRepository(applicationContext)
                        repo.insertLog(
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
                // Go home after cancel (with delay to let dismiss complete)
                mainHandler.postDelayed({
                    try {
                        performGlobalAction(GLOBAL_ACTION_HOME)
                    } catch (_: Exception) {}
                }, 300)
            }
        )
        overlayWindow?.show()
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
