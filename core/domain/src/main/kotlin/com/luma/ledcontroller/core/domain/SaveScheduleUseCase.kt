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

import com.luma.ledcontroller.core.data.IdGenerator
import com.luma.ledcontroller.core.data.repository.ScheduleRepository
import com.luma.ledcontroller.core.model.LumaError
import com.luma.ledcontroller.core.model.LumaLimits
import com.luma.ledcontroller.core.model.LumaSchedule
import com.luma.ledcontroller.core.model.LumaValidation
import com.luma.ledcontroller.core.model.Weekday
import javax.inject.Inject

/** Creates or updates a schedule. */
class SaveScheduleUseCase @Inject constructor(
    private val scheduleRepository: ScheduleRepository,
    private val idGenerator: IdGenerator,
) {
    suspend operator fun invoke(
        scheduleId: String?,
        name: String,
        stripId: String?,
        modeId: String?,
        hour: Int,
        minute: Int,
        repeatDays: Set<Weekday>,
        transitionSeconds: Int,
    ): SaveResult<LumaSchedule> {
        val errors = LumaValidation.validateSchedule(
            stripId = stripId,
            modeId = modeId,
            hour = hour,
            minute = minute,
            repeatDays = repeatDays,
            transitionSeconds = transitionSeconds,
        )
        if (errors.isNotEmpty()) return SaveResult.Invalid(errors)

        val existing = scheduleId?.let { scheduleRepository.getSchedule(it) }
        val schedule = LumaSchedule(
            id = existing?.id ?: idGenerator.newId(),
            name = name.trim(),
            stripId = requireNotNull(stripId),
            modeId = requireNotNull(modeId),
            hour = hour,
            minute = minute,
            repeatDays = repeatDays,
            transitionSeconds = transitionSeconds.coerceIn(
                LumaLimits.MIN_TRANSITION_SECONDS,
                LumaLimits.MAX_TRANSITION_SECONDS,
            ),
            enabled = existing?.enabled ?: true,
        )
        scheduleRepository.upsertSchedule(schedule)
        return SaveResult.Saved(schedule)
    }
}

/** Deletes a schedule. */
class DeleteScheduleUseCase @Inject constructor(
    private val scheduleRepository: ScheduleRepository,
) {
    suspend operator fun invoke(scheduleId: String) {
        scheduleRepository.deleteSchedule(scheduleId)
    }
}

/** Enables or disables a schedule without editing it. */
class SetScheduleEnabledUseCase @Inject constructor(
    private val scheduleRepository: ScheduleRepository,
) {
    suspend operator fun invoke(scheduleId: String, enabled: Boolean) {
        scheduleRepository.setEnabled(scheduleId, enabled)
    }
}

/** Shared validation for the schedule editor. */
class ValidateScheduleUseCase @Inject constructor() {
    operator fun invoke(
        stripId: String?,
        modeId: String?,
        hour: Int,
        minute: Int,
        repeatDays: Set<Weekday>,
        transitionSeconds: Int,
    ): Set<LumaError> = LumaValidation.validateSchedule(
        stripId = stripId,
        modeId = modeId,
        hour = hour,
        minute = minute,
        repeatDays = repeatDays,
        transitionSeconds = transitionSeconds,
    )
}
