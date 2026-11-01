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

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.luma.ledcontroller.core.model.LedLayout

/** Localised name for a stored pixel layout. */
@Composable
fun LedLayout.label(): String = stringResource(labelRes())

/** Resource id, for places that need a label outside a composable call. */
fun LedLayout.labelRes(): Int = when (this) {
    LedLayout.LINEAR -> R.string.feature_settings_impl_layout_linear
    LedLayout.SERPENTINE -> R.string.feature_settings_impl_layout_serpentine
}
