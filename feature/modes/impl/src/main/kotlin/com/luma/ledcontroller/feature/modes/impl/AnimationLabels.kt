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

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.luma.ledcontroller.core.designsystem.icon.LumaIcons
import com.luma.ledcontroller.core.model.AnimationKind

/** Consistent names and icons for the stored animation kinds. */
@Composable
fun AnimationKind.label(): String = stringResource(
    when (this) {
        AnimationKind.SOLID -> R.string.feature_modes_impl_animation_solid
        AnimationKind.BREATHE -> R.string.feature_modes_impl_animation_breathe
        AnimationKind.CHASE -> R.string.feature_modes_impl_animation_chase
        AnimationKind.TWINKLE -> R.string.feature_modes_impl_animation_twinkle
    },
)

fun AnimationKind.icon(): ImageVector = when (this) {
    AnimationKind.SOLID -> LumaIcons.Palette
    AnimationKind.BREATHE -> LumaIcons.Brightness
    AnimationKind.CHASE -> LumaIcons.Speed
    AnimationKind.TWINKLE -> LumaIcons.AutoAwesome
}

/** Short description of what the kind does, used as supporting text in the picker. */
@Composable
fun AnimationKind.description(): String = when (this) {
    AnimationKind.SOLID -> "The ramp is mapped along the strip and stays still."
    AnimationKind.BREATHE -> "The strip fades in and out along the ramp."
    AnimationKind.CHASE -> "The ramp travels along the strip."
    AnimationKind.TWINKLE -> "Pixels light up along the ramp at random positions."
}
