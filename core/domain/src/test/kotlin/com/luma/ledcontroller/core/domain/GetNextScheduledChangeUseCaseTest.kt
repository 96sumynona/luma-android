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

package com.luma.ledcontroller.core.domain

import com.luma.ledcontroller.core.model.LumaSchedule
import com.luma.ledcontroller.core.model.Weekday
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetNextScheduledChangeUseCaseTest {

    private val useCase = GetNextScheduledChangeUseCase()

    private fun schedule(id: String, stripId: String, hour: Int, enabled: Boolean = true) = LumaSchedule(
        id = id,
        name = id,
        stripId = stripId,
        modeId = "mode-1",
        hour = hour,
        minute = 0,
        repeatDays = setOf(Weekday.MONDAY),
        transitionSeconds = 0,
        enabled = enabled,
    )

    @Test
    fun everyStripGetsItsOwnNextChange() {
        val now = LocalDateTime.parse("2025-01-06T10:00")

        val result = useCase(
            schedules = listOf(
                schedule(id = "a", stripId = "strip-1", hour = 18),
                schedule(id = "b", stripId = "strip-2", hour = 12),
            ),
            now = now,
        )

        assertEquals("a", result["strip-1"]?.schedule?.id)
        assertEquals("b", result["strip-2"]?.schedule?.id)
        assertEquals("2025-01-06T12:00", result["strip-2"]?.nextRun?.toString())
    }

    @Test
    fun disabledSchedulesDoNotProduceAChange() {
        val now = LocalDateTime.parse("2025-01-06T10:00")

        val result = useCase(
            schedules = listOf(schedule(id = "a", stripId = "strip-1", hour = 18, enabled = false)),
            now = now,
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun stripsWithoutSchedulesAreAbsentRatherThanNull() {
        val result = useCase(schedules = emptyList(), now = LocalDateTime.parse("2025-01-06T10:00"))

        assertTrue(result.isEmpty())
    }
}
