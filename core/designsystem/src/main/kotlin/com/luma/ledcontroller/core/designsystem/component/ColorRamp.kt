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

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luma.ledcontroller.core.designsystem.R
import com.luma.ledcontroller.core.designsystem.theme.LumaTheme
import com.luma.ledcontroller.core.model.ColorRamp
import com.luma.ledcontroller.core.model.ColorStop
import kotlin.math.abs

/** Converts a packed ARGB [Long] from the model into a Compose colour. */
fun Long.toComposeColor(): Color = Color(this.toInt())

/** Converts a Compose colour into the packed ARGB [Long] the model and the controller use. */
fun Color.toArgbLong(): Long = this.toArgb().toLong() and 0xFFFFFFFFL

/**
 * The ramp rendered as a continuous gradient.
 *
 * Stop positions and colours animate, so when a stop is dragged or recoloured the change reads as a
 * movement rather than a jump. Brightness is never applied here: a ramp preview always shows the
 * full-strength colours, even when a strip is dimmed.
 */
@Composable
fun LumaColorRampBar(
    ramp: ColorRamp,
    modifier: Modifier = Modifier,
    height: Dp = 14.dp,
) {
    val rampPreviewDescription = stringResource(R.string.core_designsystem_ramp_preview)
    val animatedStops = rememberAnimatedStops(ramp)
    val colors = remember(animatedStops) {
        animatedStops
            .sortedBy { it.position }
            .let { sorted ->
                ColorRamp(sorted.map { ColorStop(it.id, it.position, it.color.toArgbLong()) })
            }
            .colors(SAMPLE_COUNT)
            .map { it.toComposeColor() }
    }

    Canvas(
        modifier = modifier
            .height(height)
            .semantics {
                contentDescription = rampPreviewDescription
            },
    ) {
        val corner = CornerRadius(size.height / 2f, size.height / 2f)
        drawRoundRect(
            brush = Brush.horizontalGradient(colors),
            size = Size(size.width, size.height),
            cornerRadius = corner,
        )
    }
}

/**
 * The interactive ramp editor.
 *
 * Every stop is a draggable handle, including the two endpoints: endpoints are not pinned to 0 and
 * 100, and the nearest endpoint's colour extends to the edge of the ramp so there is never an
 * undefined section. Dragging is continuous and the model keeps a minimum gap between stops.
 *
 * @param ramp the ramp being edited.
 * @param selectedStopId the stop the colour picker acts on.
 * @param onStopSelected invoked when the user taps a handle.
 * @param onStopMoved invoked continuously while dragging, and once on release.
 */
@Composable
fun LumaRampEditor(
    ramp: ColorRamp,
    selectedStopId: String?,
    onStopSelected: (String) -> Unit,
    onStopMoved: (stopId: String, position: Float) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 96.dp,
) {
    val editorDescription = stringResource(
        R.string.core_designsystem_ramp_editor_a11y,
        ramp.stops.size,
    )
    val animatedStops = rememberAnimatedStops(ramp)
    val sorted = animatedStops.sortedBy { it.position }
    val colors = remember(sorted) {
        ColorRamp(sorted.map { ColorStop(it.id, it.position, it.color.toArgbLong()) })
            .colors(SAMPLE_COUNT)
            .map { it.toComposeColor() }
    }
    val handlePositions = ramp.stops.associate { stop -> stop.id to stop.position }
    val currentSelectedId = selectedStopId

    Box(
        modifier = modifier
            .height(height)
            .semantics {
                contentDescription = editorDescription
            },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val trackHeight = 22.dp.toPx()
            val trackTop = (size.height - trackHeight) / 2f
            drawRoundRect(
                brush = Brush.horizontalGradient(colors),
                topLeft = Offset(0f, trackTop),
                size = Size(size.width, trackHeight),
                cornerRadius = CornerRadius(trackHeight / 2f, trackHeight / 2f),
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.16f),
                topLeft = Offset(0f, trackTop),
                size = Size(size.width, trackHeight),
                cornerRadius = CornerRadius(trackHeight / 2f, trackHeight / 2f),
                style = Stroke(width = 1.dp.toPx()),
            )

            ramp.stops.forEach { stop ->
                val x = (stop.position / 100f) * size.width
                val isSelected = stop.id == currentSelectedId
                val radius = if (isSelected) 13.dp.toPx() else 10.dp.toPx()
                val centre = Offset(x, size.height / 2f)
                drawCircle(color = Color.Black.copy(alpha = 0.55f), radius = radius + 3.dp.toPx(), center = centre)
                drawCircle(color = stop.color.toComposeColor(), radius = radius, center = centre)
                drawCircle(
                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.55f),
                    radius = radius,
                    center = centre,
                    style = Stroke(width = if (isSelected) 3.dp.toPx() else 2.dp.toPx()),
                )
            }
        }

        // Tap selects the nearest handle; dragging from a handle moves it. Both are handled in one
        // gesture loop so a tap can never be swallowed by the drag detector (or the other way round).
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(handlePositions, currentSelectedId) {
                    val touchRadiusPx = 28.dp.toPx()
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val nearest = handlePositions.minByOrNull { (_, position) ->
                            abs((position / 100f) * size.width - down.position.x)
                        } ?: return@awaitEachGesture

                        onStopSelected(nearest.key)
                        val nearestX = (nearest.value / 100f) * size.width
                        if (abs(nearestX - down.position.x) > touchRadiusPx) {
                            // The user tapped the ramp rather than a handle: select only.
                            return@awaitEachGesture
                        }

                        onStopMoved(
                            nearest.key,
                            (down.position.x / size.width * 100f).coerceIn(0f, 100f),
                        )
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (change.changedToUpIgnoreConsumed()) break
                            onStopMoved(
                                nearest.key,
                                (change.position.x / size.width * 100f).coerceIn(0f, 100f),
                            )
                            change.consume()
                        }
                    }
                },
        )
    }
}

private const val SAMPLE_COUNT = 48

/**
 * Animates each stop by identity, so a stop keeps its animation when the list reorders.
 */
@Composable
private fun rememberAnimatedStops(ramp: ColorRamp): List<AnimatedStop> =
    ramp.sortedStops.map { stop ->
        key(stop.id) {
            val position by animateFloatAsState(
                targetValue = stop.position,
                label = "stop position",
            )
            val color by animateColorAsState(
                targetValue = stop.color.toComposeColor(),
                label = "stop colour",
            )
            AnimatedStop(id = stop.id, position = position, color = color)
        }
    }

private data class AnimatedStop(val id: String, val position: Float, val color: Color)

@ThemePreviews
@Composable
private fun ColorRampPreview() {
    LumaTheme {
        LumaColorRampBar(ramp = ColorRamp.default(), modifier = Modifier)
    }
}
