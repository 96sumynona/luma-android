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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luma.ledcontroller.core.data.repository.ModeRepository
import com.luma.ledcontroller.core.data.repository.WifiRepository
import com.luma.ledcontroller.core.domain.GetBoardsUseCase
import com.luma.ledcontroller.core.domain.GetStripsUseCase
import com.luma.ledcontroller.core.domain.SaveResult
import com.luma.ledcontroller.core.domain.SaveWifiCredentialsUseCase
import com.luma.ledcontroller.core.domain.savedValue
import com.luma.ledcontroller.core.model.LumaBoard
import com.luma.ledcontroller.core.model.LumaError
import com.luma.ledcontroller.core.model.LumaMode
import com.luma.ledcontroller.core.model.LumaStrip
import com.luma.ledcontroller.core.model.WifiCredentialsSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** A board with the strips that belong to it, which is the relationship the UI has to make clear. */
data class BoardWithStrips(
    val board: LumaBoard,
    val strips: List<LumaStrip>,
)

data class SettingsUiState(
    val boards: List<BoardWithStrips> = emptyList(),
    val modes: List<LumaMode> = emptyList(),
    val wifi: WifiCredentialsSummary = WifiCredentialsSummary(),
    val isLoading: Boolean = true,
)

sealed interface SettingsEvent {
    data object WifiSaved : SettingsEvent

    data object WifiEncryptionFailed : SettingsEvent

    data class WifiRejected(val errors: Set<LumaError>) : SettingsEvent
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    getBoards: GetBoardsUseCase,
    getStrips: GetStripsUseCase,
    modeRepository: ModeRepository,
    wifiRepository: WifiRepository,
    private val saveWifiCredentials: SaveWifiCredentialsUseCase,
) : ViewModel() {

    private val events = Channel<SettingsEvent>(Channel.BUFFERED)
    val eventFlow = events.receiveAsFlow()

    val uiState: StateFlow<SettingsUiState> = combine(
        getBoards(),
        getStrips(),
        modeRepository.modes,
        wifiRepository.credentials,
    ) { boards, strips, modes, wifi ->
        SettingsUiState(
            boards = boards
                .sortedBy { it.name.lowercase() }
                .map { board ->
                    BoardWithStrips(
                        board = board,
                        strips = strips
                            .filter { it.boardId == board.id }
                            .sortedBy { it.name.lowercase() },
                    )
                },
            modes = modes.sortedBy { it.name.lowercase() },
            wifi = wifi,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(),
    )

    fun saveWifiCredentials(ssid: String, password: String) {
        viewModelScope.launch {
            val result = saveWifiCredentials(ssid, password)
            if (result.savedValue() != null) {
                events.send(SettingsEvent.WifiSaved)
                return@launch
            }
            // A blank password is reported as validation; a rejected *non-blank* password means the
            // encryption step failed and nothing was written.
            val rejected = result as SaveResult.Invalid
            events.send(
                if (LumaError.PASSWORD_BLANK in rejected.errors && password.isNotBlank()) {
                    SettingsEvent.WifiEncryptionFailed
                } else {
                    SettingsEvent.WifiRejected(rejected.errors)
                },
            )
        }
    }
}
