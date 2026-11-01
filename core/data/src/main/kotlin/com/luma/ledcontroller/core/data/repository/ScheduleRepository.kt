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

package com.luma.ledcontroller.core.data.repository

import com.luma.ledcontroller.core.model.LumaSchedule
import kotlinx.coroutines.flow.Flow

/** Repeating, per-strip changes to a mode. */
interface ScheduleRepository {
    val schedules: Flow<List<LumaSchedule>>

    fun observeSchedulesForStrip(stripId: String): Flow<List<LumaSchedule>>

    fun observeSchedule(scheduleId: String): Flow<LumaSchedule?>

    suspend fun getSchedule(scheduleId: String): LumaSchedule?

    suspend fun upsertSchedule(schedule: LumaSchedule)

    suspend fun setEnabled(scheduleId: String, enabled: Boolean)

    suspend fun deleteSchedule(scheduleId: String)

    /** Removes every schedule that targets a strip, used when the strip is deleted. */
    suspend fun deleteSchedulesForStrip(stripId: String)
}
