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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.luma.ledcontroller.core.designsystem.component.LumaCard
import com.luma.ledcontroller.core.designsystem.component.LumaConfirmDialog
import com.luma.ledcontroller.core.designsystem.component.LumaEmptyState
import com.luma.ledcontroller.core.designsystem.component.LumaInfoBanner
import com.luma.ledcontroller.core.designsystem.component.LumaModePreview
import com.luma.ledcontroller.core.designsystem.component.LumaTag
import com.luma.ledcontroller.core.designsystem.icon.LumaIcons
import com.luma.ledcontroller.core.model.LumaMode
import com.luma.ledcontroller.core.ui.LocalSnackbarHostState
import com.luma.ledcontroller.core.ui.LumaScreenEnter

/**
 * The mode library.
 *
 * Modes are their own data objects here: nothing on this screen edits a strip, it only edits named,
 * reusable modes that strips and schedules point at.
 */
@Composable
fun ModesScreen(
    onModeClick: (modeId: String) -> Unit,
    onCreateMode: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ModesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = LocalSnackbarHostState.current
    var pendingDelete by remember { mutableStateOf<LumaMode?>(null) }

    val duplicatedMessage = stringResource(R.string.feature_modes_impl_duplicate_done)
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is ModesEvent.Duplicated -> snackbarHostState.showSnackbar(duplicatedMessage)
            }
        }
    }

    pendingDelete?.let { mode ->
        LumaConfirmDialog(
            title = stringResource(R.string.feature_modes_impl_delete_title, mode.name),
            message = stringResource(R.string.feature_modes_impl_delete_message),
            confirmLabel = stringResource(R.string.feature_modes_impl_delete_confirm),
            dismissLabel = stringResource(R.string.feature_modes_impl_cancel),
            destructive = true,
            onConfirm = {
                viewModel.deleteMode(mode.id)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }

    LumaScreenEnter(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "hint") {
                    LumaInfoBanner(
                        icon = LumaIcons.Palette,
                        title = stringResource(R.string.feature_modes_impl_library_title),
                        message = stringResource(R.string.feature_modes_impl_library_hint),
                    )
                }

                if (uiState.modes.isEmpty() && !uiState.isLoading) {
                    item(key = "empty") {
                        LumaEmptyState(
                            icon = LumaIcons.AutoAwesome,
                            title = stringResource(R.string.feature_modes_impl_empty_title),
                            message = stringResource(R.string.feature_modes_impl_empty_message),
                            actionLabel = stringResource(R.string.feature_modes_impl_new_mode),
                            onAction = onCreateMode,
                        )
                    }
                }

                items(items = uiState.modes, key = { it.mode.id }) { summary ->
                    ModeCard(
                        summary = summary,
                        onClick = { onModeClick(summary.mode.id) },
                        onDuplicate = { viewModel.duplicateMode(summary.mode) },
                        onDelete = { pendingDelete = summary.mode },
                    )
                }
            }

            ExtendedFloatingActionButton(
                onClick = onCreateMode,
                icon = { Icon(imageVector = LumaIcons.Add, contentDescription = null) },
                text = { Text(text = stringResource(R.string.feature_modes_impl_new_mode)) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
            )
        }
    }
}

@Composable
private fun ModeCard(
    summary: ModeSummary,
    onClick: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
) {
    val mode = summary.mode
    var menuExpanded by remember { mutableStateOf(false) }

    LumaCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = mode.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = mode.subtitle(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = LumaIcons.MoreVert,
                        contentDescription = stringResource(R.string.feature_modes_impl_edit),
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(text = stringResource(R.string.feature_modes_impl_duplicate)) },
                        leadingIcon = { Icon(imageVector = LumaIcons.ContentCopy, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onDuplicate()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(text = stringResource(R.string.feature_modes_impl_delete)) },
                        leadingIcon = {
                            Icon(
                                imageVector = LumaIcons.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        },
                    )
                }
            }
        }

        LumaModePreview(
            ramp = mode.ramp,
            animation = mode.animation,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LumaTag(
                text = if (mode.isGradient) {
                    stringResource(R.string.feature_modes_impl_stops, mode.stopCount)
                } else {
                    stringResource(R.string.feature_modes_impl_solid)
                },
                leadingIcon = LumaIcons.Palette,
            )
            LumaTag(
                text = mode.animation.kind.label(),
                leadingIcon = mode.animation.kind.icon(),
            )
            Icon(
                imageVector = LumaIcons.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = stringResource(R.string.feature_modes_impl_used_by, summary.stripCount),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LumaMode.subtitle(): String =
    if (isGradient) {
        stringResource(R.string.feature_modes_impl_stops, stopCount) + " · " + animation.kind.label()
    } else {
        stringResource(R.string.feature_modes_impl_solid) + " · " + animation.kind.label()
    }
