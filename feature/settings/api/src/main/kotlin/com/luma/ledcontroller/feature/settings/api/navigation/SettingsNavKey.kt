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

package com.luma.ledcontroller.feature.settings.api.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** The Settings tab: boards, strips and Wi-Fi credentials. */
@Serializable
data object SettingsNavKey : NavKey

/**
 * Board configuration.
 *
 * @param boardId the board being edited, or `null` for a new board.
 */
@Serializable
data class BoardEditorNavKey(val boardId: String? = null) : NavKey

/**
 * Strip configuration.
 *
 * @param stripId the strip being edited, or `null` for a new strip.
 * @param boardId the board the strip belongs to. A strip always has exactly one board.
 */
@Serializable
data class StripEditorNavKey(
    val stripId: String? = null,
    val boardId: String,
) : NavKey
