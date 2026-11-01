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

package com.luma.ledcontroller.feature.schedules.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.luma.ledcontroller.core.navigation.Navigator
import com.luma.ledcontroller.feature.schedules.api.navigation.ScheduleEditorNavKey
import com.luma.ledcontroller.feature.schedules.api.navigation.SchedulesNavKey
import com.luma.ledcontroller.feature.schedules.impl.ScheduleEditorScreen
import com.luma.ledcontroller.feature.schedules.impl.SchedulesScreen

/** The Schedules tab: the library plus the editor pushed above it. */
fun EntryProviderScope<NavKey>.schedulesEntry(navigator: Navigator) {
    entry<SchedulesNavKey> {
        SchedulesScreen(
            onScheduleClick = { scheduleId ->
                navigator.navigate(ScheduleEditorNavKey(scheduleId = scheduleId))
            },
            onCreateSchedule = { navigator.navigate(ScheduleEditorNavKey()) },
        )
    }
    entry<ScheduleEditorNavKey> { key ->
        ScheduleEditorScreen(
            scheduleId = key.scheduleId,
            presetStripId = key.stripId,
        )
    }
}
