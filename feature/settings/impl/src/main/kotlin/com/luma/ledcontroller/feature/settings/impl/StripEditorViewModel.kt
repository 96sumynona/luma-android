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

package com.luma.ledcontroller.feature.settings.impl

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luma.ledcontroller.core.data.repository.BoardRepository
import com.luma.ledcontroller.core.data.repository.ModeRepository
import com.luma.ledcontroller.core.data.repository.StripRepository
import com.luma.ledcontroller.core.domain.DeleteStripUseCase
import com.luma.ledcontroller.core.domain.SaveStripUseCase
import com.luma.ledcontroller.core.domain.errors
import com.luma.ledcontroller.core.domain.savedValue
import com.luma.ledcontroller.core.model.LumaBoard
import com.luma.ledcontroller.core.model.LumaError
import com.luma.ledcontroller.core.model.LumaLimits
import com.luma.ledcontroller.core.model.LumaMode
import com.luma.ledcontroller.core.model.LumaStrip
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject

@Serializable
data class StripEditorDraft(
    val stripId: String? = null,
    val isNew: Boolean = true,
    val boardId: String,
    val name: String = "",
    val gpio: String = "5",
    val activeLeds: String = LumaLimits.DEFAULT_ACTIVE_LEDS.toString(),
    val brightness: Float = LumaLimits.DEFAULT_BRIGHTNESS,
    val modeId: String? = null,
    val isDirty: Boolean = false,
)

data class StripEditorUiState(
    val draft: StripEditorDraft,
    val boards: List<LumaBoard> = emptyList(),
    val modes: List<LumaMode> = emptyList(),
    val errors: Set<LumaError> = emptySet(),
    val isSaving: Boolean = false,
    val isLoading: Boolean = true,
    val showLeaveDialog: Boolean = false,
    val isGone: Boolean = false,
    val savedMessage: String? = null,
) {
    val board: LumaBoard?
        get() = boards.firstOrNull { it.id == draft.boardId }

    val maxLeds: Int
        get() = minOf(board?.effectivePixelLimit ?: LumaLimits.MAX_LEDS, LumaLimits.MAX_LEDS)
}

@HiltViewModel
class StripEditorViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val stripRepository: StripRepository,
    private val boardRepository: BoardRepository,
    modeRepository: ModeRepository,
    private val saveStrip: SaveStripUseCase,
    private val deleteStrip: DeleteStripUseCase,
) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val _uiState = MutableStateFlow(
        StripEditorUiState(draft = StripEditorDraft(boardId = "")),
    )
    val uiState: StateFlow<StripEditorUiState> = _uiState.asStateFlow()

    private var pendingLeave: (() -> Unit)? = null
    private var boardsJob: Job? = null
    private var modesJob: Job? = null
    private var loaded = false

    fun load(stripId: String?, boardId: String) {
        if (loaded) return
        loaded = true
        viewModelScope.launch {
            val existing = stripId?.let { stripRepository.getStrip(it) }
            val restored = savedStateHandle.get<String>(KEY_DRAFT)
                ?.let { encoded -> runCatching { json.decodeFromString<StripEditorDraft>(encoded) }.getOrNull() }
            val draft = restored ?: existing?.toDraft() ?: StripEditorDraft(boardId = boardId)
            _uiState.update { it.copy(draft = draft, isLoading = false) }
            boardsJob = viewModelScope.launch {
                boardRepository.boards.collect { boards ->
                    _uiState.update { it.copy(boards = boards) }
                }
            }
            modesJob = viewModelScope.launch {
                modeRepository.modes.collect { modes ->
                    _uiState.update { it.copy(modes = modes) }
                }
            }
        }
    }

    fun onNameChange(name: String) = edit { it.copy(name = name) }

    fun onBoardChange(boardId: String) = edit {
        // Moving a strip to a board with a smaller pixel limit must not leave the strip above it.
        val limit = _uiState.value.boards.firstOrNull { board -> board.id == boardId }
            ?.effectivePixelLimit ?: LumaLimits.MAX_LEDS
        val leds = it.activeLeds.toIntOrNull() ?: LumaLimits.DEFAULT_ACTIVE_LEDS
        it.copy(boardId = boardId, activeLeds = leds.coerceAtMost(limit).toString())
    }

    fun onGpioChange(value: String) = edit { it.copy(gpio = value.filter(Char::isDigit).take(2)) }

    fun onActiveLedsChange(value: String) = edit {
        it.copy(activeLeds = value.filter(Char::isDigit).take(4))
    }

    fun onBrightnessChange(brightness: Float) = edit {
        it.copy(brightness = brightness.coerceIn(0f, LumaLimits.MAX_BRIGHTNESS))
    }

    fun onModeChange(modeId: String?) = edit { it.copy(modeId = modeId) }

    fun save() {
        val state = _uiState.value
        if (state.isSaving) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val draft = state.draft
            val result = saveStrip(
                stripId = draft.stripId,
                boardId = draft.boardId,
                name = draft.name,
                gpio = draft.gpio.toIntOrNull() ?: -1,
                activeLeds = draft.activeLeds.toIntOrNull() ?: 0,
                brightness = draft.brightness,
                modeId = draft.modeId,
            )
            val saved = result.savedValue()
            if (saved == null) {
                pendingLeave = null
                _uiState.update {
                    it.copy(isSaving = false, errors = result.errors(), showLeaveDialog = false)
                }
                return@launch
            }
            val nextDraft = draft.copy(stripId = saved.id, isNew = false, isDirty = false)
            persist(nextDraft)
            _uiState.update {
                it.copy(
                    draft = nextDraft,
                    isSaving = false,
                    errors = emptySet(),
                    showLeaveDialog = false,
                    savedMessage = saved.name,
                )
            }
            pendingLeave?.let { leave ->
                pendingLeave = null
                _uiState.update { it.copy(isGone = true) }
                leave()
            }
        }
    }

    fun requestLeave(onLeave: () -> Unit) {
        if (!_uiState.value.draft.isDirty) {
            onLeave()
            return
        }
        pendingLeave = onLeave
        _uiState.update { it.copy(showLeaveDialog = true) }
    }

    fun discardAndLeave() {
        val leave = pendingLeave
        pendingLeave = null
        _uiState.update { it.copy(showLeaveDialog = false, isGone = true) }
        leave?.invoke()
    }

    fun cancelLeave() {
        pendingLeave = null
        _uiState.update { it.copy(showLeaveDialog = false) }
    }

    fun onDeleteStrip() {
        val stripId = _uiState.value.draft.stripId ?: return
        viewModelScope.launch {
            deleteStrip(stripId)
            _uiState.update { it.copy(isGone = true) }
            pendingLeave?.let { leave ->
                pendingLeave = null
                leave()
            }
        }
    }

    fun consumeSavedMessage() {
        _uiState.update { it.copy(savedMessage = null) }
    }

    private fun edit(transform: (StripEditorDraft) -> StripEditorDraft) {
        val draft = transform(_uiState.value.draft).copy(isDirty = true)
        persist(draft)
        _uiState.update { it.copy(draft = draft) }
    }

    private fun persist(draft: StripEditorDraft) {
        savedStateHandle[KEY_DRAFT] = json.encodeToString(draft)
    }

    private companion object {
        const val KEY_DRAFT = "strip_editor_draft"
    }
}

private fun LumaStrip.toDraft(): StripEditorDraft = StripEditorDraft(
    stripId = id,
    isNew = false,
    boardId = boardId,
    name = name,
    gpio = gpio.toString(),
    activeLeds = activeLeds.toString(),
    brightness = brightness,
    modeId = modeId,
)
