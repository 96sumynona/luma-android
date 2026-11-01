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

package com.luma.ledcontroller.feature.schedules.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luma.ledcontroller.core.data.repository.ModeRepository
import com.luma.ledcontroller.core.data.repository.StripRepository
import com.luma.ledcontroller.core.domain.DeleteScheduleUseCase
import com.luma.ledcontroller.core.domain.GetSchedulesUseCase
import com.luma.ledcontroller.core.domain.SetScheduleEnabledUseCase
import com.luma.ledcontroller.core.model.LumaMode
import com.luma.ledcontroller.core.model.LumaSchedule
import com.luma.ledcontroller.core.model.LumaStrip
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** A schedule with the names it refers to resolved for display. */
data class ScheduleSummary(
    val schedule: LumaSchedule,
    val stripName: String?,
    val modeName: String?,
)

data class SchedulesUiState(
    val schedules: List<ScheduleSummary> = emptyList(),
    val hasStrips: Boolean = false,
    val hasModes: Boolean = false,
    val isLoading: Boolean = true,
)

@HiltViewModel
class SchedulesViewModel @Inject constructor(
    getSchedules: GetSchedulesUseCase,
    stripRepository: StripRepository,
    modeRepository: ModeRepository,
    private val setScheduleEnabled: SetScheduleEnabledUseCase,
    private val deleteSchedule: DeleteScheduleUseCase,
) : ViewModel() {

    val uiState: StateFlow<SchedulesUiState> = combine(
        getSchedules(),
        stripRepository.strips,
        modeRepository.modes,
    ) { schedules, strips, modes ->
        SchedulesUiState(
            schedules = schedules
                .sortedWith(compareBy({ it.hour }, { it.minute }, { it.name }))
                .map { schedule -> schedule.toSummary(strips, modes) },
            hasStrips = strips.isNotEmpty(),
            hasModes = modes.isNotEmpty(),
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SchedulesUiState(),
    )

    fun setEnabled(scheduleId: String, enabled: Boolean) {
        viewModelScope.launch { setScheduleEnabled(scheduleId = scheduleId, enabled = enabled) }
    }

    fun deleteSchedule(scheduleId: String) {
        viewModelScope.launch { deleteSchedule.invoke(scheduleId) }
    }

    private fun LumaSchedule.toSummary(
        strips: List<LumaStrip>,
        modes: List<LumaMode>,
    ): ScheduleSummary = ScheduleSummary(
        schedule = this,
        stripName = strips.firstOrNull { it.id == stripId }?.name,
        modeName = modes.firstOrNull { it.id == modeId }?.name,
    )
}
