package com.mauricekuehl.appblock.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import com.mauricekuehl.appblock.BlockedActivity
import com.mauricekuehl.appblock.data.ScheduleEvaluator
import com.mauricekuehl.appblock.data.ScheduleRepository

class AppBlockAccessibilityService : AccessibilityService() {
    private lateinit var scheduleRepository: ScheduleRepository
    private var lastBlockedPackage: String? = null
    private var lastBlockAt = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        isConnected = true
        scheduleRepository = ScheduleRepository(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType !in SUPPORTED_EVENT_TYPES) return

        val foregroundPackage = event.packageName?.toString() ?: return
        if (foregroundPackage == packageName) return

        if (!::scheduleRepository.isInitialized) {
            scheduleRepository = ScheduleRepository(this)
        }

        val schedule = scheduleRepository.load()
        if (foregroundPackage !in schedule.blockedPackages || !ScheduleEvaluator.isActive(schedule)) {
            return
        }

        val now = SystemClock.elapsedRealtime()
        if (foregroundPackage == lastBlockedPackage && now - lastBlockAt < BLOCK_DEBOUNCE_MS) return
        lastBlockedPackage = foregroundPackage
        lastBlockAt = now

        val blocker = Intent(this, BlockedActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(BlockedActivity.EXTRA_PACKAGE_NAME, foregroundPackage)
        }
        startActivity(blocker)
    }

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        isConnected = false
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        isConnected = false
        super.onDestroy()
    }

    companion object {
        @Volatile
        var isConnected: Boolean = false
            private set

        private val SUPPORTED_EVENT_TYPES = setOf(
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
        )

        const val BLOCK_DEBOUNCE_MS = 750L
    }
}
