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

package com.luma.ledcontroller.feature.modes.api.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** The Modes tab: the library of reusable modes. */
@Serializable
data object ModesNavKey : NavKey

/**
 * The mode editor.
 *
 * @param modeId the mode being edited, or `null` for a brand new mode.
 */
@Serializable
data class ModeEditorNavKey(val modeId: String? = null) : NavKey
