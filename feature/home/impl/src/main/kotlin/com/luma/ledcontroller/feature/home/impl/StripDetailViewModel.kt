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

package com.luma.ledcontroller.feature.home.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luma.ledcontroller.core.data.repository.BoardRepository
import com.luma.ledcontroller.core.data.repository.ModeRepository
import com.luma.ledcontroller.core.data.repository.ScheduleRepository
import com.luma.ledcontroller.core.data.repository.StripRepository
import com.luma.ledcontroller.core.domain.AssignModeToStripUseCase
import com.luma.ledcontroller.core.domain.SetStripBrightnessUseCase
import com.luma.ledcontroller.core.model.LumaBoard
import com.luma.ledcontroller.core.model.LumaMode
import com.luma.ledcontroller.core.model.LumaSchedule
import com.luma.ledcontroller.core.model.LumaStrip
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface StripDetailUiState {
    data object Loading : StripDetailUiState

    data object NotFound : StripDetailUiState

    data class Success(
        val strip: LumaStrip,
        val board: LumaBoard?,
        val mode: LumaMode?,
        val modes: List<LumaMode>,
        val schedules: List<LumaSchedule>,
    ) : StripDetailUiState
}

@HiltViewModel
class StripDetailViewModel @Inject constructor(
    private val stripRepository: StripRepository,
    private val boardRepository: BoardRepository,
    private val modeRepository: ModeRepository,
    private val scheduleRepository: ScheduleRepository,
    private val assignModeToStrip: AssignModeToStripUseCase,
    private val setStripBrightness: SetStripBrightnessUseCase,
) : ViewModel() {

    private val stripId = MutableStateFlow<String?>(null)
    private val pendingBrightness = MutableStateFlow<Float?>(null)
    private val brightnessUpdates = MutableSharedFlow<Float>(extraBufferCapacity = 32)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<StripDetailUiState> = stripId
        .filterNotNull()
        .flatMapLatest { id ->
            combine(
                stripRepository.observeStrip(id),
                boardRepository.boards,
                modeRepository.modes,
                scheduleRepository.observeSchedulesForStrip(id),
                pendingBrightness,
            ) { strip, boards, modes, schedules, pending ->
                if (strip == null) {
                    StripDetailUiState.NotFound
                } else {
                    StripDetailUiState.Success(
                        strip = strip.copy(brightness = pending ?: strip.brightness),
                        board = boards.firstOrNull { it.id == strip.boardId },
                        mode = modes.firstOrNull { it.id == strip.modeId },
                        modes = modes,
                        schedules = schedules,
                    )
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = StripDetailUiState.Loading,
        )

    init {
        viewModelScope.launch { persistBrightness() }
    }

    /** Called once with the navigation argument, and again after process death. */
    fun load(stripId: String) {
        this.stripId.value = stripId
    }

    fun onBrightnessChanged(brightness: Float) {
        pendingBrightness.value = brightness
        brightnessUpdates.tryEmit(brightness)
    }

    fun onModeSelected(modeId: String?) {
        val id = stripId.value ?: return
        viewModelScope.launch { assignModeToStrip(stripId = id, modeId = modeId) }
    }

    @OptIn(FlowPreview::class)
    private suspend fun persistBrightness() {
        brightnessUpdates.debounce(220L).collect { brightness ->
            val id = stripId.value ?: return@collect
            setStripBrightness(stripId = id, brightness = brightness)
        }
    }
}
