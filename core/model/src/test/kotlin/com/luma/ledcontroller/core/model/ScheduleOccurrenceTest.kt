/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.luma.ledcontroller.core.model

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ScheduleOccurrenceTest {

    // 2025-01-06 is a Monday.
    private val mondayMorning = LocalDateTime.parse("2025-01-06T10:00")

    private fun schedule(
        id: String = "schedule",
        hour: Int = 18,
        minute: Int = 0,
        days: Set<Weekday> = setOf(Weekday.MONDAY),
        enabled: Boolean = true,
    ) = LumaSchedule(
        id = id,
        name = "Evening",
        stripId = "strip-1",
        modeId = "mode-1",
        hour = hour,
        minute = minute,
        repeatDays = days,
        transitionSeconds = 30,
        enabled = enabled,
    )

    @Test
    fun fixtureIsAMonday() {
        assertEquals(DayOfWeek.MONDAY, mondayMorning.date.dayOfWeek)
    }

    @Test
    fun nextOccurrenceLaterToday() {
        val next = ScheduleOccurrence.nextOccurrence(schedule(), mondayMorning)

        assertEquals("2025-01-06T18:00", next.toString())
    }

    @Test
    fun nextOccurrenceRollsOverToNextWeek() {
        val now = LocalDateTime.parse("2025-01-06T19:30")

        val next = ScheduleOccurrence.nextOccurrence(schedule(), now)

        assertEquals("2025-01-13T18:00", next.toString())
    }

    @Test
    fun aScheduleWithoutRepeatDaysNeverFires() {
        assertNull(ScheduleOccurrence.nextOccurrence(schedule(days = emptySet()), mondayMorning))
    }

    @Test
    fun earliestChangeWins() {
        val early = schedule(id = "early", hour = 12, days = setOf(Weekday.MONDAY))
        val late = schedule(id = "late", hour = 20, days = setOf(Weekday.MONDAY))

        val next = ScheduleOccurrence.nextChange(listOf(late, early), mondayMorning)

        assertEquals("early", next?.schedule?.id)
        assertEquals("2025-01-06T12:00", next?.nextRun?.toString())
    }

    @Test
    fun disabledSchedulesAreIgnored() {
        val disabled = schedule(id = "disabled", hour = 12, enabled = false)
        val enabled = schedule(id = "enabled", hour = 20)

        val next = ScheduleOccurrence.nextChange(listOf(disabled, enabled), mondayMorning)

        assertEquals("enabled", next?.schedule?.id)
    }

    @Test
    fun weekdayBitMaskRoundTrips() {
        val days = setOf(Weekday.MONDAY, Weekday.FRIDAY, Weekday.SUNDAY)

        assertEquals(days, Weekday.fromBitMask(Weekday.toBitMask(days)))
        assertEquals(0, Weekday.toBitMask(emptySet()))
    }
}
