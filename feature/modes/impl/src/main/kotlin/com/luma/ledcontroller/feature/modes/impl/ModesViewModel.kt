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

package com.luma.ledcontroller.feature.modes.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luma.ledcontroller.core.data.repository.StripRepository
import com.luma.ledcontroller.core.domain.DeleteModeUseCase
import com.luma.ledcontroller.core.domain.GetModesUseCase
import com.luma.ledcontroller.core.domain.SaveModeUseCase
import com.luma.ledcontroller.core.domain.savedValue
import com.luma.ledcontroller.core.model.LumaMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** A mode plus how many strips currently point at it. */
data class ModeSummary(
    val mode: LumaMode,
    val stripCount: Int,
)

data class ModesUiState(
    val modes: List<ModeSummary> = emptyList(),
    val isLoading: Boolean = true,
)

/** One-shot messages for the library screen, kept as events so rotation does not repeat them. */
sealed interface ModesEvent {
    data class Duplicated(val name: String) : ModesEvent
}

@HiltViewModel
class ModesViewModel @Inject constructor(
    getModes: GetModesUseCase,
    stripRepository: StripRepository,
    private val saveMode: SaveModeUseCase,
    private val deleteMode: DeleteModeUseCase,
) : ViewModel() {

    private val events = Channel<ModesEvent>(Channel.BUFFERED)
    val eventFlow = events.receiveAsFlow()

    val uiState: StateFlow<ModesUiState> = combine(
        getModes(),
        stripRepository.strips,
    ) { modes, strips ->
        ModesUiState(
            modes = modes
                .sortedBy { it.name.lowercase() }
                .map { mode ->
                    ModeSummary(mode = mode, stripCount = strips.count { it.modeId == mode.id })
                },
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ModesUiState(),
    )

    fun deleteMode(modeId: String) {
        viewModelScope.launch { deleteMode(modeId) }
    }

    /**
     * Copies a mode into a new, independent mode. The copy keeps the source's ramp and animation but
     * gets its own id, so editing it never touches the original.
     */
    fun duplicateMode(mode: LumaMode) {
        viewModelScope.launch {
            val result = saveMode(
                modeId = null,
                name = mode.name + DUPLICATE_SUFFIX,
                ramp = mode.ramp,
                animation = mode.animation,
            )
            result.savedValue()?.let { saved -> events.send(ModesEvent.Duplicated(saved.name)) }
        }
    }

    private companion object {
        const val DUPLICATE_SUFFIX = " copy"
    }
}
