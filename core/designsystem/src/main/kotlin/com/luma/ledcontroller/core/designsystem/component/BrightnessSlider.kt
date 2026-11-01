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

package com.luma.ledcontroller.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.luma.ledcontroller.core.designsystem.R
import com.luma.ledcontroller.core.designsystem.icon.LumaIcons
import com.luma.ledcontroller.core.designsystem.theme.LumaTheme
import com.luma.ledcontroller.core.model.BrightnessCurve
import com.luma.ledcontroller.core.model.LumaLimits

/**
 * Brightness control with Luma's exponential slider scale.
 *
 * The slider position and the stored brightness are different scales: the slider is heavily
 * exponential so the dim end, where LED changes are actually visible, gets most of the travel. The
 * stored value is never allowed above the 80% ceiling.
 *
 * The control deliberately does not touch any preview: it reports a brightness up to its caller,
 * and previews always render at full ramp strength.
 */
@Composable
fun LumaBrightnessSlider(
    brightness: Float,
    onBrightnessChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    showCaption: Boolean = true,
) {
    var sliderPosition by remember(brightness) {
        mutableFloatStateOf(BrightnessCurve.sliderForBrightness(brightness))
    }
    val percent = BrightnessCurve.percentOf(BrightnessCurve.brightnessForSlider(sliderPosition))

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = LumaIcons.Brightness,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = stringResource(R.string.core_designsystem_brightness),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            Text(
                text = stringResource(R.string.core_designsystem_brightness_percent, percent),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = sliderPosition,
            onValueChange = { newPosition ->
                sliderPosition = newPosition
                onBrightnessChange(BrightnessCurve.brightnessForSlider(newPosition))
            },
            modifier = Modifier.semantics {
                contentDescription = stringResource(
                    R.string.core_designsystem_brightness_a11y,
                    (LumaLimits.MAX_BRIGHTNESS * 100).toInt(),
                )
            },
            enabled = enabled,
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        )
        if (showCaption) {
            Text(
                text = stringResource(
                    R.string.core_designsystem_brightness_caption,
                    (LumaLimits.MAX_BRIGHTNESS * 100).toInt(),
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@ThemePreviews
@Composable
fun BrightnessSliderPreview() {
    LumaTheme {
        LumaBrightnessSlider(brightness = 0.4f, onBrightnessChange = {})
    }
}
