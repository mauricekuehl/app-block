package com.mauricekuehl.appblock.service

import android.app.ActivityManager
import android.content.Context
import android.util.AtomicFile
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File

internal class AccessibilityServiceHealth(private val context: Context) {
    private val heartbeatFile = AtomicFile(File(context.noBackupFilesDir, HEARTBEAT_FILE_NAME))

    fun markAlive(nowMillis: Long = System.currentTimeMillis()) {
        writeTimestamp(nowMillis)
    }

    fun markStopped() {
        writeTimestamp(0L)
    }

    fun isRecentlyAlive(nowMillis: Long = System.currentTimeMillis()): Boolean {
        if (!isAccessibilityProcessRunning()) return false
        val heartbeat = runCatching {
            DataInputStream(heartbeatFile.openRead()).use(DataInputStream::readLong)
        }.getOrDefault(0L)
        return heartbeat in (nowMillis - HEARTBEAT_MAX_AGE_MS)..nowMillis
    }

    private fun writeTimestamp(timestamp: Long) {
        var stream = heartbeatFile.startWrite()
        try {
            DataOutputStream(stream).writeLong(timestamp)
            heartbeatFile.finishWrite(stream)
        } catch (error: Throwable) {
            heartbeatFile.failWrite(stream)
            throw error
        }
    }

    private fun isAccessibilityProcessRunning(): Boolean {
        val activityManager = context.getSystemService(ActivityManager::class.java)
        val expectedProcess = "${context.packageName}:accessibility"
        return activityManager.runningAppProcesses.orEmpty().any { it.processName == expectedProcess }
    }

    private companion object {
        const val HEARTBEAT_FILE_NAME = "accessibility_heartbeat.bin"
        const val HEARTBEAT_MAX_AGE_MS = 30_000L
    }
}
