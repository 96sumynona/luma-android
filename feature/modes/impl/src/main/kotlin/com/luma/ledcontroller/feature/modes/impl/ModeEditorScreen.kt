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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luma.ledcontroller.core.designsystem.component.LumaButton
import com.luma.ledcontroller.core.designsystem.component.LumaColorPickerDialog
import com.luma.ledcontroller.core.designsystem.component.LumaDropdown
import com.luma.ledcontroller.core.designsystem.component.LumaFilterChip
import com.luma.ledcontroller.core.designsystem.component.LumaModePreview
import com.luma.ledcontroller.core.designsystem.component.LumaRampEditor
import com.luma.ledcontroller.core.designsystem.component.LumaSectionCard
import com.luma.ledcontroller.core.designsystem.component.LumaSettingRow
import com.luma.ledcontroller.core.designsystem.component.LumaTag
import com.luma.ledcontroller.core.designsystem.component.LumaTextButton
import com.luma.ledcontroller.core.designsystem.component.LumaTextField
import com.luma.ledcontroller.core.designsystem.component.toComposeColor
import com.luma.ledcontroller.core.designsystem.icon.LumaIcons
import com.luma.ledcontroller.core.model.AnimationKind
import com.luma.ledcontroller.core.model.LumaError
import com.luma.ledcontroller.core.model.LumaLimits
import com.luma.ledcontroller.core.ui.LeaveGuardEffect
import com.luma.ledcontroller.core.ui.LocalSnackbarHostState
import com.luma.ledcontroller.core.ui.LumaScreenEnter
import com.luma.ledcontroller.core.ui.UnsavedChangesDialog
import kotlin.math.roundToInt

/**
 * The mode editor.
 *
 * Everything here edits a draft. The saved mode only changes when the user presses Save, and leaving
 * with unsaved work always asks Discard / Save / Save as new mode first.
 */
@Composable
fun ModeEditorScreen(
    modeId: String?,
    modifier: Modifier = Modifier,
    viewModel: ModeEditorViewModel = hiltViewModel(),
) {
    LaunchedEffect(modeId) { viewModel.load(modeId) }

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = LocalSnackbarHostState.current
    val savedMessage = stringResource(R.string.feature_modes_impl_saved)

    // One registration covers system back, toolbar back and tab switches.
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
            onSave = viewModel::saveAndLeave,
            onSaveAsNewMode = viewModel::saveAsNewAndLeave,
            onKeepEditing = viewModel::cancelLeave,
        )
    }

    var colorEditingStopId by remember { mutableStateOf<String?>(null) }
    colorEditingStopId?.let { stopId ->
        state.draft.ramp.stop(stopId)?.let { stop ->
            LumaColorPickerDialog(
                initialColor = stop.color,
                title = stringResource(R.string.feature_modes_impl_stop_colour_title),
                onColorChange = { color -> viewModel.onStopColorChanged(stopId, color) },
                onDismiss = { colorEditingStopId = null },
            )
        }
    }

    LumaScreenEnter(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (state.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.feature_modes_impl_preview_title),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item(key = "preview") {
                        LumaSectionCard(
                            title = stringResource(R.string.feature_modes_impl_preview_title),
                            icon = LumaIcons.AutoAwesome,
                            subtitle = stringResource(R.string.feature_modes_impl_preview_hint),
                        ) {
                            LumaModePreview(
                                ramp = state.draft.ramp,
                                animation = state.draft.animation,
                                modifier = Modifier.fillMaxWidth(),
                                height = 40.dp,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                LumaTag(
                                    text = if (state.draft.ramp.stops.size > 1) {
                                        stringResource(
                                            R.string.feature_modes_impl_stops,
                                            state.draft.ramp.stops.size,
                                        )
                                    } else {
                                        stringResource(R.string.feature_modes_impl_solid)
                                    },
                                    leadingIcon = LumaIcons.Palette,
                                )
                                LumaTag(
                                    text = state.draft.animation.kind.label(),
                                    leadingIcon = state.draft.animation.kind.icon(),
                                )
                                if (state.draft.isDirty) {
                                    LumaTag(
                                        text = stringResource(R.string.feature_modes_impl_unsaved),
                                        leadingIcon = LumaIcons.Edit,
                                    )
                                }
                            }
                        }
                    }

                    item(key = "details") {
                        LumaSectionCard(
                            title = stringResource(R.string.feature_modes_impl_editor_details),
                            icon = LumaIcons.Edit,
                        ) {
                            LumaTextField(
                                value = state.draft.name,
                                onValueChange = viewModel::onNameChange,
                                label = stringResource(R.string.feature_modes_impl_name_label),
                                error = if (LumaError.NAME_BLANK in state.errors) {
                                    stringResource(R.string.feature_modes_impl_name_error)
                                } else {
                                    null
                                },
                                placeholder = stringResource(R.string.feature_modes_impl_name_placeholder),
                            )
                        }
                    }

                    item(key = "ramp") {
                        LumaSectionCard(
                            title = stringResource(R.string.feature_modes_impl_ramp_title),
                            icon = LumaIcons.Palette,
                            subtitle = stringResource(R.string.feature_modes_impl_ramp_hint),
                        ) {
                            LumaRampEditor(
                                ramp = state.draft.ramp,
                                selectedStopId = state.draft.selectedStopId,
                                onStopSelected = viewModel::onStopSelected,
                                onStopMoved = viewModel::onStopMoved,
                                modifier = Modifier.fillMaxWidth(),
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                state.draft.ramp.sortedStops.forEachIndexed { index, stop ->
                                    LumaFilterChip(
                                        selected = stop.id == state.draft.selectedStopId,
                                        onSelectedChange = { viewModel.onStopSelected(stop.id) },
                                        label = {
                                            Text(
                                                text = "${index + 1} · ${stop.position.roundToInt()}%",
                                            )
                                        },
                                    )
                                }
                            }

                            val selectedStop = state.draft.ramp.stop(state.draft.selectedStopId.orEmpty())
                                ?: state.draft.ramp.sortedStops.firstOrNull()

                            if (selectedStop != null) {
                                StopEditor(
                                    stopId = selectedStop.id,
                                    position = selectedStop.position,
                                    color = selectedStop.color,
                                    canRemove = state.draft.ramp.stops.size > LumaLimits.MIN_STOPS,
                                    onPositionChange = { position ->
                                        viewModel.onStopPositionChanged(selectedStop.id, position)
                                    },
                                    onEditColor = { colorEditingStopId = selectedStop.id },
                                    onRemove = { viewModel.onRemoveStop(selectedStop.id) },
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                LumaButton(
                                    onClick = viewModel::onAddStop,
                                    enabled = state.draft.ramp.stops.size < LumaLimits.MAX_STOPS,
                                ) {
                                    Icon(
                                        imageVector = LumaIcons.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = stringResource(R.string.feature_modes_impl_add_stop))
                                }
                                if (state.draft.ramp.stops.size >= LumaLimits.MAX_STOPS) {
                                    Text(
                                        text = stringResource(R.string.feature_modes_impl_add_stop_limit),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }

                            if (LumaError.RAMP_INVALID in state.errors) {
                                Text(
                                    text = stringResource(R.string.feature_modes_impl_ramp_invalid),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }

                    item(key = "animation") {
                        val animation = state.draft.animation
                        LumaSectionCard(
                            title = stringResource(R.string.feature_modes_impl_animation_title),
                            icon = LumaIcons.Speed,
                            subtitle = stringResource(R.string.feature_modes_impl_animation_hint),
                        ) {
                            LumaDropdown(
                                label = stringResource(R.string.feature_modes_impl_animation_kind),
                                options = AnimationKind.entries.toList(),
                                selected = animation.kind,
                                onSelected = { kind ->
                                    viewModel.onAnimationChanged(animation.copy(kind = kind))
                                },
                                optionLabel = { kind -> kind.label() },
                                selectedIcon = animation.kind.icon(),
                                optionIcon = { kind -> kind.icon() },
                                optionSupportingText = { kind -> kind.description() },
                            )

                            AnimationSlider(
                                label = stringResource(R.string.feature_modes_impl_animation_speed),
                                value = animation.speed,
                                onChange = { speed ->
                                    viewModel.onAnimationChanged(animation.copy(speed = speed))
                                },
                            )

                            if (animation.kind == AnimationKind.CHASE ||
                                animation.kind == AnimationKind.TWINKLE
                            ) {
                                AnimationSlider(
                                    label = stringResource(R.string.feature_modes_impl_animation_spread),
                                    value = animation.spread,
                                    onChange = { spread ->
                                        viewModel.onAnimationChanged(animation.copy(spread = spread))
                                    },
                                )
                            }

                            LumaSettingRow(
                                title = stringResource(R.string.feature_modes_impl_animation_reverse),
                                icon = LumaIcons.Repeat,
                                trailing = {
                                    Switch(
                                        checked = animation.reverse,
                                        onCheckedChange = { reverse ->
                                            viewModel.onAnimationChanged(
                                                animation.copy(reverse = reverse),
                                            )
                                        },
                                    )
                                },
                            )
                        }
                    }

                    if (!state.draft.isNew) {
                        item(key = "delete") {
                            LumaTextButton(
                                onClick = viewModel::onDeleteMode,
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
                                    text = stringResource(R.string.feature_modes_impl_delete),
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                }

                EditorActions(
                    isDirty = state.draft.isDirty,
                    canSave = state.canSave,
                    isNew = state.draft.isNew,
                    onSave = { viewModel.save() },
                    onSaveAsNew = { viewModel.save(saveAsNew = true) },
                )
            }
        }
    }
}

@Composable
private fun StopEditor(
    stopId: String,
    position: Float,
    color: Long,
    canRemove: Boolean,
    onPositionChange: (Float) -> Unit,
    onEditColor: () -> Unit,
    onRemove: () -> Unit,
) {
    var positionText by remember(stopId) { mutableStateOf(position.roundToInt().toString()) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(color.toComposeColor())
                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                .clickable(onClick = onEditColor),
        )
        LumaTextField(
            value = positionText,
            onValueChange = { text ->
                val digits = text.filter(Char::isDigit).take(3)
                positionText = digits
                digits.toFloatOrNull()?.let { value ->
                    onPositionChange(value.coerceIn(0f, 100f))
                }
            },
            label = stringResource(R.string.feature_modes_impl_position),
            modifier = Modifier.weight(1f),
            supportingText = "0–100",
        )
        IconButton(onClick = onEditColor) {
            Icon(
                imageVector = LumaIcons.Palette,
                contentDescription = stringResource(R.string.feature_modes_impl_edit_colour),
            )
        }
        IconButton(onClick = onRemove, enabled = canRemove) {
            Icon(
                imageVector = LumaIcons.Delete,
                contentDescription = stringResource(R.string.feature_modes_impl_remove_stop),
                tint = if (canRemove) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

@Composable
private fun AnimationSlider(label: String, value: Float, onChange: (Float) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = label, style = MaterialTheme.typography.labelLarge)
            Text(
                text = "${(value * 100).roundToInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Slider(value = value, onValueChange = onChange, valueRange = 0f..1f)
    }
}

@Composable
private fun EditorActions(
    isDirty: Boolean,
    canSave: Boolean,
    isNew: Boolean,
    onSave: () -> Unit,
    onSaveAsNew: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (isDirty) {
                LumaTag(
                    text = stringResource(R.string.feature_modes_impl_unsaved),
                    leadingIcon = LumaIcons.Edit,
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            if (!isNew) {
                LumaTextButton(onClick = onSaveAsNew) {
                    Text(text = stringResource(R.string.feature_modes_impl_save_as_new))
                }
            }
            LumaButton(onClick = onSave, enabled = canSave) {
                Icon(
                    imageVector = LumaIcons.Save,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.feature_modes_impl_save))
            }
        }
    }
}
