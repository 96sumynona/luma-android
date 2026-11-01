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

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luma.ledcontroller.core.data.repository.ModeRepository
import com.luma.ledcontroller.core.data.repository.ScheduleRepository
import com.luma.ledcontroller.core.data.repository.StripRepository
import com.luma.ledcontroller.core.domain.DeleteScheduleUseCase
import com.luma.ledcontroller.core.domain.SaveScheduleUseCase
import com.luma.ledcontroller.core.domain.ValidateScheduleUseCase
import com.luma.ledcontroller.core.domain.errors
import com.luma.ledcontroller.core.domain.savedValue
import com.luma.ledcontroller.core.model.LumaError
import com.luma.ledcontroller.core.model.LumaLimits
import com.luma.ledcontroller.core.model.LumaMode
import com.luma.ledcontroller.core.model.LumaSchedule
import com.luma.ledcontroller.core.model.LumaStrip
import com.luma.ledcontroller.core.model.Weekday
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
 * The schedule editor's unsaved work, serialised into [SavedStateHandle] so a draft survives
 * recomposition, rotation and process death.
 */
@Serializable
data class ScheduleEditorDraft(
    val scheduleId: String? = null,
    val isNew: Boolean = true,
    val name: String = "",
    val stripId: String? = null,
    val modeId: String? = null,
    val hour: Int = 19,
    val minute: Int = 0,
    val repeatDays: Set<Weekday> = Weekday.entries.toSet(),
    val transitionSeconds: Int = 0,
    val isDirty: Boolean = false,
)

data class ScheduleEditorUiState(
    val draft: ScheduleEditorDraft = ScheduleEditorDraft(),
    val strips: List<LumaStrip> = emptyList(),
    val modes: List<LumaMode> = emptyList(),
    val errors: Set<LumaError> = emptySet(),
    val isSaving: Boolean = false,
    val isLoading: Boolean = true,
    val showLeaveDialog: Boolean = false,
    val isGone: Boolean = false,
    val savedMessage: String? = null,
)

@HiltViewModel
class ScheduleEditorViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    stripRepository: StripRepository,
    modeRepository: ModeRepository,
    private val scheduleRepository: ScheduleRepository,
    private val saveSchedule: SaveScheduleUseCase,
    private val validateSchedule: ValidateScheduleUseCase,
    private val deleteSchedule: DeleteScheduleUseCase,
) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val _uiState = MutableStateFlow(ScheduleEditorUiState())
    val uiState: StateFlow<ScheduleEditorUiState> = _uiState.asStateFlow()

    private var pendingLeave: (() -> Unit)? = null
    private var loaded = false

    private val strips = stripRepository.strips
    private val modes = modeRepository.modes

    fun load(scheduleId: String?, presetStripId: String?) {
        if (loaded) return
        loaded = true
        viewModelScope.launch {
            val stripList = strips.first()
            val modeList = modes.first()
            val restored = savedStateHandle.get<String>(KEY_DRAFT)
                ?.let { encoded -> runCatching { json.decodeFromString<ScheduleEditorDraft>(encoded) }.getOrNull() }
            val existing = scheduleId?.let { scheduleRepository.getSchedule(it) }
            val draft = restored ?: when {
                existing != null -> existing.toDraft()
                else -> ScheduleEditorDraft(
                    stripId = presetStripId ?: stripList.firstOrNull()?.id,
                    modeId = stripList.firstOrNull { it.id == presetStripId }?.modeId
                        ?: modeList.firstOrNull()?.id,
                )
            }
            _uiState.value = ScheduleEditorUiState(
                draft = draft,
                strips = stripList,
                modes = modeList,
                isLoading = false,
            )
        }
    }

    fun onNameChange(name: String) = edit { it.copy(name = name) }

    fun onStripSelected(stripId: String) = edit { it.copy(stripId = stripId) }

    fun onModeSelected(modeId: String) = edit { it.copy(modeId = modeId) }

    fun onTimeChanged(hour: Int, minute: Int) = edit {
        it.copy(hour = hour.coerceIn(0, 23), minute = minute.coerceIn(0, 59))
    }

    fun onDayToggled(day: Weekday) = edit {
        val days = if (day in it.repeatDays) it.repeatDays - day else it.repeatDays + day
        it.copy(repeatDays = days)
    }

    fun onAllDaysToggled(selected: Boolean) = edit {
        it.copy(repeatDays = if (selected) Weekday.entries.toSet() else emptySet())
    }

    fun onTransitionChanged(seconds: Int) = edit {
        it.copy(
            transitionSeconds = seconds.coerceIn(
                LumaLimits.MIN_TRANSITION_SECONDS,
                LumaLimits.MAX_TRANSITION_SECONDS,
            ),
        )
    }

    fun save() {
        val state = _uiState.value
        if (state.isSaving) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val draft = state.draft
            val result = saveSchedule(
                scheduleId = draft.scheduleId,
                name = draft.name,
                stripId = draft.stripId,
                modeId = draft.modeId,
                hour = draft.hour,
                minute = draft.minute,
                repeatDays = draft.repeatDays,
                transitionSeconds = draft.transitionSeconds,
            )
            val saved = result.savedValue()
            if (saved == null) {
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
            val nextDraft = draft.copy(
                scheduleId = saved.id,
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

    fun onDeleteSchedule() {
        val scheduleId = _uiState.value.draft.scheduleId ?: return
        viewModelScope.launch {
            deleteSchedule.invoke(scheduleId)
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

    private fun edit(transform: (ScheduleEditorDraft) -> ScheduleEditorDraft) {
        val draft = transform(_uiState.value.draft).copy(isDirty = true)
        persist(draft)
        _uiState.update {
            it.copy(
                draft = draft,
                errors = validateSchedule(
                    stripId = draft.stripId,
                    modeId = draft.modeId,
                    hour = draft.hour,
                    minute = draft.minute,
                    repeatDays = draft.repeatDays,
                    transitionSeconds = draft.transitionSeconds,
                ),
            )
        }
    }

    private fun persist(draft: ScheduleEditorDraft) {
        savedStateHandle[KEY_DRAFT] = json.encodeToString(draft)
    }

    private companion object {
        const val KEY_DRAFT = "schedule_editor_draft"
    }
}

private fun LumaSchedule.toDraft(): ScheduleEditorDraft = ScheduleEditorDraft(
    scheduleId = id,
    isNew = false,
    name = name,
    stripId = stripId,
    modeId = modeId,
    hour = hour,
    minute = minute,
    repeatDays = repeatDays,
    transitionSeconds = transitionSeconds,
)
