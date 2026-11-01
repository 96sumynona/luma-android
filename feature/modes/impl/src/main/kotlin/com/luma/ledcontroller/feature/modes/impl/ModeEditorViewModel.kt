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

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luma.ledcontroller.core.data.IdGenerator
import com.luma.ledcontroller.core.domain.DeleteModeUseCase
import com.luma.ledcontroller.core.domain.GetModesUseCase
import com.luma.ledcontroller.core.domain.SaveModeUseCase
import com.luma.ledcontroller.core.domain.errors
import com.luma.ledcontroller.core.domain.savedValue
import com.luma.ledcontroller.core.model.ColorRamp
import com.luma.ledcontroller.core.model.ColorStop
import com.luma.ledcontroller.core.model.LumaError
import com.luma.ledcontroller.core.model.LumaLimits
import com.luma.ledcontroller.core.model.LumaMode
import com.luma.ledcontroller.core.model.ModeAnimation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject

/**
 * The editor's working copy of a mode.
 *
 * This is deliberately the only representation of "unsaved work": it is serialised into the
 * [SavedStateHandle] on every edit, so neither recomposition, rotation, nor process death can lose
 * a draft before the user decides what to do with it.
 */
@Serializable
data class ModeEditorDraft(
    val modeId: String? = null,
    val isNew: Boolean = true,
    val name: String = "",
    val ramp: ColorRamp = ColorRamp.default(),
    val animation: ModeAnimation = ModeAnimation.Default,
    val selectedStopId: String? = null,
    val isDirty: Boolean = false,
)

data class ModeEditorUiState(
    val draft: ModeEditorDraft = ModeEditorDraft(),
    val errors: Set<LumaError> = emptySet(),
    val isSaving: Boolean = false,
    val isLoading: Boolean = false,
    val showLeaveDialog: Boolean = false,
    val isGone: Boolean = false,
    val savedMessage: String? = null,
) {
    val canSave: Boolean
        get() = !isSaving && !isLoading
}

@HiltViewModel
class ModeEditorViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val getModes: GetModesUseCase,
    private val saveMode: SaveModeUseCase,
    private val deleteMode: DeleteModeUseCase,
    private val idGenerator: IdGenerator,
) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val _uiState = MutableStateFlow(ModeEditorUiState())
    val uiState: StateFlow<ModeEditorUiState> = _uiState.asStateFlow()

    /** What "leaving" means for whoever asked, so the dialog can hand control back. */
    private var pendingLeave: (() -> Unit)? = null

    private var loaded = false

    /**
     * Loads the editor once. A restored draft always wins over the stored mode: it is the user's
     * unsaved work.
     */
    fun load(modeId: String?) {
        if (loaded) return
        loaded = true

        val restored = savedStateHandle.get<String>(KEY_DRAFT)
            ?.let { encoded -> runCatching { json.decodeFromString<ModeEditorDraft>(encoded) }.getOrNull() }
        if (restored != null) {
            _uiState.value = ModeEditorUiState(draft = restored)
            return
        }

        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val mode = if (modeId == null) null else getModes().first().firstOrNull { it.id == modeId }
            val draft = if (mode == null) {
                newDraft()
            } else {
                ModeEditorDraft(
                    modeId = mode.id,
                    isNew = false,
                    name = mode.name,
                    ramp = mode.ramp,
                    animation = mode.animation,
                    selectedStopId = mode.ramp.sortedStops.firstOrNull()?.id,
                )
            }
            _uiState.value = ModeEditorUiState(draft = draft)
        }
    }

    fun onNameChange(name: String) = edit { it.copy(name = name) }

    fun onStopSelected(stopId: String) = edit(markDirty = false) { it.copy(selectedStopId = stopId) }

    fun onStopMoved(stopId: String, position: Float) = edit {
        it.copy(
            ramp = it.ramp.withStopMoved(stopId, position),
            selectedStopId = stopId,
        )
    }

    fun onStopPositionChanged(stopId: String, position: Float) = onStopMoved(stopId, position)

    fun onStopColorChanged(stopId: String, color: Long) = edit {
        it.copy(ramp = it.ramp.withStopColor(stopId, color), selectedStopId = stopId)
    }

    fun onAddStop() = edit {
        val ramp = it.ramp
        if (ramp.stops.size >= LumaLimits.MAX_STOPS) return@edit it
        val widestGap = ramp.sortedStops.zipWithNext()
            .maxByOrNull { (from, to) -> to.position - from.position }
        val position = widestGap?.let { (from, to) -> (from.position + to.position) / 2f } ?: 50f
        val color = ramp.colorAt(position)
        val stopId = idGenerator.newId()
        it.copy(
            ramp = ramp.withStopAdded(stopId = stopId, position = position, color = color),
            selectedStopId = stopId,
        )
    }

    fun onRemoveStop(stopId: String) = edit {
        val ramp = it.ramp.withStopRemoved(stopId)
        if (ramp.stops.size == it.ramp.stops.size) {
            it
        } else {
            it.copy(
                ramp = ramp,
                selectedStopId = ramp.sortedStops.firstOrNull()?.id,
            )
        }
    }

    fun onAnimationChanged(animation: ModeAnimation) = edit { it.copy(animation = animation) }

    /** Saves the draft. Returns control to [pendingLeave] only when the save succeeded. */
    fun save(saveAsNew: Boolean = false) {
        val state = _uiState.value
        if (state.isSaving) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val result = saveMode(
                modeId = if (saveAsNew) null else state.draft.modeId,
                name = state.draft.name,
                ramp = state.draft.ramp,
                animation = state.draft.animation,
            )
            val saved = result.savedValue()
            if (saved == null) {
                // Keep the editor open so the errors are visible; the draft is untouched.
                pendingLeave = null
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errors = result.errors(),
                        showLeaveDialog = false,
                    )
                }
                return@launch
            }
            val nextDraft = state.draft.copy(
                modeId = saved.id,
                isNew = false,
                name = saved.name,
                isDirty = false,
            )
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

    /**
     * Called by the app shell for every way of leaving: system back, toolbar back and tab switches.
     */
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

    fun saveAndLeave() = save(saveAsNew = false)

    fun saveAsNewAndLeave() = save(saveAsNew = true)

    fun cancelLeave() {
        pendingLeave = null
        _uiState.update { it.copy(showLeaveDialog = false) }
    }

    fun onDeleteMode() {
        val modeId = _uiState.value.draft.modeId ?: return
        viewModelScope.launch {
            deleteMode.invoke(modeId)
            pendingLeave?.let { leave ->
                pendingLeave = null
                _uiState.update { it.copy(isGone = true) }
                leave()
            }
        }
    }

    fun consumeSavedMessage() {
        _uiState.update { it.copy(savedMessage = null) }
    }

    private fun edit(markDirty: Boolean = true, transform: (ModeEditorDraft) -> ModeEditorDraft) {
        val current = _uiState.value
        if (!markDirty) {
            _uiState.update { it.copy(draft = transform(it.draft)) }
            return
        }
        val draft = transform(current.draft).copy(isDirty = true)
        persist(draft)
        _uiState.update {
            it.copy(
                draft = draft,
                errors = emptySet(),
            )
        }
    }

    private fun persist(draft: ModeEditorDraft) {
        savedStateHandle[KEY_DRAFT] = json.encodeToString(draft)
    }

    private fun newDraft(): ModeEditorDraft {
        val ramp = ColorRamp.default()
        return ModeEditorDraft(
            modeId = null,
            isNew = true,
            name = "",
            ramp = ramp,
            animation = ModeAnimation.Default,
            selectedStopId = ramp.sortedStops.firstOrNull()?.id,
        )
    }

    private companion object {
        const val KEY_DRAFT = "mode_editor_draft"
    }
}

/** Convenience for previews and tests. */
internal fun LumaMode.toDraft(): ModeEditorDraft = ModeEditorDraft(
    modeId = id,
    isNew = false,
    name = name,
    ramp = ramp,
    animation = animation,
    selectedStopId = ramp.sortedStops.firstOrNull()?.id,
)

/** A new stop placed at [position] with the colour the ramp already has there. */
internal fun ColorRamp.stopAt(position: Float, id: String): ColorStop =
    ColorStop(id = id, position = position, color = colorAt(position))
