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

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luma.ledcontroller.core.designsystem.component.LumaButton
import com.luma.ledcontroller.core.designsystem.component.LumaDropdown
import com.luma.ledcontroller.core.designsystem.component.LumaFilterChip
import com.luma.ledcontroller.core.designsystem.component.LumaSectionCard
import com.luma.ledcontroller.core.designsystem.component.LumaSettingRow
import com.luma.ledcontroller.core.designsystem.component.LumaTag
import com.luma.ledcontroller.core.designsystem.component.LumaTextButton
import com.luma.ledcontroller.core.designsystem.component.LumaTextField
import com.luma.ledcontroller.core.designsystem.icon.LumaIcons
import com.luma.ledcontroller.core.model.LumaError
import com.luma.ledcontroller.core.model.LumaLimits
import com.luma.ledcontroller.core.model.Weekday
import com.luma.ledcontroller.core.ui.LeaveGuardEffect
import com.luma.ledcontroller.core.ui.LocalSnackbarHostState
import com.luma.ledcontroller.core.ui.LumaScreenEnter
import com.luma.ledcontroller.core.ui.UnsavedChangesDialog
import com.luma.ledcontroller.core.ui.formatRepeatDays
import com.luma.ledcontroller.core.ui.shortLabel
import kotlin.math.roundToInt

/**
 * The schedule editor.
 *
 * Everything is a draft until Save. The time picker, the day chips and the transition length all
 * write into the same draft, and leaving with unsaved changes asks before anything is dropped.
 */
@Composable
fun ScheduleEditorScreen(
    scheduleId: String?,
    presetStripId: String?,
    modifier: Modifier = Modifier,
    viewModel: ScheduleEditorViewModel = hiltViewModel(),
) {
    LaunchedEffect(scheduleId, presetStripId) { viewModel.load(scheduleId, presetStripId) }

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = LocalSnackbarHostState.current
    val savedMessage = stringResource(R.string.feature_schedules_impl_saved)

    LeaveGuardEffect(isDirty = state.draft.isDirty) { proceed -> viewModel.requestLeave(proceed) }

    LaunchedEffect(state.savedMessage, state.isGone) {
        if (state.savedMessage != null && !state.isGone) {
            viewModel.consumeSavedMessage()
            snackbarHostState.showSnackbar(savedMessage)
        }
    }

    if (state.showLeaveDialog) {
        UnsavedChangesDialog(
            onDiscard = viewModel::discardAndLeave,
            onSave = viewModel::save,
            onSaveAsNewMode = viewModel::save,
            onKeepEditing = viewModel::cancelLeave,
            showSaveAsNew = false,
        )
    }

    var showTimePicker by remember { mutableStateOf(false) }

    if (showTimePicker) {
        ScheduleTimePickerDialog(
            initialHour = state.draft.hour,
            initialMinute = state.draft.minute,
            onDismiss = { showTimePicker = false },
            onConfirm = { hour, minute ->
                viewModel.onTimeChanged(hour, minute)
                showTimePicker = false
            },
        )
    }

    LumaScreenEnter(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "target") {
                    LumaSectionCard(
                        title = stringResource(R.string.feature_schedules_impl_editor_title),
                        icon = LumaIcons.Schedule,
                    ) {
                        LumaTextField(
                            value = state.draft.name,
                            onValueChange = viewModel::onNameChange,
                            label = stringResource(R.string.feature_schedules_impl_name_label),
                            placeholder = stringResource(R.string.feature_schedules_impl_name_placeholder),
                        )
                        LumaDropdown(
                            label = stringResource(R.string.feature_schedules_impl_strip_label),
                            options = state.strips,
                            selected = state.strips.firstOrNull { it.id == state.draft.stripId },
                            onSelected = { strip -> viewModel.onStripSelected(strip.id) },
                            optionLabel = { strip -> strip.name },
                            selectedIcon = LumaIcons.Memory,
                            optionIcon = { LumaIcons.Memory },
                            optionSupportingText = { strip -> "GPIO ${strip.gpio} · ${strip.activeLeds} LEDs" },
                            emptyText = stringResource(R.string.feature_schedules_impl_no_strips),
                        )
                        LumaDropdown(
                            label = stringResource(R.string.feature_schedules_impl_mode_label),
                            options = state.modes,
                            selected = state.modes.firstOrNull { it.id == state.draft.modeId },
                            onSelected = { mode -> viewModel.onModeSelected(mode.id) },
                            optionLabel = { mode -> mode.name },
                            selectedIcon = LumaIcons.Palette,
                            optionIcon = { LumaIcons.Palette },
                            optionSupportingText = { mode ->
                                if (mode.isGradient) "${mode.stopCount} stops" else "Solid"
                            },
                            emptyText = stringResource(R.string.feature_schedules_impl_no_modes),
                        )
                    }
                }

                item(key = "time") {
                    LumaSectionCard(
                        title = stringResource(R.string.feature_schedules_impl_time_title),
                        icon = LumaIcons.Timer,
                    ) {
                        LumaSettingRow(
                            title = stringResource(R.string.feature_schedules_impl_time_label),
                            value = state.draft.timeLabel(),
                            icon = LumaIcons.Timer,
                            trailing = {
                                LumaTextButton(onClick = { showTimePicker = true }) {
                                    Text(text = stringResource(R.string.feature_schedules_impl_edit))
                                }
                            },
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Weekday.entries.forEach { day ->
                                LumaFilterChip(
                                    selected = day in state.draft.repeatDays,
                                    onSelectedChange = { viewModel.onDayToggled(day) },
                                    label = { Text(text = day.shortLabel()) },
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = formatRepeatDays(state.draft.repeatDays),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            LumaTextButton(
                                onClick = {
                                    viewModel.onAllDaysToggled(
                                        state.draft.repeatDays.size != Weekday.entries.size,
                                    )
                                },
                            ) {
                                Text(text = stringResource(R.string.feature_schedules_impl_all_days))
                            }
                        }
                        if (LumaError.NO_REPEAT_DAYS in state.errors) {
                            Text(
                                text = stringResource(R.string.feature_schedules_impl_error_days),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }

                item(key = "transition") {
                    LumaSectionCard(
                        title = stringResource(R.string.feature_schedules_impl_transition_label),
                        icon = LumaIcons.Repeat,
                        subtitle = stringResource(R.string.feature_schedules_impl_transition_hint),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            LumaTag(
                                text = if (state.draft.transitionSeconds == 0) {
                                    stringResource(R.string.feature_schedules_impl_immediate)
                                } else {
                                    stringResource(
                                        R.string.feature_schedules_impl_seconds,
                                        state.draft.transitionSeconds,
                                    )
                                },
                                leadingIcon = LumaIcons.Repeat,
                            )
                        }
                        Slider(
                            value = state.draft.transitionSeconds.toFloat(),
                            onValueChange = { value -> viewModel.onTransitionChanged(value.roundToInt()) },
                            valueRange = LumaLimits.MIN_TRANSITION_SECONDS.toFloat()..
                                LumaLimits.MAX_TRANSITION_SECONDS.toFloat(),
                            steps = TRANSITION_STEPS,
                        )
                    }
                }

                if (!state.draft.isNew) {
                    item(key = "delete") {
                        LumaTextButton(
                            onClick = viewModel::onDeleteSchedule,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(
                                imageVector = LumaIcons.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.feature_schedules_impl_delete_schedule),
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 3.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (state.draft.isDirty) {
                        LumaTag(
                            text = stringResource(R.string.feature_schedules_impl_unsaved),
                            leadingIcon = LumaIcons.Edit,
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    LumaButton(onClick = viewModel::save, enabled = !state.isSaving) {
                        Icon(
                            imageVector = LumaIcons.Save,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = stringResource(R.string.feature_schedules_impl_save))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit,
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.feature_schedules_impl_time_title)) },
        text = { TimePicker(state = timePickerState) },
        confirmButton = {
            TextButton(onClick = { onConfirm(timePickerState.hour, timePickerState.minute) }) {
                Text(text = stringResource(R.string.feature_schedules_impl_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.feature_schedules_impl_cancel))
            }
        },
    )
}

private fun ScheduleEditorDraft.timeLabel(): String =
    "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"

private const val TRANSITION_STEPS = 59
