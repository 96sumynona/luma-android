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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luma.ledcontroller.core.designsystem.component.LumaActionDialog
import com.luma.ledcontroller.core.designsystem.component.LumaButton
import com.luma.ledcontroller.core.designsystem.component.LumaEmptyState
import com.luma.ledcontroller.core.designsystem.component.LumaInfoBanner
import com.luma.ledcontroller.core.designsystem.component.LumaSectionCard
import com.luma.ledcontroller.core.designsystem.component.LumaSectionHeader
import com.luma.ledcontroller.core.designsystem.component.LumaSettingRow
import com.luma.ledcontroller.core.designsystem.component.LumaTag
import com.luma.ledcontroller.core.designsystem.component.LumaTextButton
import com.luma.ledcontroller.core.designsystem.component.LumaTextField
import com.luma.ledcontroller.core.designsystem.icon.LumaIcons
import com.luma.ledcontroller.core.ui.LocalSnackbarHostState
import com.luma.ledcontroller.core.ui.LumaScreenEnter

/**
 * Settings is where the board → strip relationship is configured, and the only place Wi-Fi
 * credentials are entered.
 */
@Composable
fun SettingsScreen(
    onBoardClick: (boardId: String) -> Unit,
    onStripClick: (stripId: String, boardId: String) -> Unit,
    onAddBoard: () -> Unit,
    onAddStrip: (boardId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = LocalSnackbarHostState.current
    var showWifiDialog by remember { mutableStateOf(false) }

    val wifiSaved = stringResource(R.string.feature_settings_impl_wifi_saved)
    val wifiFailed = stringResource(R.string.feature_settings_impl_wifi_encryption_failed)
    val wifiRejected = stringResource(R.string.feature_settings_impl_wifi_ssid_error)
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                SettingsEvent.WifiSaved -> snackbarHostState.showSnackbar(wifiSaved)
                SettingsEvent.WifiEncryptionFailed -> snackbarHostState.showSnackbar(wifiFailed)
                is SettingsEvent.WifiRejected -> snackbarHostState.showSnackbar(wifiRejected)
            }
        }
    }

    if (showWifiDialog) {
        WifiCredentialsDialog(
            initialSsid = uiState.wifi.ssid,
            hasStoredPassword = uiState.wifi.hasStoredPassword,
            onDismiss = { showWifiDialog = false },
            onSave = { ssid, password ->
                viewModel.saveWifiCredentials(ssid, password)
                showWifiDialog = false
            },
        )
    }

    LumaScreenEnter(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "shell") {
                LumaInfoBanner(
                    icon = LumaIcons.Info,
                    title = stringResource(R.string.feature_settings_impl_shell_title),
                    message = stringResource(R.string.feature_settings_impl_shell_message),
                )
            }

            item(key = "boards-header") {
                LumaSectionHeader(
                    title = stringResource(R.string.feature_settings_impl_boards_title),
                    supportingText = stringResource(R.string.feature_settings_impl_boards_hint),
                )
            }

            if (uiState.boards.isEmpty() && !uiState.isLoading) {
                item(key = "boards-empty") {
                    LumaEmptyState(
                        icon = LumaIcons.Memory,
                        title = stringResource(R.string.feature_settings_impl_no_boards),
                        message = stringResource(R.string.feature_settings_impl_no_boards_message),
                        actionLabel = stringResource(R.string.feature_settings_impl_boards_empty_action),
                        onAction = onAddBoard,
                    )
                }
            }

            items(items = uiState.boards, key = { it.board.id }) { boardWithStrips ->
                val board = boardWithStrips.board
                LumaSectionCard(
                    title = board.name,
                    icon = LumaIcons.Memory,
                    subtitle = stringResource(
                        R.string.feature_settings_impl_board_summary,
                        board.effectivePixelLimit,
                        board.bufferSizeBytes,
                        board.layout.label(),
                    ),
                    trailing = {
                        IconButton(onClick = { onBoardClick(board.id) }) {
                            Icon(
                                imageVector = LumaIcons.Edit,
                                contentDescription = stringResource(R.string.feature_settings_impl_boards_title),
                            )
                        }
                    },
                ) {
                    if (boardWithStrips.strips.isEmpty()) {
                        LumaTag(
                            text = stringResource(R.string.feature_settings_impl_no_strips),
                            leadingIcon = LumaIcons.Lightbulb,
                        )
                    }
                    boardWithStrips.strips.forEach { strip ->
                        LumaSettingRow(
                            title = strip.name,
                            subtitle = stringResource(
                                R.string.feature_settings_impl_strip_summary,
                                strip.gpio,
                                strip.activeLeds,
                            ),
                            icon = LumaIcons.Lightbulb,
                            value = "${strip.brightnessPercent}%",
                            modifier = Modifier.clickable { onStripClick(strip.id, board.id) },
                        )
                    }
                    LumaTextButton(onClick = { onAddStrip(board.id) }) {
                        Icon(
                            imageVector = LumaIcons.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = stringResource(R.string.feature_settings_impl_add_strip))
                    }
                }
            }

            item(key = "add-board") {
                LumaTextButton(onClick = onAddBoard, modifier = Modifier.fillMaxWidth()) {
                    Icon(
                        imageVector = LumaIcons.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(R.string.feature_settings_impl_add_board))
                }
            }

            item(key = "wifi") {
                LumaSectionCard(
                    title = stringResource(R.string.feature_settings_impl_wifi_title),
                    icon = LumaIcons.Wifi,
                    subtitle = stringResource(R.string.feature_settings_impl_wifi_hint),
                    trailing = {
                        LumaTextButton(onClick = { showWifiDialog = true }) {
                            Text(text = stringResource(R.string.feature_settings_impl_wifi_edit))
                        }
                    },
                ) {
                    if (uiState.wifi.ssid.isBlank()) {
                        LumaTag(
                            text = stringResource(R.string.feature_settings_impl_wifi_not_set),
                            leadingIcon = LumaIcons.Wifi,
                        )
                    } else {
                        LumaSettingRow(
                            title = stringResource(R.string.feature_settings_impl_wifi_ssid),
                            value = uiState.wifi.ssid,
                            icon = LumaIcons.Wifi,
                        )
                        if (uiState.wifi.hasStoredPassword) {
                            LumaSettingRow(
                                title = stringResource(R.string.feature_settings_impl_wifi_password_stored),
                                icon = LumaIcons.Lock,
                                value = "••••••••",
                            )
                        }
                    }
                }
            }

            item(key = "about") {
                LumaSectionCard(
                    title = stringResource(R.string.feature_settings_impl_about_title),
                    icon = LumaIcons.Info,
                    subtitle = stringResource(R.string.feature_settings_impl_about_message),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        LumaTag(
                            text = stringResource(R.string.feature_settings_impl_limit_pixels),
                            leadingIcon = LumaIcons.Grid,
                        )
                        LumaTag(
                            text = stringResource(R.string.feature_settings_impl_limit_brightness),
                            leadingIcon = LumaIcons.Brightness,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WifiCredentialsDialog(
    initialSsid: String,
    hasStoredPassword: Boolean,
    onDismiss: () -> Unit,
    onSave: (ssid: String, password: String) -> Unit,
) {
    var ssid by remember { mutableStateOf(initialSsid) }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var showErrors by remember { mutableStateOf(false) }

    val ssidError = showErrors && ssid.isBlank()
    val passwordError = showErrors && password.isBlank() && !hasStoredPassword

    LumaActionDialog(
        title = stringResource(R.string.feature_settings_impl_wifi_title),
        onDismissRequest = onDismiss,
        message = stringResource(R.string.feature_settings_impl_wifi_hint),
    ) {
        LumaTextField(
            value = ssid,
            onValueChange = { ssid = it },
            label = stringResource(R.string.feature_settings_impl_wifi_ssid_label),
            error = if (ssidError) {
                stringResource(R.string.feature_settings_impl_wifi_ssid_error)
            } else {
                null
            },
            leadingIcon = LumaIcons.Wifi,
        )
        LumaTextField(
            value = password,
            onValueChange = { password = it },
            label = stringResource(R.string.feature_settings_impl_wifi_password_label),
            error = if (passwordError) {
                stringResource(R.string.feature_settings_impl_wifi_password_error)
            } else {
                null
            },
            leadingIcon = LumaIcons.Lock,
            keyboardType = KeyboardType.Password,
            visualTransformation = if (passwordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) {
                            LumaIcons.VisibilityOff
                        } else {
                            LumaIcons.Visibility
                        },
                        contentDescription = stringResource(
                            R.string.feature_settings_impl_wifi_password_label,
                        ),
                    )
                }
            },
            supportingText = if (hasStoredPassword) {
                stringResource(R.string.feature_settings_impl_wifi_password_stored)
            } else {
                null
            },
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            LumaTextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.feature_settings_impl_cancel))
            }
            LumaButton(
                onClick = {
                    showErrors = true
                    val validSsid = ssid.isNotBlank()
                    val validPassword = password.isNotBlank() || hasStoredPassword
                    if (validSsid && validPassword) onSave(ssid, password)
                },
                enabled = !ssidError,
            ) {
                Text(text = stringResource(R.string.feature_settings_impl_wifi_save))
            }
        }
    }
}
