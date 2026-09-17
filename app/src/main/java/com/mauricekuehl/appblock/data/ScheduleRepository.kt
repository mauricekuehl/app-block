package com.mauricekuehl.appblock.data

import android.content.Context
import android.util.AtomicFile
import androidx.core.content.edit
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.time.DayOfWeek

class ScheduleRepository(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val scheduleFile = AtomicFile(File(context.noBackupFilesDir, SCHEDULE_FILE_NAME))

    fun load(): BlockSchedule {
        if (scheduleFile.baseFile.exists()) {
            runCatching {
                DataInputStream(BufferedInputStream(scheduleFile.openRead())).use(ScheduleFileCodec::read)
            }.getOrNull()?.let { return it }
        }

        return loadLegacyPreferences()
    }

    private fun loadLegacyPreferences(): BlockSchedule {
        val defaultDays = BlockSchedule().days.map { it.value.toString() }.toSet()
        val storedDays = preferences.getStringSet(KEY_DAYS, defaultDays).orEmpty()

        return BlockSchedule(
            enabled = preferences.getBoolean(KEY_ENABLED, true),
            startMinute = preferences.getInt(KEY_START_MINUTE, 6 * 60),
            endMinute = preferences.getInt(KEY_END_MINUTE, 9 * 60),
            days = storedDays.mapNotNull { value ->
                value.toIntOrNull()?.let { runCatching { DayOfWeek.of(it) }.getOrNull() }
            }.toSet(),
            blockedPackages = preferences.getStringSet(KEY_PACKAGES, emptySet()).orEmpty().toSet(),
        )
    }

    fun save(schedule: BlockSchedule) {
        writeScheduleFile(schedule)
        preferences.edit {
            putBoolean(KEY_ENABLED, schedule.enabled)
            putInt(KEY_START_MINUTE, schedule.startMinute)
            putInt(KEY_END_MINUTE, schedule.endMinute)
            putStringSet(KEY_DAYS, schedule.days.map { it.value.toString() }.toSet())
            putStringSet(KEY_PACKAGES, schedule.blockedPackages)
        }
    }

    private fun writeScheduleFile(schedule: BlockSchedule) {
        var stream = scheduleFile.startWrite()
        try {
            val output = DataOutputStream(BufferedOutputStream(stream))
            ScheduleFileCodec.write(output, schedule)
            output.flush()
            scheduleFile.finishWrite(stream)
        } catch (error: Throwable) {
            scheduleFile.failWrite(stream)
            throw error
        }
    }

    private companion object {
        const val PREFERENCES_NAME = "app_block_schedule"
        const val SCHEDULE_FILE_NAME = "app_block_schedule.bin"
        const val KEY_ENABLED = "enabled"
        const val KEY_START_MINUTE = "start_minute"
        const val KEY_END_MINUTE = "end_minute"
        const val KEY_DAYS = "days"
        const val KEY_PACKAGES = "packages"
    }
}

internal object ScheduleFileCodec {
    private const val MAGIC = 0x4150424C
    private const val VERSION = 1
    private const val MAX_PACKAGE_COUNT = 10_000

    fun write(output: OutputStream, schedule: BlockSchedule) {
        val data = output as? DataOutputStream ?: DataOutputStream(output)
        data.writeInt(MAGIC)
        data.writeInt(VERSION)
        data.writeBoolean(schedule.enabled)
        data.writeInt(schedule.startMinute)
        data.writeInt(schedule.endMinute)
        data.writeInt(schedule.days.size)
        schedule.days.sortedBy(DayOfWeek::getValue).forEach { data.writeInt(it.value) }
        data.writeInt(schedule.blockedPackages.size)
        schedule.blockedPackages.sorted().forEach(data::writeUTF)
    }

    fun read(input: InputStream): BlockSchedule {
        val data = input as? DataInputStream ?: DataInputStream(input)
        require(data.readInt() == MAGIC) { "Invalid schedule file" }
        require(data.readInt() == VERSION) { "Unsupported schedule file version" }

        val enabled = data.readBoolean()
        val startMinute = data.readInt().also { require(it in 0 until 24 * 60) }
        val endMinute = data.readInt().also { require(it in 0 until 24 * 60) }
        val dayCount = data.readInt().also { require(it in 0..7) }
        val days = buildSet {
            repeat(dayCount) { add(DayOfWeek.of(data.readInt())) }
        }
        val packageCount = data.readInt().also { require(it in 0..MAX_PACKAGE_COUNT) }
        val packages = buildSet {
            repeat(packageCount) { add(data.readUTF()) }
        }

        return BlockSchedule(enabled, startMinute, endMinute, days, packages)
    }
}
