package com.arnyminerz.androidmatic.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import kotlin.reflect.KClass

/**
 * Launches the selected activity.
 * @author Arnau Mora
 * @since 20220923
 * @param kClass The class of the Activity to launch.
 * @param options Used for applying options to the Intent being launched.
 */
fun <A: Activity> Context.launch(kClass: KClass<A>, options: Intent.() -> Unit = {}) =
    startActivity(Intent(this, kClass.java).apply(options))

/**
 * Gets the value of an attribute resource as a Color.
 * @author Arnau Mora
 * @since 20220924
 * @param id The id of the attribute.
 * @return A color in Integer format.
 */
@ColorInt
fun Context.getColorAttribute(@AttrRes id: Int): Int {
    val typedValue = TypedValue()
    theme.resolveAttribute(id, typedValue, true)
    return typedValue.data
}
