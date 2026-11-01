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

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.luma.ledcontroller.core.designsystem.component.LumaTag
import com.luma.ledcontroller.core.designsystem.icon.LumaIcons
import com.luma.ledcontroller.core.ui.LumaScreenEnter
import com.luma.ledcontroller.core.ui.formatRepeatDays
import com.luma.ledcontroller.core.ui.formatTransition

/**
 * The schedule library.
 *
 * Each card spells out the relationship a schedule makes: a strip, a mode, a time and the days it
 * repeats. Nothing here executes: schedules are stored for the controller.
 */
@Composable
fun SchedulesScreen(
    onScheduleClick: (scheduleId: String) -> Unit,
    onCreateSchedule: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SchedulesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<ScheduleSummary?>(null) }

    pendingDelete?.let { summary ->
        LumaConfirmDialog(
            title = stringResource(R.string.feature_schedules_impl_delete_title),
            message = stringResource(R.string.feature_schedules_impl_delete_message),
            confirmLabel = stringResource(R.string.feature_schedules_impl_delete),
            dismissLabel = stringResource(R.string.feature_schedules_impl_cancel),
            destructive = true,
            onConfirm = {
                viewModel.deleteSchedule(summary.schedule.id)
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
                        icon = LumaIcons.Schedule,
                        title = stringResource(R.string.feature_schedules_impl_hint_title),
                        message = stringResource(R.string.feature_schedules_impl_hint),
                    )
                }

                if (uiState.schedules.isEmpty() && !uiState.isLoading) {
                    item(key = "empty") {
                        LumaEmptyState(
                            icon = LumaIcons.Schedule,
                            title = stringResource(R.string.feature_schedules_impl_empty_title),
                            message = stringResource(R.string.feature_schedules_impl_empty_message),
                            actionLabel = if (uiState.hasStrips && uiState.hasModes) {
                                stringResource(R.string.feature_schedules_impl_new)
                            } else {
                                stringResource(R.string.feature_schedules_impl_create_first)
                            },
                            onAction = if (uiState.hasStrips && uiState.hasModes) {
                                onCreateSchedule
                            } else {
                                null
                            },
                        )
                    }
                }

                items(items = uiState.schedules, key = { it.schedule.id }) { summary ->
                    ScheduleCard(
                        summary = summary,
                        onClick = { onScheduleClick(summary.schedule.id) },
                        onEnabledChange = { enabled ->
                            viewModel.setEnabled(summary.schedule.id, enabled)
                        },
                        onDelete = { pendingDelete = summary },
                    )
                }
            }

            ExtendedFloatingActionButton(
                onClick = onCreateSchedule,
                icon = { Icon(imageVector = LumaIcons.Add, contentDescription = null) },
                text = { Text(text = stringResource(R.string.feature_schedules_impl_new)) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
            )
        }
    }
}

@Composable
private fun ScheduleCard(
    summary: ScheduleSummary,
    onClick: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    val schedule = summary.schedule
    var menuExpanded by remember { mutableStateOf(false) }
    // The card dims as a schedule is switched off, which makes the enabled state readable at a glance.
    val titleColor by animateColorAsState(
        targetValue = if (schedule.enabled) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "scheduleTitle",
    )

    LumaCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = schedule.timeLabel,
                    style = MaterialTheme.typography.headlineSmall,
                    color = titleColor,
                )
                Text(
                    text = formatRepeatDays(schedule.repeatDays),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = schedule.enabled, onCheckedChange = onEnabledChange)
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = LumaIcons.MoreVert,
                        contentDescription = stringResource(R.string.feature_schedules_impl_edit),
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(text = stringResource(R.string.feature_schedules_impl_edit)) },
                        leadingIcon = { Icon(imageVector = LumaIcons.Edit, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onClick()
                        },
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(R.string.feature_schedules_impl_delete),
                                color = MaterialTheme.colorScheme.error,
                            )
                        },
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LumaTag(
                text = summary.stripName ?: stringResource(R.string.feature_schedules_impl_strip_missing),
                leadingIcon = LumaIcons.Memory,
            )
            Icon(
                imageVector = LumaIcons.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
            LumaTag(
                text = summary.modeName ?: stringResource(R.string.feature_schedules_impl_mode_missing),
                leadingIcon = LumaIcons.Palette,
            )
        }

        Text(
            text = stringResource(R.string.feature_schedules_impl_transition) + " · " +
                formatTransition(schedule.transitionSeconds),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
