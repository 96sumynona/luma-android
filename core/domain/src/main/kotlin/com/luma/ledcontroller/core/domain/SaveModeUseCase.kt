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

package com.luma.ledcontroller.core.domain

import com.luma.ledcontroller.core.data.IdGenerator
import com.luma.ledcontroller.core.data.repository.ModeRepository
import com.luma.ledcontroller.core.model.ColorRamp
import com.luma.ledcontroller.core.model.LumaError
import com.luma.ledcontroller.core.model.LumaMode
import com.luma.ledcontroller.core.model.LumaValidation
import com.luma.ledcontroller.core.model.ModeAnimation
import kotlinx.datetime.Clock
import javax.inject.Inject

/**
 * Saves a mode, either over the existing mode or as a brand new one.
 *
 * The mode editor only ever holds a draft; this is the single place that turns a draft into saved
 * data, so the "Discard / Save / Save as new mode" choice can never half-apply.
 */
class SaveModeUseCase @Inject constructor(
    private val modeRepository: ModeRepository,
    private val idGenerator: IdGenerator,
    private val clock: Clock,
) {
    suspend operator fun invoke(
        modeId: String?,
        name: String,
        ramp: ColorRamp,
        animation: ModeAnimation,
        saveAsNewMode: Boolean = false,
    ): SaveResult<LumaMode> {
        val errors = LumaValidation.validateMode(name = name, ramp = ramp, animation = animation)
        if (errors.isNotEmpty()) return SaveResult.Invalid(errors)

        val now = clock.now().toEpochMilliseconds()
        val existing = modeId?.takeUnless { saveAsNewMode }?.let { modeRepository.getMode(it) }
        val mode = LumaMode(
            id = existing?.id ?: idGenerator.newId(),
            name = name.trim(),
            ramp = ramp,
            animation = animation.normalised(),
            createdAtEpochMillis = existing?.createdAtEpochMillis ?: now,
            updatedAtEpochMillis = now,
        )
        modeRepository.upsertMode(mode)
        return SaveResult.Saved(mode)
    }
}

/** Deletes a mode and cleans up the strips and schedules that referenced it. */
class DeleteModeUseCase @Inject constructor(
    private val modeRepository: ModeRepository,
) {
    suspend operator fun invoke(modeId: String) {
        modeRepository.deleteMode(modeId)
    }
}

/** Validation helper used by the editor to show inline errors before saving. */
class ValidateModeUseCase @Inject constructor() {
    operator fun invoke(name: String, ramp: ColorRamp, animation: ModeAnimation): Set<LumaError> =
        LumaValidation.validateMode(name = name, ramp = ramp, animation = animation)
}
