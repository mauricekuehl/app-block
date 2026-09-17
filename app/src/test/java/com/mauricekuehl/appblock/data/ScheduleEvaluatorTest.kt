package com.mauricekuehl.appblock.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDateTime

class ScheduleEvaluatorTest {
    private val weekdayMorning = BlockSchedule(
        enabled = true,
        startMinute = 6 * 60,
        endMinute = 9 * 60,
        days = setOf(DayOfWeek.MONDAY),
        blockedPackages = setOf("example.app"),
    )

    @Test
    fun activeInsideSameDayWindow() {
        assertTrue(ScheduleEvaluator.isActive(weekdayMorning, at(DayOfWeek.MONDAY, 7, 30)))
        assertFalse(ScheduleEvaluator.isActive(weekdayMorning, at(DayOfWeek.MONDAY, 9, 0)))
        assertFalse(ScheduleEvaluator.isActive(weekdayMorning, at(DayOfWeek.TUESDAY, 7, 30)))
    }

    @Test
    fun overnightWindowUsesTheDayOnWhichItStarts() {
        val overnight = weekdayMorning.copy(startMinute = 22 * 60, endMinute = 7 * 60)

        assertTrue(ScheduleEvaluator.isActive(overnight, at(DayOfWeek.MONDAY, 23, 0)))
        assertTrue(ScheduleEvaluator.isActive(overnight, at(DayOfWeek.TUESDAY, 2, 0)))
        assertFalse(ScheduleEvaluator.isActive(overnight, at(DayOfWeek.TUESDAY, 8, 0)))
    }

    @Test
    fun disabledOrEmptyScheduleIsNeverActive() {
        val monday = at(DayOfWeek.MONDAY, 7, 0)
        assertFalse(ScheduleEvaluator.isActive(weekdayMorning.copy(enabled = false), monday))
        assertFalse(ScheduleEvaluator.isActive(weekdayMorning.copy(blockedPackages = emptySet()), monday))
        assertFalse(ScheduleEvaluator.isActive(weekdayMorning.copy(days = emptySet()), monday))
    }

    private fun at(day: DayOfWeek, hour: Int, minute: Int): LocalDateTime {
        val monday = LocalDateTime.of(2026, 9, 14, hour, minute)
        return monday.plusDays((day.value - DayOfWeek.MONDAY.value).toLong())
    }
}
