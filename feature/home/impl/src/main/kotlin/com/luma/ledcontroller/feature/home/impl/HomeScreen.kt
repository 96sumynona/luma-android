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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luma.ledcontroller.core.designsystem.component.LumaBrightnessSlider
import com.luma.ledcontroller.core.designsystem.component.LumaCard
import com.luma.ledcontroller.core.designsystem.component.LumaEmptyState
import com.luma.ledcontroller.core.designsystem.component.LumaInfoBanner
import com.luma.ledcontroller.core.designsystem.component.LumaModePreview
import com.luma.ledcontroller.core.designsystem.component.LumaTag
import com.luma.ledcontroller.core.designsystem.icon.LumaIcons
import com.luma.ledcontroller.core.model.LumaMode
import com.luma.ledcontroller.core.ui.LumaScreenEnter
import com.luma.ledcontroller.core.ui.formatNextScheduledChange
import kotlinx.datetime.LocalDateTime

/**
 * Home shows every configured strip: its mode, a colour preview, its brightness and the next time a
 * schedule will change it.
 *
 * Brightness is the only control here that writes immediately; the colour preview never dims with
 * it, so the mode is always visible at full strength.
 */
@Composable
fun HomeScreen(
    onStripClick: (stripId: String) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LumaScreenEnter(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "banner") {
                LumaInfoBanner(
                    icon = LumaIcons.Bluetooth,
                    title = stringResource(R.string.feature_home_impl_shell_title),
                    message = stringResource(R.string.feature_home_impl_shell_message),
                )
            }

            when (val state = uiState) {
                HomeUiState.Loading -> Unit

                is HomeUiState.Success -> {
                    if (state.strips.isEmpty()) {
                        item(key = "empty") {
                            LumaEmptyState(
                                icon = LumaIcons.Lightbulb,
                                title = stringResource(R.string.feature_home_impl_empty_title),
                                message = stringResource(R.string.feature_home_impl_empty_message),
                                actionLabel = stringResource(R.string.feature_home_impl_empty_action),
                                onAction = onOpenSettings,
                            )
                        }
                    } else {
                        items(items = state.strips, key = { it.strip.id }) { card ->
                            StripSummaryCard(
                                card = card,
                                now = state.now,
                                onStripClick = { onStripClick(card.strip.id) },
                                onBrightnessChange = { brightness ->
                                    viewModel.onBrightnessChanged(card.strip.id, brightness)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StripSummaryCard(
    card: StripCard,
    now: LocalDateTime,
    onStripClick: () -> Unit,
    onBrightnessChange: (Float) -> Unit,
) {
    val strip = card.strip
    LumaCard(onClick = onStripClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = strip.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = stringResource(
                        R.string.feature_home_impl_gpio_leds,
                        strip.gpio,
                        strip.activeLeds,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = LumaIcons.ChevronRight,
                contentDescription = stringResource(R.string.feature_home_impl_open_strip),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }

        ModePreviewArea(mode = card.mode)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LumaTag(
                text = card.mode?.name ?: stringResource(R.string.feature_home_impl_no_mode),
                leadingIcon = LumaIcons.Palette,
            )
            LumaTag(text = card.board?.name ?: stringResource(R.string.feature_home_impl_board_label))
        }

        LumaBrightnessSlider(
            brightness = strip.brightness,
            onBrightnessChange = onBrightnessChange,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = LumaIcons.Schedule,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = stringResource(R.string.feature_home_impl_next_change),
                style = MaterialTheme.typography.labelLarge,
            )
            Box(modifier = Modifier.weight(1f))
            val nextChange = card.nextChange
            Text(
                text = if (nextChange == null) {
                    stringResource(R.string.feature_home_impl_no_next_change)
                } else {
                    stringResource(
                        R.string.feature_home_impl_next_change_value,
                        formatNextScheduledChange(nextChange.nextRun, now),
                        nextChange.schedule.name,
                    )
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** The colour preview: full-strength ramp preview, or an explicit placeholder when no mode is set. */
@Composable
private fun ModePreviewArea(mode: LumaMode?) {
    if (mode == null) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp),
            shape = RoundedCornerShape(percent = 50),
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.feature_home_impl_no_mode),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    } else {
        LumaModePreview(
            ramp = mode.ramp,
            animation = mode.animation,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
        )
    }
}
