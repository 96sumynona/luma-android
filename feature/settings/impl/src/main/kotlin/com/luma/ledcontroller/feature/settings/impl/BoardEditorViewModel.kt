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
import com.luma.ledcontroller.core.data.repository.StripRepository
import com.luma.ledcontroller.core.domain.DeleteBoardUseCase
import com.luma.ledcontroller.core.domain.SaveBoardUseCase
import com.luma.ledcontroller.core.domain.errors
import com.luma.ledcontroller.core.domain.savedValue
import com.luma.ledcontroller.core.model.LedLayout
import com.luma.ledcontroller.core.model.LumaBoard
import com.luma.ledcontroller.core.model.LumaError
import com.luma.ledcontroller.core.model.LumaLimits
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
data class BoardEditorDraft(
    val boardId: String? = null,
    val isNew: Boolean = true,
    val name: String = "",
    val pixelLimit: String = LumaLimits.DEFAULT_PIXEL_LIMIT.toString(),
    val bufferSizeBytes: String = LumaLimits.DEFAULT_BUFFER_SIZE.toString(),
    val layout: LedLayout = LedLayout.LINEAR,
    val isDirty: Boolean = false,
)

data class BoardEditorUiState(
    val draft: BoardEditorDraft = BoardEditorDraft(),
    val strips: List<LumaStrip> = emptyList(),
    val errors: Set<LumaError> = emptySet(),
    val isSaving: Boolean = false,
    val isLoading: Boolean = true,
    val showLeaveDialog: Boolean = false,
    val isGone: Boolean = false,
    val savedMessage: String? = null,
)

@HiltViewModel
class BoardEditorViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val boardRepository: BoardRepository,
    private val stripRepository: StripRepository,
    private val saveBoard: SaveBoardUseCase,
    private val deleteBoard: DeleteBoardUseCase,
) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val _uiState = MutableStateFlow(BoardEditorUiState())
    val uiState: StateFlow<BoardEditorUiState> = _uiState.asStateFlow()

    private var pendingLeave: (() -> Unit)? = null
    private var stripsJob: Job? = null
    private var loaded = false

    fun load(boardId: String?) {
        if (loaded) return
        loaded = true
        viewModelScope.launch {
            val existing = boardId?.let { boardRepository.getBoard(it) }
            val restored = savedStateHandle.get<String>(KEY_DRAFT)
                ?.let { encoded -> runCatching { json.decodeFromString<BoardEditorDraft>(encoded) }.getOrNull() }
            val draft = restored ?: existing?.toDraft() ?: BoardEditorDraft()
            _uiState.value = BoardEditorUiState(draft = draft, isLoading = false)
            observeStrips(draft.boardId)
        }
    }

    fun onNameChange(name: String) = edit { it.copy(name = name) }

    fun onPixelLimitChange(value: String) = edit {
        it.copy(pixelLimit = value.filter(Char::isDigit).take(4))
    }

    fun onBufferSizeChange(value: String) = edit {
        it.copy(bufferSizeBytes = value.filter(Char::isDigit).take(5))
    }

    fun onLayoutChange(layout: LedLayout) = edit { it.copy(layout = layout) }

    fun save() {
        val state = _uiState.value
        if (state.isSaving) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val draft = state.draft
            val result = saveBoard(
                boardId = draft.boardId,
                name = draft.name,
                pixelLimit = draft.pixelLimit.toIntOrNull() ?: 0,
                bufferSizeBytes = draft.bufferSizeBytes.toIntOrNull() ?: 0,
                layout = draft.layout,
            )
            val saved = result.savedValue()
            if (saved == null) {
                pendingLeave = null
                _uiState.update {
                    it.copy(isSaving = false, errors = result.errors(), showLeaveDialog = false)
                }
                return@launch
            }
            val nextDraft = draft.copy(boardId = saved.id, isNew = false, isDirty = false)
            persist(nextDraft)
            observeStrips(saved.id)
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

    fun onDeleteBoard() {
        val boardId = _uiState.value.draft.boardId ?: return
        viewModelScope.launch {
            deleteBoard(boardId)
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

    private fun observeStrips(boardId: String?) {
        stripsJob?.cancel()
        if (boardId == null) {
            _uiState.update { it.copy(strips = emptyList()) }
            return
        }
        stripsJob = viewModelScope.launch {
            stripRepository.observeStripsForBoard(boardId).collect { strips ->
                _uiState.update { it.copy(strips = strips) }
            }
        }
    }

    private fun edit(transform: (BoardEditorDraft) -> BoardEditorDraft) {
        val draft = transform(_uiState.value.draft).copy(isDirty = true)
        persist(draft)
        _uiState.update { it.copy(draft = draft) }
    }

    private fun persist(draft: BoardEditorDraft) {
        savedStateHandle[KEY_DRAFT] = json.encodeToString(draft)
    }

    private companion object {
        const val KEY_DRAFT = "board_editor_draft"
    }
}

private fun LumaBoard.toDraft(): BoardEditorDraft = BoardEditorDraft(
    boardId = id,
    isNew = false,
    name = name,
    pixelLimit = pixelLimit.toString(),
    bufferSizeBytes = bufferSizeBytes.toString(),
    layout = layout,
)
