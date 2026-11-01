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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.luma.ledcontroller.core.designsystem.R
import com.luma.ledcontroller.core.designsystem.theme.LumaTheme
import com.luma.ledcontroller.core.model.ColorMath

/**
 * Colour picker for a ramp stop.
 *
 * Three channels (hue, saturation, value) plus a hex field. Every change is reported immediately so
 * the ramp preview behind the dialog updates while the user drags — the stop is never left in an
 * intermediate state.
 *
 * @param initialColor packed ARGB colour to start from.
 * @param onColorChange called on every change.
 * @param onDismiss called when the dialog is closed (the change is already applied).
 */
@Composable
fun LumaColorPickerDialog(
    initialColor: Long,
    onColorChange: (Long) -> Unit,
    onDismiss: () -> Unit,
    title: String = "Stop colour",
) {
    val initialHsv = remember(initialColor) { initialColor.toColorInt().toHsv() }
    var hue by remember(initialColor) { mutableStateOf(initialHsv[0]) }
    var saturation by remember(initialColor) { mutableStateOf(initialHsv[1]) }
    var value by remember(initialColor) { mutableStateOf(initialHsv[2]) }
    var hexText by remember(initialColor) { mutableStateOf(ColorMath.toHex(initialColor)) }

    val currentColor = android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value))
    val composeColor = Color(currentColor)

    fun emitColor() {
        val argb = android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value))
        hexText = ColorMath.toHex(argb.toLong() and 0xFFFFFFFFL)
        onColorChange(argb.toLong() and 0xFFFFFFFFL)
    }

    LumaActionDialog(
        title = title,
        onDismissRequest = onDismiss,
        message = "Adjust the stop's colour. The ramp preview updates as you go.",
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(composeColor),
        )

        ChannelSlider(
            label = "Hue",
            value = hue,
            valueRange = 0f..360f,
            brush = Brush.horizontalGradient(
                listOf(
                    Color(android.graphics.Color.HSVToColor(floatArrayOf(0f, 1f, 1f))),
                    Color(android.graphics.Color.HSVToColor(floatArrayOf(60f, 1f, 1f))),
                    Color(android.graphics.Color.HSVToColor(floatArrayOf(120f, 1f, 1f))),
                    Color(android.graphics.Color.HSVToColor(floatArrayOf(180f, 1f, 1f))),
                    Color(android.graphics.Color.HSVToColor(floatArrayOf(240f, 1f, 1f))),
                    Color(android.graphics.Color.HSVToColor(floatArrayOf(300f, 1f, 1f))),
                    Color(android.graphics.Color.HSVToColor(floatArrayOf(360f, 1f, 1f))),
                ),
            ),
        ) { newValue ->
            hue = newValue
            emitColor()
        }

        ChannelSlider(
            label = "Saturation",
            value = saturation,
            valueRange = 0f..1f,
            brush = Brush.horizontalGradient(
                listOf(
                    Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0f, value))),
                    Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, value))),
                ),
            ),
        ) { newValue ->
            saturation = newValue
            emitColor()
        }

        ChannelSlider(
            label = "Value",
            value = value,
            valueRange = 0f..1f,
            brush = Brush.horizontalGradient(
                listOf(Color.Black, Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, 1f)))),
            ),
        ) { newValue ->
            value = newValue
            emitColor()
        }

        LumaTextField(
            value = hexText,
            onValueChange = { text ->
                hexText = text
                val parsed = ColorMath.fromHex(text)
                if (parsed != null) {
                    val hsv = parsed.toColorInt().toHsv()
                    hue = hsv[0]
                    saturation = hsv[1]
                    value = hsv[2]
                    onColorChange(parsed)
                }
            },
            label = stringResource(R.string.core_designsystem_hex_label),
            supportingText = stringResource(R.string.core_designsystem_hex_supporting),
            keyboardType = KeyboardType.Ascii,
            error = if (ColorMath.fromHex(hexText) == null) {
                stringResource(R.string.core_designsystem_hex_error)
            } else {
                null
            },
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LumaButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.core_designsystem_color_done))
            }
        }
    }
}

@Composable
private fun ChannelSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    brush: Brush,
    onValueChange: (Float) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(text = "${(value * 100).toInt()}", style = MaterialTheme.typography.labelSmall)
        }
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .padding(top = 12.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(brush),
            )
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                colors = SliderDefaults.colors(
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent,
                    thumbColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        }
    }
}

private fun Long.toColorInt(): Int = this.toInt()

private fun Int.toHsv(): FloatArray {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(this, hsv)
    return hsv
}

@ThemePreviews
@Composable
fun ColorPickerPreview() {
    LumaTheme {
        LumaColorPickerDialog(
            initialColor = 0xFFFF5EB2,
            onColorChange = {},
            onDismiss = {},
        )
    }
}
