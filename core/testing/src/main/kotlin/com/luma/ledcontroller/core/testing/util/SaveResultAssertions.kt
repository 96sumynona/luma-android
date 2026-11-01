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

package com.luma.ledcontroller.core.testing.util

import com.luma.ledcontroller.core.domain.SaveResult
import com.luma.ledcontroller.core.domain.savedValue

/** Returns the saved value, failing the test when the use case rejected the input. */
fun <T> SaveResult<T>.requireSaved(): T = savedValue()
    ?: error("Expected a saved result but the input was rejected")
