package com.mauricekuehl.appblock.data

import android.content.Context
import androidx.core.content.edit
import java.time.DayOfWeek

class ScheduleRepository(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun load(): BlockSchedule {
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
        preferences.edit {
            putBoolean(KEY_ENABLED, schedule.enabled)
            putInt(KEY_START_MINUTE, schedule.startMinute)
            putInt(KEY_END_MINUTE, schedule.endMinute)
            putStringSet(KEY_DAYS, schedule.days.map { it.value.toString() }.toSet())
            putStringSet(KEY_PACKAGES, schedule.blockedPackages)
        }
    }

    private companion object {
        const val PREFERENCES_NAME = "app_block_schedule"
        const val KEY_ENABLED = "enabled"
        const val KEY_START_MINUTE = "start_minute"
        const val KEY_END_MINUTE = "end_minute"
        const val KEY_DAYS = "days"
        const val KEY_PACKAGES = "packages"
    }
}
