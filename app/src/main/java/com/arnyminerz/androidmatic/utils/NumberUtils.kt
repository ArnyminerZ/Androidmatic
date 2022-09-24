package com.arnyminerz.androidmatic.utils

import androidx.annotation.ColorInt
import androidx.compose.ui.graphics.Color

/**
 * Converts an integer into a Jetpack Compose' Color.
 * @author Arnau Mora
 * @since 20220924
 */
@ColorInt
fun Int.toColor() = Color(this)
