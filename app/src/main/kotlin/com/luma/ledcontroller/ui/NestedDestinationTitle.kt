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

package com.luma.ledcontroller.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavKey
import com.luma.ledcontroller.R
import com.luma.ledcontroller.feature.home.api.R as homeApiR
import com.luma.ledcontroller.feature.home.api.navigation.StripDetailNavKey
import com.luma.ledcontroller.feature.modes.api.R as modesApiR
import com.luma.ledcontroller.feature.modes.api.navigation.ModeEditorNavKey
import com.luma.ledcontroller.feature.schedules.api.R as schedulesApiR
import com.luma.ledcontroller.feature.schedules.api.navigation.ScheduleEditorNavKey
import com.luma.ledcontroller.feature.settings.api.R as settingsApiR
import com.luma.ledcontroller.feature.settings.api.navigation.BoardEditorNavKey
import com.luma.ledcontroller.feature.settings.api.navigation.StripEditorNavKey

/**
 * Titles for the destinations that are not tabs.
 *
 * The strings live in each feature's `api` module, because the shell draws the toolbar and would
 * otherwise have to know what a feature's screens are called.
 */
@Composable
internal fun NavKey.nestedTitle(): String = when (this) {
    is StripDetailNavKey -> stringResource(homeApiR.string.feature_home_api_strip_detail_title)

    is ModeEditorNavKey -> stringResource(
        if (modeId == null) {
            modesApiR.string.feature_modes_api_editor_new_title
        } else {
            modesApiR.string.feature_modes_api_editor_edit_title
        },
    )

    is ScheduleEditorNavKey -> stringResource(
        if (scheduleId == null) {
            schedulesApiR.string.feature_schedules_api_new_title
        } else {
            schedulesApiR.string.feature_schedules_api_edit_title
        },
    )

    is BoardEditorNavKey -> stringResource(
        if (boardId == null) {
            settingsApiR.string.feature_settings_api_new_board_title
        } else {
            settingsApiR.string.feature_settings_api_board_title
        },
    )

    is StripEditorNavKey -> stringResource(
        if (stripId == null) {
            settingsApiR.string.feature_settings_api_new_strip_title
        } else {
            settingsApiR.string.feature_settings_api_strip_title
        },
    )

    else -> stringResource(R.string.app_name)
}
