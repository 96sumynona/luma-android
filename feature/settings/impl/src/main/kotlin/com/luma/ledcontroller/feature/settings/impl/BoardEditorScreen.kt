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

import androidx.compose.foundation.clickable
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
import com.luma.ledcontroller.core.designsystem.component.LumaButton
import com.luma.ledcontroller.core.designsystem.component.LumaConfirmDialog
import com.luma.ledcontroller.core.designsystem.component.LumaDropdown
import com.luma.ledcontroller.core.designsystem.component.LumaNumberField
import com.luma.ledcontroller.core.designsystem.component.LumaSectionCard
import com.luma.ledcontroller.core.designsystem.component.LumaSettingRow
import com.luma.ledcontroller.core.designsystem.component.LumaTag
import com.luma.ledcontroller.core.designsystem.component.LumaTextButton
import com.luma.ledcontroller.core.designsystem.component.LumaTextField
import com.luma.ledcontroller.core.designsystem.icon.LumaIcons
import com.luma.ledcontroller.core.model.LedLayout
import com.luma.ledcontroller.core.model.LumaError
import com.luma.ledcontroller.core.model.LumaLimits
import com.luma.ledcontroller.core.ui.LeaveGuardEffect
import com.luma.ledcontroller.core.ui.LocalSnackbarHostState
import com.luma.ledcontroller.core.ui.LumaScreenEnter
import com.luma.ledcontroller.core.ui.UnsavedChangesDialog

/**
 * Board configuration: the board's own limits, plus the strips wired to it.
 *
 * The strips are listed inside the board card on purpose — a strip never exists without a board.
 */
@Composable
fun BoardEditorScreen(
    boardId: String?,
    onStripClick: (stripId: String, boardId: String) -> Unit,
    onAddStrip: (boardId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BoardEditorViewModel = hiltViewModel(),
) {
    LaunchedEffect(boardId) { viewModel.load(boardId) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = LocalSnackbarHostState.current
    val savedMessage = stringResource(R.string.feature_settings_impl_board_saved)

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
            title = stringResource(R.string.feature_settings_impl_board_delete_title),
            message = stringResource(R.string.feature_settings_impl_board_delete_message),
            confirmLabel = stringResource(R.string.feature_settings_impl_delete),
            dismissLabel = stringResource(R.string.feature_settings_impl_cancel),
            destructive = true,
            onConfirm = {
                showDeleteConfirm = false
                viewModel.onDeleteBoard()
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
                        title = stringResource(R.string.feature_settings_impl_board_title),
                        icon = LumaIcons.Memory,
                    ) {
                        LumaTextField(
                            value = state.draft.name,
                            onValueChange = viewModel::onNameChange,
                            label = stringResource(R.string.feature_settings_impl_board_name),
                            error = if (LumaError.NAME_BLANK in state.errors) {
                                stringResource(R.string.feature_settings_impl_name_error)
                            } else {
                                null
                            },
                            placeholder = stringResource(R.string.feature_settings_impl_board_name_placeholder),
                        )
                        LumaNumberField(
                            value = state.draft.pixelLimit,
                            onValueChange = viewModel::onPixelLimitChange,
                            label = stringResource(R.string.feature_settings_impl_board_pixel_limit),
                            error = if (LumaError.PIXEL_LIMIT_OUT_OF_RANGE in state.errors) {
                                stringResource(
                                    R.string.feature_settings_impl_pixel_limit_error,
                                    LumaLimits.MAX_LEDS,
                                )
                            } else {
                                null
                            },
                            supportingText = stringResource(
                                R.string.feature_settings_impl_board_pixel_limit_hint,
                                LumaLimits.MAX_LEDS,
                            ),
                            leadingIcon = LumaIcons.Grid,
                        )
                        LumaNumberField(
                            value = state.draft.bufferSizeBytes,
                            onValueChange = viewModel::onBufferSizeChange,
                            label = stringResource(R.string.feature_settings_impl_board_buffer),
                            error = if (LumaError.BUFFER_OUT_OF_RANGE in state.errors) {
                                stringResource(
                                    R.string.feature_settings_impl_buffer_error,
                                    LumaLimits.MAX_BUFFER_SIZE,
                                )
                            } else {
                                null
                            },
                            leadingIcon = LumaIcons.Storage,
                        )
                        LumaDropdown(
                            label = stringResource(R.string.feature_settings_impl_board_layout),
                            options = LedLayout.entries.toList(),
                            selected = state.draft.layout,
                            onSelected = viewModel::onLayoutChange,
                            optionLabel = { layout -> layout.label() },
                            selectedIcon = LumaIcons.LinearScale,
                            optionIcon = { LumaIcons.LinearScale },
                        )
                    }
                }

                item(key = "strips") {
                    LumaSectionCard(
                        title = stringResource(R.string.feature_settings_impl_strips_title),
                        icon = LumaIcons.Lightbulb,
                        subtitle = stringResource(R.string.feature_settings_impl_boards_hint),
                    ) {
                        if (state.strips.isEmpty()) {
                            LumaTag(
                                text = stringResource(R.string.feature_settings_impl_no_strips),
                                leadingIcon = LumaIcons.Lightbulb,
                            )
                        }
                        val currentBoardId = state.draft.boardId
                        state.strips.forEach { strip ->
                            LumaSettingRow(
                                title = strip.name,
                                subtitle = stringResource(
                                    R.string.feature_settings_impl_strip_summary,
                                    strip.gpio,
                                    strip.activeLeds,
                                ),
                                icon = LumaIcons.Lightbulb,
                                value = "${strip.brightnessPercent}%",
                                modifier = Modifier.clickable {
                                    if (currentBoardId != null) {
                                        onStripClick(strip.id, currentBoardId)
                                    }
                                },
                            )
                        }
                        if (currentBoardId == null) {
                            Text(
                                text = stringResource(R.string.feature_settings_impl_strip_empty_action),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            LumaTextButton(onClick = { onAddStrip(currentBoardId) }) {
                                Icon(
                                    imageVector = LumaIcons.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = stringResource(R.string.feature_settings_impl_add_strip))
                            }
                        }
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
                                text = stringResource(R.string.feature_settings_impl_board_delete),
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
                        Text(text = stringResource(R.string.feature_settings_impl_board_save))
                    }
                }
            }
        }
    }
}
