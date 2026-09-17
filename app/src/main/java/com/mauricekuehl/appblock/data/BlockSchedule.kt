package com.mauricekuehl.appblock.data

import java.time.DayOfWeek
import java.time.LocalDateTime

data class BlockSchedule(
    val enabled: Boolean = true,
    val startMinute: Int = 6 * 60,
    val endMinute: Int = 9 * 60,
    val days: Set<DayOfWeek> = setOf(
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY,
    ),
    val blockedPackages: Set<String> = emptySet(),
)

object ScheduleEvaluator {
    fun isActive(schedule: BlockSchedule, now: LocalDateTime = LocalDateTime.now()): Boolean {
        if (!schedule.enabled || schedule.blockedPackages.isEmpty() || schedule.days.isEmpty()) {
            return false
        }

        val minute = now.hour * 60 + now.minute
        return when {
            schedule.startMinute == schedule.endMinute -> now.dayOfWeek in schedule.days
            schedule.startMinute < schedule.endMinute ->
                now.dayOfWeek in schedule.days && minute in schedule.startMinute until schedule.endMinute
            minute >= schedule.startMinute -> now.dayOfWeek in schedule.days
            minute < schedule.endMinute -> now.dayOfWeek.minus(1) in schedule.days
            else -> false
        }
    }
}
