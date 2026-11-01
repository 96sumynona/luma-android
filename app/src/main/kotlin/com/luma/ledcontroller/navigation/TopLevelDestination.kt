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

package com.luma.ledcontroller.navigation

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import com.luma.ledcontroller.R
import com.luma.ledcontroller.core.designsystem.icon.LumaIcons
import com.luma.ledcontroller.feature.home.api.navigation.HomeNavKey
import com.luma.ledcontroller.feature.modes.api.navigation.ModesNavKey
import com.luma.ledcontroller.feature.schedules.api.navigation.SchedulesNavKey
import com.luma.ledcontroller.feature.settings.api.navigation.SettingsNavKey

/**
 * The four tabs of the app.
 *
 * Every tab is a top-level navigation key with its own back stack; nested destinations (an editor, a
 * strip detail) are pushed onto the stack of the tab that opened them.
 */
enum class TopLevelDestination(
    val navKey: NavKey,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    @StringRes val labelRes: Int,
) {
    HOME(
        navKey = HomeNavKey,
        selectedIcon = LumaIcons.Lightbulb,
        unselectedIcon = LumaIcons.LightbulbBorder,
        labelRes = R.string.app_tab_home,
    ),
    MODES(
        navKey = ModesNavKey,
        selectedIcon = LumaIcons.Palette,
        unselectedIcon = LumaIcons.PaletteBorder,
        labelRes = R.string.app_tab_modes,
    ),
    SCHEDULES(
        navKey = SchedulesNavKey,
        selectedIcon = LumaIcons.Schedule,
        unselectedIcon = LumaIcons.ScheduleBorder,
        labelRes = R.string.app_tab_schedules,
    ),
    SETTINGS(
        navKey = SettingsNavKey,
        selectedIcon = LumaIcons.Settings,
        unselectedIcon = LumaIcons.SettingsBorder,
        labelRes = R.string.app_tab_settings,
    ),
}

/** Top level nav items, keyed by their nav key. */
val TOP_LEVEL_NAV_ITEMS: Map<NavKey, TopLevelDestination> =
    TopLevelDestination.entries.associateBy { it.navKey }
