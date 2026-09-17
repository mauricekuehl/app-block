package com.mauricekuehl.appblock.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.mauricekuehl.appblock.BlockedActivity
import com.mauricekuehl.appblock.data.InstalledAppsRepository
import com.mauricekuehl.appblock.data.ScheduleEvaluator
import com.mauricekuehl.appblock.data.ScheduleRepository

class AppBlockAccessibilityService : AccessibilityService() {
    private lateinit var scheduleRepository: ScheduleRepository
    private lateinit var installedAppsRepository: InstalledAppsRepository
    private lateinit var blockingOverlay: BlockingOverlay
    private lateinit var serviceHealth: AccessibilityServiceHealth
    private val heartbeatHandler = Handler(Looper.getMainLooper())
    private val heartbeat = object : Runnable {
        override fun run() {
            if (::serviceHealth.isInitialized) {
                runCatching { serviceHealth.markAlive() }
                heartbeatHandler.postDelayed(this, HEARTBEAT_INTERVAL_MS)
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        initializeDependencies()
        heartbeatHandler.removeCallbacks(heartbeat)
        heartbeat.run()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        try {
            handleAccessibilityEvent(event)
        } catch (error: RuntimeException) {
            Log.e(TAG, "Ignoring accessibility event failure", error)
        }
    }

    private fun handleAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType !in SUPPORTED_EVENT_TYPES) return

        val foregroundPackage = event.packageName?.toString() ?: return
        Log.d(TAG, "event=${event.eventType} package=$foregroundPackage")
        if (foregroundPackage == packageName) return

        if (!::scheduleRepository.isInitialized) {
            initializeDependencies()
        }

        val schedule = scheduleRepository.load()
        Log.d(TAG, "blocked=${foregroundPackage in schedule.blockedPackages} active=${ScheduleEvaluator.isActive(schedule)}")
        if (foregroundPackage !in schedule.blockedPackages || !ScheduleEvaluator.isActive(schedule)) {
            if (
                event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
                foregroundPackage != SYSTEM_UI_PACKAGE
            ) {
                blockingOverlay.hide()
            }
            return
        }

        blockingOverlay.show(
            packageName = foregroundPackage,
            appLabel = installedAppsRepository.labelFor(foregroundPackage),
            endMinute = schedule.endMinute,
            onOverlayFailure = { launchFallbackBlocker(foregroundPackage) },
        )
    }

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        stopHeartbeat()
        if (::blockingOverlay.isInitialized) blockingOverlay.hide()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        stopHeartbeat()
        if (::blockingOverlay.isInitialized) blockingOverlay.hide()
        super.onDestroy()
    }

    private fun initializeDependencies() {
        scheduleRepository = ScheduleRepository(this)
        installedAppsRepository = InstalledAppsRepository(this)
        blockingOverlay = BlockingOverlay(this)
        serviceHealth = AccessibilityServiceHealth(this)
    }

    private fun stopHeartbeat() {
        heartbeatHandler.removeCallbacks(heartbeat)
        if (::serviceHealth.isInitialized) {
            runCatching { serviceHealth.markStopped() }
        }
    }

    private fun launchFallbackBlocker(foregroundPackage: String) {
        val blocker = Intent(this, BlockedActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(BlockedActivity.EXTRA_PACKAGE_NAME, foregroundPackage)
        }
        runCatching { startActivity(blocker) }
            .onFailure { Log.e(TAG, "Unable to show fallback blocker", it) }
    }

    companion object {
        private val SUPPORTED_EVENT_TYPES = setOf(
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            AccessibilityEvent.TYPE_VIEW_FOCUSED,
        )

        private const val SYSTEM_UI_PACKAGE = "com.android.systemui"
        private const val TAG = "AppBlockService"
        private const val HEARTBEAT_INTERVAL_MS = 5_000L
    }
}
