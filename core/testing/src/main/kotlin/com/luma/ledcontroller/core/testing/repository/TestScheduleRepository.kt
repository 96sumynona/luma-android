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

package com.luma.ledcontroller.core.testing.repository

import com.luma.ledcontroller.core.data.repository.ScheduleRepository
import com.luma.ledcontroller.core.model.LumaSchedule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory [ScheduleRepository] for tests. */
class TestScheduleRepository : ScheduleRepository {
    private val store = MutableStateFlow<List<LumaSchedule>>(emptyList())

    override val schedules: Flow<List<LumaSchedule>> = store

    override fun observeSchedulesForStrip(stripId: String): Flow<List<LumaSchedule>> =
        store.map { schedules -> schedules.filter { it.stripId == stripId } }

    override fun observeSchedule(scheduleId: String): Flow<LumaSchedule?> =
        store.map { schedules -> schedules.firstOrNull { it.id == scheduleId } }

    override suspend fun getSchedule(scheduleId: String): LumaSchedule? =
        store.value.firstOrNull { it.id == scheduleId }

    override suspend fun upsertSchedule(schedule: LumaSchedule) {
        store.value = store.value.filterNot { it.id == schedule.id } + schedule
    }

    override suspend fun setEnabled(scheduleId: String, enabled: Boolean) {
        store.value = store.value.map { if (it.id == scheduleId) it.copy(enabled = enabled) else it }
    }

    override suspend fun deleteSchedule(scheduleId: String) {
        store.value = store.value.filterNot { it.id == scheduleId }
    }

    override suspend fun deleteSchedulesForStrip(stripId: String) {
        store.value = store.value.filterNot { it.stripId == stripId }
    }
}
