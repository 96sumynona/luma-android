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

import com.luma.ledcontroller.core.data.repository.ScheduleRepository
import com.luma.ledcontroller.core.data.repository.StripRepository
import javax.inject.Inject

/** Deletes a strip and the schedules that pointed at it. */
class DeleteStripUseCase @Inject constructor(
    private val stripRepository: StripRepository,
    private val scheduleRepository: ScheduleRepository,
) {
    suspend operator fun invoke(stripId: String) {
        // Schedules are meaningless without their strip, so they go with it.
        scheduleRepository.deleteSchedulesForStrip(stripId)
        stripRepository.deleteStrip(stripId)
    }
}
