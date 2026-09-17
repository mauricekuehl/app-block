package com.mauricekuehl.appblock.data

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Test

class ScheduleFileCodecTest {
    @Test
    fun roundTripsScheduleAcrossProcesses() {
        val expected = BlockSchedule(
            enabled = true,
            startMinute = 22 * 60,
            endMinute = 7 * 60,
            days = setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY),
            blockedPackages = setOf("org.mozilla.firefox", "com.fitbit.FitbitMobile"),
        )
        val bytes = ByteArrayOutputStream().also { ScheduleFileCodec.write(it, expected) }.toByteArray()

        val actual = ScheduleFileCodec.read(ByteArrayInputStream(bytes))

        assertEquals(expected, actual)
    }
}
