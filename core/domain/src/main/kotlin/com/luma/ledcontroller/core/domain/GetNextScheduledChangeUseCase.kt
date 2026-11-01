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
import com.luma.ledcontroller.core.model.NextScheduledChange
import com.luma.ledcontroller.core.model.ScheduleOccurrence
import kotlinx.datetime.LocalDateTime
import javax.inject.Inject

/**
 * Works out the next change for each strip, so Home can show "next scheduled change" without the
 * UI re-implementing schedule maths.
 */
class GetNextScheduledChangeUseCase @Inject constructor() {
    operator fun invoke(
        schedules: List<LumaSchedule>,
        now: LocalDateTime,
    ): Map<String, NextScheduledChange> =
        schedules
            .filter { it.enabled }
            .groupBy { it.stripId }
            .mapValues { (_, stripSchedules) -> ScheduleOccurrence.nextChange(stripSchedules, now) }
            .filterValues { it != null }
            .mapValues { (_, change) -> change!! }
}
