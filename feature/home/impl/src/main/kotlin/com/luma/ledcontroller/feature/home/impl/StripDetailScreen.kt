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

package com.luma.ledcontroller.feature.home.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luma.ledcontroller.core.designsystem.component.LumaBrightnessSlider
import com.luma.ledcontroller.core.designsystem.component.LumaCard
import com.luma.ledcontroller.core.designsystem.component.LumaDropdown
import com.luma.ledcontroller.core.designsystem.component.LumaModePreview
import com.luma.ledcontroller.core.designsystem.component.LumaSectionCard
import com.luma.ledcontroller.core.designsystem.component.LumaSettingRow
import com.luma.ledcontroller.core.designsystem.component.LumaTag
import com.luma.ledcontroller.core.designsystem.icon.LumaIcons
import com.luma.ledcontroller.core.model.LumaSchedule
import com.luma.ledcontroller.core.ui.LumaScreenEnter
import com.luma.ledcontroller.core.ui.formatRepeatDays
import com.luma.ledcontroller.core.ui.formatTransition

/**
 * One strip in detail: which board it belongs to, which reusable mode it points at, its brightness
 * and the schedules that will change it.
 *
 * Mode assignment writes immediately — a mode is shared, so this screen never edits mode content.
 */
@Composable
fun StripDetailScreen(
    stripId: String,
    modifier: Modifier = Modifier,
    viewModel: StripDetailViewModel = hiltViewModel(),
) {
    LaunchedEffect(stripId) { viewModel.load(stripId) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LumaScreenEnter(modifier = modifier.fillMaxSize()) {
        when (val state = uiState) {
            StripDetailUiState.Loading -> Unit

            StripDetailUiState.NotFound -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
            ) {
                item {
                    Text(
                        text = stringResource(R.string.feature_home_impl_strip_not_found),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }

            is StripDetailUiState.Success -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "header") {
                    LumaSectionCard(
                        title = state.strip.name,
                        subtitle = state.board?.name,
                        icon = LumaIcons.Lightbulb,
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            LumaTag(
                                text = stringResource(
                                    R.string.feature_home_impl_gpio_leds,
                                    state.strip.gpio,
                                    state.strip.activeLeds,
                                ),
                                leadingIcon = LumaIcons.Pin,
                            )
                            LumaTag(
                                text = stringResource(
                                    R.string.feature_home_impl_board_label,
                                ) + " " + (state.board?.name ?: "–"),
                                leadingIcon = LumaIcons.Memory,
                            )
                        }
                        LumaSettingRow(
                            title = stringResource(R.string.feature_home_impl_active_leds),
                            value = state.board?.let { "${state.strip.activeLeds} / ${it.effectivePixelLimit}" }
                                ?: state.strip.activeLeds.toString(),
                            icon = LumaIcons.Grid,
                        )
                    }
                }

                item(key = "mode") {
                    LumaSectionCard(
                        title = stringResource(R.string.feature_home_impl_assign_mode),
                        icon = LumaIcons.Palette,
                        subtitle = stringResource(R.string.feature_home_impl_assign_mode_hint),
                    ) {
                        state.mode?.let { mode ->
                            LumaModePreview(
                                ramp = mode.ramp,
                                animation = mode.animation,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        LumaDropdown(
                            label = stringResource(R.string.feature_home_impl_mode_label),
                            options = state.modes,
                            selected = state.mode,
                            onSelected = { mode -> viewModel.onModeSelected(mode.id) },
                            optionLabel = { it.name },
                            selectedIcon = LumaIcons.Palette,
                            optionIcon = { LumaIcons.Palette },
                            optionSupportingText = { mode ->
                                "${mode.ramp.stops.size} stops · ${mode.animation.kind.name.lowercase()}"
                            },
                            emptyText = stringResource(R.string.feature_home_impl_no_mode),
                        )
                    }
                }

                item(key = "brightness") {
                    LumaCard {
                        LumaBrightnessSlider(
                            brightness = state.strip.brightness,
                            onBrightnessChange = viewModel::onBrightnessChanged,
                        )
                        Text(
                            text = stringResource(R.string.feature_home_impl_brightness_saved_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                item(key = "schedules") {
                    LumaSectionCard(
                        title = stringResource(R.string.feature_home_impl_schedules_title),
                        icon = LumaIcons.Schedule,
                    ) {
                        if (state.schedules.isEmpty()) {
                            Text(
                                text = stringResource(R.string.feature_home_impl_no_schedules),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            state.schedules.forEach { schedule ->
                                ScheduleRow(schedule = schedule, modeName = state.mode?.name)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScheduleRow(schedule: LumaSchedule, modeName: String?) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = LumaIcons.Timer,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
            Text(text = schedule.timeLabel, style = MaterialTheme.typography.titleSmall)
            Text(
                text = formatRepeatDays(schedule.repeatDays),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = stringResource(
                R.string.feature_home_impl_schedule_summary,
                schedule.name.ifBlank { modeName.orEmpty() },
                formatTransition(schedule.transitionSeconds),
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
