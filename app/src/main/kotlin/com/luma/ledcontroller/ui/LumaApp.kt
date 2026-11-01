/*
 * Copyright 2022 The Android Open Source Project
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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.luma.ledcontroller.R
import com.luma.ledcontroller.core.designsystem.component.LumaBackground
import com.luma.ledcontroller.core.designsystem.component.LumaGradientBackground
import com.luma.ledcontroller.core.designsystem.component.LumaNavigationSuiteScaffold
import com.luma.ledcontroller.core.designsystem.component.LumaTopAppBar
import com.luma.ledcontroller.core.designsystem.icon.LumaIcons
import com.luma.ledcontroller.core.designsystem.theme.GradientColors
import com.luma.ledcontroller.core.navigation.Navigator
import com.luma.ledcontroller.core.navigation.toEntries
import com.luma.ledcontroller.core.ui.LeaveGuard
import com.luma.ledcontroller.core.ui.LocalLeaveGuard
import com.luma.ledcontroller.core.ui.LocalSnackbarHostState
import com.luma.ledcontroller.feature.home.impl.navigation.homeEntry
import com.luma.ledcontroller.feature.modes.impl.navigation.modesEntry
import com.luma.ledcontroller.feature.schedules.impl.navigation.schedulesEntry
import com.luma.ledcontroller.feature.settings.api.navigation.SettingsNavKey
import com.luma.ledcontroller.feature.settings.impl.navigation.settingsEntry
import com.luma.ledcontroller.navigation.TOP_LEVEL_NAV_ITEMS

/**
 * The app shell.
 *
 * The shell owns the four tabs, the single top app bar and the one place leaving is decided: every
 * exit (system back, toolbar back, tab switch) goes through [LeaveGuard], so a dirty editor is never
 * skipped by a different route.
 */
@Composable
fun LumaApp(
    appState: LumaAppState,
    modifier: Modifier = Modifier,
    windowAdaptiveInfo: WindowAdaptiveInfo = currentWindowAdaptiveInfo(),
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val leaveGuard = remember { LeaveGuard() }

    CompositionLocalProvider(
        LocalSnackbarHostState provides snackbarHostState,
        LocalLeaveGuard provides leaveGuard,
    ) {
        LumaBackground(modifier = modifier) {
            LumaGradientBackground(gradientColors = GradientColors()) {
                LumaAppScaffold(
                    appState = appState,
                    leaveGuard = leaveGuard,
                    snackbarHostState = snackbarHostState,
                    windowAdaptiveInfo = windowAdaptiveInfo,
                )
            }
        }
    }
}

@Composable
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalComposeUiApi::class,
    ExperimentalMaterial3AdaptiveApi::class,
)
private fun LumaAppScaffold(
    appState: LumaAppState,
    leaveGuard: LeaveGuard,
    snackbarHostState: SnackbarHostState,
    windowAdaptiveInfo: WindowAdaptiveInfo,
    modifier: Modifier = Modifier,
) {
    val navigationState = appState.navigationState
    val navigator = remember(navigationState) { Navigator(navigationState) }
    val currentKey = navigationState.currentKey
    val isAtTopLevel = navigationState.isAtTopLevel

    // Tab switches are exits too: a dirty editor gets the same prompt as toolbar or system back.
    fun switchTab(navKey: NavKey) {
        leaveGuard.requestLeave { navigator.navigate(navKey) }
    }

    LumaNavigationSuiteScaffold(
        navigationSuiteItems = {
            TOP_LEVEL_NAV_ITEMS.forEach { (navKey, destination) ->
                item(
                    selected = navKey == navigationState.currentTopLevelKey,
                    onClick = { switchTab(navKey) },
                    icon = {
                        Icon(imageVector = destination.unselectedIcon, contentDescription = null)
                    },
                    selectedIcon = {
                        Icon(imageVector = destination.selectedIcon, contentDescription = null)
                    },
                    label = { Text(text = stringResource(destination.labelRes)) },
                )
            }
        },
        windowAdaptiveInfo = windowAdaptiveInfo,
    ) {
        Scaffold(
            modifier = modifier.semantics { testTagsAsResourceId = true },
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .consumeWindowInsets(padding)
                    .imePadding(),
            ) {
                LumaTopAppBar(
                    title = currentKey.destinationTitle(),
                    navigationIcon = if (isAtTopLevel) null else LumaIcons.ArrowBack,
                    navigationIconContentDescription = if (isAtTopLevel) {
                        null
                    } else {
                        stringResource(R.string.app_navigate_back)
                    },
                    onNavigationClick = {
                        leaveGuard.requestLeave { navigator.goBack() }
                    },
                )

                Box(modifier = Modifier.weight(1f)) {
                    val entryProvider = entryProvider {
                        homeEntry(
                            navigator = navigator,
                            onNavigateToSettings = { switchTab(SettingsNavKey) },
                        )
                        modesEntry(navigator)
                        schedulesEntry(navigator)
                        settingsEntry(navigator)
                    }
                    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>()

                    NavDisplay(
                        entries = navigationState.toEntries(entryProvider),
                        sceneStrategies = listOf(listDetailStrategy),
                        onBack = {
                            // One exit path for gestures and the system back button.
                            leaveGuard.requestLeave { navigator.goBack() }
                        },
                    )
                }
            }
        }
    }
}

/**
 * The toolbar title for a destination.
 *
 * Nested destinations get their own title so the toolbar always says what is on screen, even though
 * the app shell draws it.
 */
@Composable
private fun NavKey.destinationTitle(): String = if (this in TOP_LEVEL_NAV_ITEMS) {
    stringResource(TOP_LEVEL_NAV_ITEMS.getValue(this).labelRes)
} else {
    nestedTitle()
}
