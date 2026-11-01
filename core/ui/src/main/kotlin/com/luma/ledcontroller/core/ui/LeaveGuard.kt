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

package com.luma.ledcontroller.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue

/**
 * Lets a screen with unsaved edits intercept *every* way of leaving it.
 *
 * The app shell routes system back, the toolbar back arrow and tab switches through
 * [requestLeave], so an editor cannot be left by one route while another silently drops the draft.
 * The guard holds at most one handler: the visible editor registers while it is dirty and clears
 * itself when it is not.
 */
@Stable
class LeaveGuard {
    private var handler: ((onLeave: () -> Unit) -> Unit)? by mutableStateOf(null)
    private var owner: Any? by mutableStateOf(null)

    /** `true` while a screen is holding the guard, i.e. when leaving needs to be confirmed. */
    val isGuarded: Boolean
        get() = handler != null

    /**
     * Registers [handler], returning a token that must be passed to [unregister].
     */
    fun register(handler: (onLeave: () -> Unit) -> Unit): Any {
        val token = Any()
        owner = token
        this.handler = handler
        return token
    }

    /** Removes the handler, but only if [token] is still the active owner. */
    fun unregister(token: Any) {
        if (owner === token) {
            owner = null
            handler = null
        }
    }

    /**
     * Leaves the current screen, asking the registered screen first when there is one.
     *
     * @param onLeave what leaving means for the caller (pop the back stack, switch tab, ...).
     */
    fun requestLeave(onLeave: () -> Unit) {
        val current = handler
        if (current == null) onLeave() else current(onLeave)
    }
}

/** The guard provided by the app shell. */
val LocalLeaveGuard = compositionLocalOf { LeaveGuard() }

/**
 * Registers [onLeaveRequested] while [isDirty] is true.
 *
 * @param isDirty whether the screen currently holds unsaved edits.
 * @param onLeaveRequested invoked with a callback that continues leaving; the screen shows its own
 *   confirmation and only calls the callback once the user has decided.
 */
@Composable
fun LeaveGuardEffect(
    isDirty: Boolean,
    onLeaveRequested: (proceed: () -> Unit) -> Unit,
) {
    val guard = LocalLeaveGuard.current
    // Always call the latest lambda, even though the guard only holds one registration.
    val currentHandler by rememberUpdatedState(onLeaveRequested)

    DisposableEffect(guard, isDirty) {
        if (isDirty) {
            val token = guard.register { onLeave -> currentHandler(onLeave) }
            onDispose { guard.unregister(token) }
        } else {
            onDispose { }
        }
    }
}
