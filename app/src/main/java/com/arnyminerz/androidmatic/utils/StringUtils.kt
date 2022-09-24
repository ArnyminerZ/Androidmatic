package com.arnyminerz.androidmatic.utils

import android.content.Context
import com.arnyminerz.androidmatic.R

/**
 * Converts a number of milliseconds into a [String] representing the time.
 *
 * Example:
 * `56000` -> `56s`
 * @author Arnau Mora
 * @since 20220924
 */
fun Long.shortTime(context: Context): String {
    val seconds = this / 1000
    if (seconds < 60)
        return context.getString(R.string.time_short_seconds, seconds)

    val minutes = seconds / 60
    if (minutes < 60)
        return context.getString(R.string.time_short_minutes, minutes)

    val hours = minutes / 60
    if (hours < 60)
        return context.getString(R.string.time_short_hours, hours)

    val days = hours / 60
    return context.getString(R.string.time_short_days, days)
}
