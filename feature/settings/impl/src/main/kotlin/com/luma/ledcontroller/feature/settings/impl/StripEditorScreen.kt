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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.luma.ledcontroller.core.designsystem.component.LumaBrightnessSlider
import com.luma.ledcontroller.core.designsystem.component.LumaButton
import com.luma.ledcontroller.core.designsystem.component.LumaConfirmDialog
import com.luma.ledcontroller.core.designsystem.component.LumaDropdown
import com.luma.ledcontroller.core.designsystem.component.LumaModePreview
import com.luma.ledcontroller.core.designsystem.component.LumaNumberField
import com.luma.ledcontroller.core.designsystem.component.LumaSectionCard
import com.luma.ledcontroller.core.designsystem.component.LumaTag
import com.luma.ledcontroller.core.designsystem.component.LumaTextButton
import com.luma.ledcontroller.core.designsystem.component.LumaTextField
import com.luma.ledcontroller.core.designsystem.icon.LumaIcons
import com.luma.ledcontroller.core.model.LumaError
import com.luma.ledcontroller.core.model.LumaLimits
import com.luma.ledcontroller.core.ui.LeaveGuardEffect
import com.luma.ledcontroller.core.ui.LocalSnackbarHostState
import com.luma.ledcontroller.core.ui.LumaScreenEnter
import com.luma.ledcontroller.core.ui.UnsavedChangesDialog
import kotlin.math.roundToInt

/** Strip configuration: which board it belongs to, its GPIO, its LED count, its mode and level. */
@Composable
fun StripEditorScreen(
    stripId: String?,
    boardId: String,
    modifier: Modifier = Modifier,
    viewModel: StripEditorViewModel = hiltViewModel(),
) {
    LaunchedEffect(stripId, boardId) { viewModel.load(stripId, boardId) }

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = LocalSnackbarHostState.current
    val savedMessage = stringResource(R.string.feature_settings_impl_strip_saved)

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

    var showDeleteConfirm by remember { mutableStateOf(false) }
    if (showDeleteConfirm) {
        LumaConfirmDialog(
            title = stringResource(R.string.feature_settings_impl_strip_delete_title),
            message = stringResource(R.string.feature_settings_impl_strip_delete_message),
            confirmLabel = stringResource(R.string.feature_settings_impl_delete),
            dismissLabel = stringResource(R.string.feature_settings_impl_cancel),
            destructive = true,
            onConfirm = {
                showDeleteConfirm = false
                viewModel.onDeleteStrip()
            },
            onDismiss = { showDeleteConfirm = false },
        )
    }

    LumaScreenEnter(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "details") {
                    LumaSectionCard(
                        title = stringResource(R.string.feature_settings_impl_strip_title),
                        icon = LumaIcons.Lightbulb,
                    ) {
                        LumaTextField(
                            value = state.draft.name,
                            onValueChange = viewModel::onNameChange,
                            label = stringResource(R.string.feature_settings_impl_strip_name),
                            error = if (LumaError.NAME_BLANK in state.errors) {
                                stringResource(R.string.feature_settings_impl_name_error)
                            } else {
                                null
                            },
                            placeholder = stringResource(R.string.feature_settings_impl_strip_name_placeholder),
                        )
                        LumaDropdown(
                            label = stringResource(R.string.feature_settings_impl_strip_board),
                            options = state.boards,
                            selected = state.board,
                            onSelected = { board -> viewModel.onBoardChange(board.id) },
                            optionLabel = { board -> board.name },
                            selectedIcon = LumaIcons.Memory,
                            optionIcon = { LumaIcons.Memory },
                            optionSupportingText = { board ->
                                stringResource(
                                    R.string.feature_settings_impl_board_summary,
                                    board.effectivePixelLimit,
                                    board.bufferSizeBytes,
                                    board.layout.label(),
                                )
                            },
                        )
                        LumaNumberField(
                            value = state.draft.gpio,
                            onValueChange = viewModel::onGpioChange,
                            label = stringResource(R.string.feature_settings_impl_strip_gpio),
                            error = if (LumaError.GPIO_OUT_OF_RANGE in state.errors) {
                                stringResource(
                                    R.string.feature_settings_impl_gpio_error,
                                    LumaLimits.MIN_GPIO,
                                    LumaLimits.MAX_GPIO,
                                )
                            } else {
                                null
                            },
                            leadingIcon = LumaIcons.Pin,
                        )
                        LumaNumberField(
                            value = state.draft.activeLeds,
                            onValueChange = viewModel::onActiveLedsChange,
                            label = stringResource(R.string.feature_settings_impl_strip_leds),
                            error = when {
                                LumaError.LED_COUNT_OUT_OF_RANGE in state.errors ->
                                    stringResource(
                                        R.string.feature_settings_impl_leds_error,
                                        state.maxLeds,
                                    )

                                LumaError.LED_COUNT_EXCEEDS_PIXEL_LIMIT in state.errors ->
                                    stringResource(
                                        R.string.feature_settings_impl_leds_exceed_error,
                                        state.maxLeds,
                                    )

                                else -> null
                            },
                            supportingText = stringResource(
                                R.string.feature_settings_impl_strip_leds_hint,
                                state.maxLeds,
                            ),
                            leadingIcon = LumaIcons.Grid,
                        )
                    }
                }

                item(key = "mode") {
                    LumaSectionCard(
                        title = stringResource(R.string.feature_settings_impl_strip_mode),
                        icon = LumaIcons.Palette,
                    ) {
                        LumaDropdown(
                            label = stringResource(R.string.feature_settings_impl_strip_mode),
                            options = state.modes,
                            selected = state.modes.firstOrNull { it.id == state.draft.modeId },
                            onSelected = { mode -> viewModel.onModeChange(mode.id) },
                            optionLabel = { mode -> mode.name },
                            selectedIcon = LumaIcons.Palette,
                            optionIcon = { LumaIcons.Palette },
                            optionSupportingText = { mode ->
                                if (mode.isGradient) "${mode.stopCount} stops" else "Solid"
                            },
                            emptyText = stringResource(R.string.feature_settings_impl_strip_mode_none),
                        )
                        state.modes.firstOrNull { it.id == state.draft.modeId }?.let { mode ->
                            LumaModePreview(
                                ramp = mode.ramp,
                                animation = mode.animation,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        if (state.draft.modeId != null) {
                            LumaTextButton(onClick = { viewModel.onModeChange(null) }) {
                                Text(text = stringResource(R.string.feature_settings_impl_strip_mode_none))
                            }
                        }
                    }
                }

                item(key = "brightness") {
                    LumaSectionCard(
                        title = stringResource(R.string.feature_settings_impl_strip_brightness),
                        icon = LumaIcons.Brightness,
                        subtitle = stringResource(
                            R.string.feature_settings_impl_strip_brightness_hint,
                            (LumaLimits.MAX_BRIGHTNESS * 100).roundToInt(),
                        ),
                    ) {
                        LumaBrightnessSlider(
                            brightness = state.draft.brightness,
                            onBrightnessChange = viewModel::onBrightnessChange,
                        )
                    }
                }

                if (!state.draft.isNew) {
                    item(key = "delete") {
                        LumaTextButton(
                            onClick = { showDeleteConfirm = true },
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
                                text = stringResource(R.string.feature_settings_impl_strip_delete),
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
                            text = stringResource(R.string.feature_settings_impl_unsaved),
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
                        Text(text = stringResource(R.string.feature_settings_impl_strip_save))
                    }
                }
            }
        }
    }
}
