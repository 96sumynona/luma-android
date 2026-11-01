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

package com.luma.ledcontroller.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.luma.ledcontroller.core.model.Weekday
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.plus

/** Short, locale-stable day names. The app ships English only for this UI-shell phase. */
private val ShortDayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

/**
 * Describes the next scheduled change as "Today · 18:30" or "Thu · 07:00".
 */
@Composable
fun formatNextScheduledChange(next: LocalDateTime, now: LocalDateTime): String {
    val dayLabel = when {
        next.date == now.date -> stringResource(R.string.core_ui_today)
        next.date == now.date.plus(1, DateTimeUnit.DAY) -> stringResource(R.string.core_ui_tomorrow)
        else -> ShortDayNames.getOrElse(next.dayOfWeek.ordinal) { "" }
    }
    val time = "${next.hour.toString().padStart(2, '0')}:${next.minute.toString().padStart(2, '0')}"
    return "$dayLabel · $time"
}

/** Collapses a repeat-day set into a short label. */
@Composable
fun formatRepeatDays(days: Set<Weekday>): String = when {
    days.isEmpty() -> stringResource(R.string.core_ui_no_repeat_days)
    days.size == Weekday.entries.size -> stringResource(R.string.core_ui_every_day)
    days == Weekday.entries.take(5).toSet() -> stringResource(R.string.core_ui_weekdays)
    days == Weekday.entries.drop(5).toSet() -> stringResource(R.string.core_ui_weekends)
    else -> days.sortedBy { it.ordinal }.joinToString(", ") {
        ShortDayNames.getOrElse(it.ordinal) { "" }
    }
}

/** Short label for a single day, used by the repeat-day chips. */
fun Weekday.shortLabel(): String = ShortDayNames.getOrElse(ordinal) { "" }

/** A transition is shown as a duration, or as "Immediate" when it is zero. */
@Composable
fun formatTransition(seconds: Int): String = when {
    seconds <= 0 -> stringResource(R.string.core_ui_transition_off)
    seconds < 60 -> "$seconds s"
    else -> {
        val minutes = seconds / 60
        val remainder = seconds % 60
        if (remainder == 0) "$minutes min" else "$minutes min $remainder s"
    }
}
