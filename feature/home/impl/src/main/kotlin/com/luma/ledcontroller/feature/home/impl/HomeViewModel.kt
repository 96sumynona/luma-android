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
import com.luma.ledcontroller.core.domain.GetBoardsUseCase
import com.luma.ledcontroller.core.domain.GetModesUseCase
import com.luma.ledcontroller.core.domain.GetNextScheduledChangeUseCase
import com.luma.ledcontroller.core.domain.GetSchedulesUseCase
import com.luma.ledcontroller.core.domain.GetStripsUseCase
import com.luma.ledcontroller.core.domain.SetStripBrightnessUseCase
import com.luma.ledcontroller.core.model.LumaBoard
import com.luma.ledcontroller.core.model.LumaMode
import com.luma.ledcontroller.core.model.LumaSchedule
import com.luma.ledcontroller.core.model.LumaStrip
import com.luma.ledcontroller.core.model.NextScheduledChange
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

/** One strip as Home shows it: its mode, its colour preview, its brightness and next change. */
data class StripCard(
    val strip: LumaStrip,
    val board: LumaBoard?,
    val mode: LumaMode?,
    val nextChange: NextScheduledChange?,
)

sealed interface HomeUiState {
    data object Loading : HomeUiState

    data class Success(
        val strips: List<StripCard>,
        val hasBoards: Boolean,
        val now: LocalDateTime,
    ) : HomeUiState
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    getStrips: GetStripsUseCase,
    getModes: GetModesUseCase,
    getBoards: GetBoardsUseCase,
    getSchedules: GetSchedulesUseCase,
    private val getNextScheduledChange: GetNextScheduledChangeUseCase,
    private val setStripBrightness: SetStripBrightnessUseCase,
    private val clock: Clock,
) : ViewModel() {

    /**
     * Brightness is written with a short debounce so dragging the slider does not hammer the
     * database, while the UI still follows the finger through [pendingBrightness].
     */
    private val brightnessUpdates = MutableSharedFlow<BrightnessChange>(extraBufferCapacity = 64)
    private val pendingBrightness = MutableStateFlow<Map<String, Float>>(emptyMap())

    private val configuration: Flow<Configuration> = combine(
        getStrips(),
        getModes(),
        getBoards(),
        getSchedules(),
    ) { strips, modes, boards, schedules ->
        Configuration(strips = strips, modes = modes, boards = boards, schedules = schedules)
    }

    private val nowFlow: Flow<LocalDateTime> = flow {
        while (true) {
            emit(clock.now().toLocalDateTime(TimeZone.currentSystemDefault()))
            kotlinx.coroutines.delay(TICK_MILLIS)
        }
    }

    val uiState: StateFlow<HomeUiState> = combine(
        configuration,
        nowFlow,
        pendingBrightness,
    ) { config, now, pending ->
        val nextChanges = getNextScheduledChange(config.schedules, now)
        HomeUiState.Success(
            strips = config.strips
                .sortedBy { it.name.lowercase() }
                .map { strip ->
                    StripCard(
                        strip = strip.copy(
                            brightness = pending[strip.id] ?: strip.brightness,
                        ),
                        board = config.boards.firstOrNull { it.id == strip.boardId },
                        mode = config.modes.firstOrNull { it.id == strip.modeId },
                        nextChange = nextChanges[strip.id],
                    )
                },
            hasBoards = config.boards.isNotEmpty(),
            now = now,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState.Loading,
    )

    init {
        viewModelScope.launch { persistBrightness() }
    }

    fun onBrightnessChanged(stripId: String, brightness: Float) {
        pendingBrightness.update(stripId, brightness)
        brightnessUpdates.tryEmit(BrightnessChange(stripId, brightness))
    }

    @OptIn(FlowPreview::class)
    private suspend fun persistBrightness() {
        brightnessUpdates
            .debounce(BRIGHTNESS_DEBOUNCE_MILLIS)
            .collect { change ->
                setStripBrightness(change.stripId, change.brightness)
            }
    }

    private fun MutableStateFlow<Map<String, Float>>.update(stripId: String, brightness: Float) {
        value = value.toMutableMap().apply { put(stripId, brightness) }
    }

    private data class Configuration(
        val strips: List<LumaStrip>,
        val modes: List<LumaMode>,
        val boards: List<LumaBoard>,
        val schedules: List<LumaSchedule>,
    )

    private data class BrightnessChange(val stripId: String, val brightness: Float)

    private companion object {
        const val BRIGHTNESS_DEBOUNCE_MILLIS = 220L
        const val TICK_MILLIS = 60_000L
    }
}
