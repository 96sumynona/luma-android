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

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.plus
import kotlinx.serialization.Serializable

/** Day of week, ordered Monday first so it can be packed into a bit mask. */
@Serializable
enum class Weekday {
    MONDAY,
    TUESDAY,
    WEDNESDAY,
    THURSDAY,
    FRIDAY,
    SATURDAY,
    SUNDAY,
    ;

    /** Bit used by the database and by the eventual controller payload. */
    val bit: Int
        get() = 1 shl ordinal

    companion object {
        val Weekdays: List<Weekday> = entries

        fun fromBitMask(mask: Int): Set<Weekday> = entries.filter { mask and it.bit != 0 }.toSet()

        fun toBitMask(days: Set<Weekday>): Int = days.fold(0) { mask, day -> mask or day.bit }
    }
}

/**
 * A repeating change that moves a strip to a mode at a time of day.
 *
 * Schedules reference a strip and a mode instead of embedding their configuration, so a single mode
 * can be scheduled on several strips and edited in one place.
 *
 * @param hour local hour, 0..23.
 * @param minute local minute, 0..59.
 * @param repeatDays the days this schedule fires. An empty set never fires.
 * @param transitionSeconds how long the change should take. `0` is an immediate switch.
 * @param enabled allows the user to keep a schedule without it firing.
 */
@Serializable
data class LumaSchedule(
    val id: String,
    val name: String,
    val stripId: String,
    val modeId: String,
    val hour: Int,
    val minute: Int,
    val repeatDays: Set<Weekday>,
    val transitionSeconds: Int = 0,
    val enabled: Boolean = true,
) {
    val isValidTime: Boolean
        get() = hour in 0..23 && minute in 0..59

    /** `18:30` style label. */
    val timeLabel: String
        get() = "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
}

/** A schedule together with the next moment it fires. */
data class NextScheduledChange(
    val schedule: LumaSchedule,
    val nextRun: LocalDateTime,
)

/**
 * Works out when a schedule fires next.
 *
 * The app only needs this to describe schedules in the UI; the eventual controller runs the
 * repeating schedules itself, independently of the app.
 */
object ScheduleOccurrence {

    /** How many days ahead to look before giving up. A week is always enough for a repeat. */
    private const val SEARCH_DAYS = 8

    /**
     * The next time this schedule fires strictly after [now], or `null` when it repeats no days.
     */
    fun nextOccurrence(schedule: LumaSchedule, now: LocalDateTime): LocalDateTime? {
        if (schedule.repeatDays.isEmpty() || !schedule.isValidTime) return null
        for (dayOffset in 0 until SEARCH_DAYS) {
            val date = now.date.plus(dayOffset, DateTimeUnit.DAY)
            if (date.dayOfWeek.toWeekday() !in schedule.repeatDays) continue
            val candidate = LocalDateTime(
                date = LocalDate(
                    year = date.year,
                    monthNumber = date.monthNumber,
                    dayOfMonth = date.dayOfMonth,
                ),
                time = LocalTime(hour = schedule.hour, minute = schedule.minute),
            )
            if (candidate > now) return candidate
        }
        return null
    }

    /** The next change across all schedules, ignoring disabled schedules. */
    fun nextChange(schedules: List<LumaSchedule>, now: LocalDateTime): NextScheduledChange? =
        schedules
            .asSequence()
            .filter { it.enabled }
            .mapNotNull { schedule -> nextOccurrence(schedule, now)?.let { NextScheduledChange(schedule, it) } }
            .minByOrNull { it.nextRun }
}

private fun kotlinx.datetime.DayOfWeek.toWeekday(): Weekday = when (this) {
    kotlinx.datetime.DayOfWeek.MONDAY -> Weekday.MONDAY
    kotlinx.datetime.DayOfWeek.TUESDAY -> Weekday.TUESDAY
    kotlinx.datetime.DayOfWeek.WEDNESDAY -> Weekday.WEDNESDAY
    kotlinx.datetime.DayOfWeek.THURSDAY -> Weekday.THURSDAY
    kotlinx.datetime.DayOfWeek.FRIDAY -> Weekday.FRIDAY
    kotlinx.datetime.DayOfWeek.SATURDAY -> Weekday.SATURDAY
    kotlinx.datetime.DayOfWeek.SUNDAY -> Weekday.SUNDAY
    else -> Weekday.MONDAY
}
