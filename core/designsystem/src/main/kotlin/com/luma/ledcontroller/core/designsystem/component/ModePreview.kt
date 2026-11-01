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

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luma.ledcontroller.core.designsystem.R
import com.luma.ledcontroller.core.designsystem.theme.LumaTheme
import com.luma.ledcontroller.core.model.AnimationKind
import com.luma.ledcontroller.core.model.ColorRamp
import com.luma.ledcontroller.core.model.ModeAnimation
import kotlin.math.PI
import kotlin.math.sin

/**
 * A pixel-strip preview of a mode.
 *
 * The strip is drawn as discrete pixels because that is what the product is, and each pixel samples
 * the mode's ramp. Motion is an overlay on top of a full-strength ramp rather than a change in
 * output level: a "breathe" mode shows a travelling highlight, it never dims the preview, so a
 * preview can never be confused with the strip's brightness setting.
 *
 * This is an in-app preview only. Nothing here is connected to hardware.
 */
@Composable
fun LumaModePreview(
    ramp: ColorRamp,
    animation: ModeAnimation,
    modifier: Modifier = Modifier,
    pixelCount: Int = 32,
    height: Dp = 24.dp,
) {
    val isPreview = LocalInspectionMode.current
    val durationMillis = animationDurationMillis(animation.speed)
    val infiniteTransition = rememberInfiniteTransition(label = "mode preview")

    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isPreview) 0f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "mode preview phase",
    )

    Canvas(
        modifier = modifier
            .height(height)
            .semantics {
                contentDescription = stringResource(
                    R.string.core_designsystem_mode_preview_a11y,
                    ramp.stops.size,
                )
            },
    ) {
        val count = pixelCount.coerceAtLeast(2)
        val gap = size.width / count * 0.18f
        val pixelWidth = (size.width / count) - gap
        val corner = CornerRadius(pixelWidth / 2.5f, pixelWidth / 2.5f)

        for (index in 0 until count) {
            val fraction = index.toFloat() / (count - 1)
            val rampPosition = when (animation.kind) {
                AnimationKind.CHASE -> {
                    val shift = phase * 100f * if (animation.reverse) -1f else 1f
                    ((fraction * 100f + shift) % 100f + 100f) % 100f
                }
                else -> fraction * 100f
            }
            val base = ramp.colorAt(rampPosition).toComposeColor()
            val highlight = highlightStrength(animation.kind, phase, index, count, animation.spread)
            val color = if (highlight <= 0f) base else blendTowardsWhite(base, highlight)

            drawRoundRect(
                color = color,
                topLeft = Offset(index * (pixelWidth + gap), 0f),
                size = Size(pixelWidth, size.height),
                cornerRadius = corner,
            )
        }
    }
}

/**
 * How strongly to light up a pixel as part of the animation overlay, `0f..1f`.
 *
 * Deterministic and index based so recomposition cannot re-roll the pattern.
 */
private fun highlightStrength(
    kind: AnimationKind,
    phase: Float,
    index: Int,
    count: Int,
    spread: Float,
): Float {
    val reach = 0.15f + spread * 0.85f
    return when (kind) {
        AnimationKind.SOLID -> 0f
        AnimationKind.BREATHE -> {
            val band = ((phase * (1f + reach)) % 1f)
            val distance = kotlin.math.abs(index.toFloat() / (count - 1) - band)
            0.28f * (1f - (distance / reach).coerceIn(0f, 1f))
        }
        AnimationKind.CHASE -> {
            val band = ((phase * (1f + reach)) % 1f)
            val distance = kotlin.math.abs(index.toFloat() / (count - 1) - band)
            0.22f * (1f - (distance / (reach * 0.5f)).coerceIn(0f, 1f))
        }
        AnimationKind.TWINKLE -> {
            val twinkle = sin((phase * 2 * PI) + index * 2.399).toFloat()
            if (twinkle > 0.82f) (twinkle - 0.82f) / 0.18f * 0.5f else 0f
        }
    }
}

/** Blends a colour towards white without changing its hue, used for highlight bands. */
private fun blendTowardsWhite(color: Color, amount: Float): Color {
    val clamped = amount.coerceIn(0f, 0.85f)
    return Color(
        red = color.red + (1f - color.red) * clamped,
        green = color.green + (1f - color.green) * clamped,
        blue = color.blue + (1f - color.blue) * clamped,
        alpha = color.alpha,
    )
}

/** Slower modes take longer to complete a cycle. */
private fun animationDurationMillis(speed: Float): Int {
    val clamped = speed.coerceIn(0f, 1f)
    return (9_000 - (clamped * 8_100f)).toInt().coerceAtLeast(900)
}

@ThemePreviews
@Composable
fun ModePreviewPreview() {
    LumaTheme {
        LumaModePreview(
            ramp = ColorRamp.default(),
            animation = ModeAnimation(kind = AnimationKind.CHASE),
        )
    }
}
