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

package com.luma.ledcontroller.core.model

import kotlin.math.cbrt
import kotlin.math.pow

/**
 * Colour utilities used by [ColorRamp].
 *
 * Colours are stored as a packed ARGB value (`0xAARRGGBB`) in a [Long] so that the model module
 * stays framework free and the same values can be written straight into a transfer payload.
 *
 * Gradient interpolation happens in OKLab rather than in sRGB. Interpolating in sRGB produces the
 * muddy mid-points that make LED scenes look washed out; OKLab keeps the perceived lightness even,
 * which matches what a strip of WS2812B pixels looks like in a room.
 */
object ColorMath {

    /** Opaque black, used when a ramp has no stops at all. */
    const val OPAQUE_BLACK: Long = 0xFF000000L

    /** Packs the components into `0xAARRGGBB`. */
    fun argb(alpha: Int, red: Int, green: Int, blue: Int): Long =
        (alpha.coerceIn(0, 255).toLong() shl 24) or
            (red.coerceIn(0, 255).toLong() shl 16) or
            (green.coerceIn(0, 255).toLong() shl 8) or
            blue.coerceIn(0, 255).toLong()

    /** Packs fully opaque components. */
    fun rgb(red: Int, green: Int, blue: Int): Long = argb(alpha = 255, red = red, green = green, blue = blue)

    fun alphaOf(argb: Long): Int = ((argb shr 24) and 0xFF).toInt()

    fun redOf(argb: Long): Int = ((argb shr 16) and 0xFF).toInt()

    fun greenOf(argb: Long): Int = ((argb shr 8) and 0xFF).toInt()

    fun blueOf(argb: Long): Int = (argb and 0xFF).toInt()

    /** Returns the colour as `#RRGGBB`, used by the hex field in the colour picker. */
    fun toHex(argb: Long): String = String.format("#%06X", argb and 0xFFFFFF)

    /** Parses `#RRGGBB` or `RRGGBB`. Returns `null` when the text is not a colour. */
    fun fromHex(hex: String): Long? {
        val cleaned = hex.trim().removePrefix("#")
        if (cleaned.length != 6) return null
        val value = cleaned.toLongOrNull(radix = 16) ?: return null
        return (0xFF000000L or value)
    }

    /** Interpolates two colours in OKLab by [fraction] (0 = [from], 1 = [to]). */
    fun mix(from: Long, to: Long, fraction: Float): Long {
        val t = fraction.coerceIn(0f, 1f)
        if (t == 0f) return from
        if (t == 1f) return to
        val labFrom = argbToOklab(from)
        val labTo = argbToOklab(to)
        val mixed = Oklab(
            l = labFrom.l + (labTo.l - labFrom.l) * t,
            a = labFrom.a + (labTo.a - labFrom.a) * t,
            b = labFrom.b + (labTo.b - labFrom.b) * t,
        )
        val alpha = (alphaOf(from) + (alphaOf(to) - alphaOf(from)) * t).toInt().coerceIn(0, 255)
        return oklabToArgb(mixed, alpha)
    }

    /** Relative luminance in linear-light sRGB, in `0f..1f`. */
    fun luminance(argb: Long): Float {
        val r = srgbToLinear(redOf(argb) / 255f)
        val g = srgbToLinear(greenOf(argb) / 255f)
        val b = srgbToLinear(blueOf(argb) / 255f)
        return (0.2126f * r) + (0.7152f * g) + (0.0722f * b)
    }

    /** Returns `true` when the colour is bright enough to need dark foreground content on it. */
    fun isLight(argb: Long): Boolean = luminance(argb) > 0.4f

    internal fun argbToOklab(argb: Long): Oklab {
        val r = srgbToLinear(redOf(argb) / 255f)
        val g = srgbToLinear(greenOf(argb) / 255f)
        val b = srgbToLinear(blueOf(argb) / 255f)

        val l = 0.4122214708f * r + 0.5363325363f * g + 0.0514459929f * b
        val m = 0.2119034982f * r + 0.6806995451f * g + 0.1073969566f * b
        val s = 0.0883024619f * r + 0.2817188376f * g + 0.6299787005f * b

        val lCbrt = cbrt(l.toDouble()).toFloat()
        val mCbrt = cbrt(m.toDouble()).toFloat()
        val sCbrt = cbrt(s.toDouble()).toFloat()

        return Oklab(
            l = 0.2104542553f * lCbrt + 0.7936177850f * mCbrt - 0.0040720468f * sCbrt,
            a = 1.9779984951f * lCbrt - 2.4285922050f * mCbrt + 0.4505937099f * sCbrt,
            b = 0.0259040371f * lCbrt + 0.7827717662f * mCbrt - 0.8086757660f * sCbrt,
        )
    }

    internal fun oklabToArgb(oklab: Oklab, alpha: Int = 255): Long {
        val lCbrt = oklab.l + 0.3963377774f * oklab.a + 0.2158037573f * oklab.b
        val mCbrt = oklab.l - 0.1055613458f * oklab.a - 0.0638541728f * oklab.b
        val sCbrt = oklab.l - 0.0894841775f * oklab.a - 1.2914855480f * oklab.b

        val l = lCbrt * lCbrt * lCbrt
        val m = mCbrt * mCbrt * mCbrt
        val s = sCbrt * sCbrt * sCbrt

        val r = 4.0767416621f * l - 3.3077115913f * m + 0.2309699292f * s
        val g = -1.2684380046f * l + 2.6097574011f * m - 0.3413193965f * s
        val b = -0.0041960863f * l - 0.7034186147f * m + 1.7076147010f * s

        return argb(
            alpha = alpha,
            red = (linearToSrgb(r) * 255f).toInt(),
            green = (linearToSrgb(g) * 255f).toInt(),
            blue = (linearToSrgb(b) * 255f).toInt(),
        )
    }

    internal fun srgbToLinear(channel: Float): Float =
        if (channel <= 0.04045f) {
            channel / 12.92f
        } else {
            ((channel + 0.055f) / 1.055f).pow(2.4f)
        }

    internal fun linearToSrgb(channel: Float): Float {
        val clamped = channel.coerceIn(0f, 1f)
        return if (clamped <= 0.0031308f) {
            12.92f * clamped
        } else {
            1.055f * clamped.pow(1f / 2.4f) - 0.055f
        }
    }

    internal data class Oklab(val l: Float, val a: Float, val b: Float)
}
