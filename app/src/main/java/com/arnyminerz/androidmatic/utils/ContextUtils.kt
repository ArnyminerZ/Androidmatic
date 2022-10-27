package com.arnyminerz.androidmatic.utils

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.util.TypedValue
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.annotation.UiThread
import androidx.lifecycle.AndroidViewModel
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import kotlin.reflect.KClass

/**
 * Launches the selected activity.
 * @author Arnau Mora
 * @since 20220923
 * @param kClass The class of the Activity to launch.
 * @param options Used for applying options to the Intent being launched.
 */
@UiThread
fun <A : Activity> Context.launch(kClass: KClass<A>, options: Intent.() -> Unit = {}) =
    startActivity(Intent(this, kClass.java).apply(options))

/**
 * Launches the selected activity for the given context.
 * @author Arnau Mora
 * @since 20221003
 * @param context The context that is requesting the launch.
 * @param kClass The class of the Activity.
 * @param options USed for applying options to the Intent being launched.
 * @throws ActivityNotFoundException If the given [kClass] is not a valid and registered activity
 * class.
 */
@UiThread
@Throws(ActivityNotFoundException::class)
fun <A : Activity> ActivityResultLauncher<Intent>.launch(
    context: Context,
    kClass: KClass<A>,
    options: Intent.() -> Unit = {},
) = launch(Intent(context, kClass.java).apply(options))

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

@UiThread
fun Context.toast(text: String, duration: Int = Toast.LENGTH_SHORT) =
    Toast.makeText(this, text, duration).show()

@UiThread
fun Context.toast(@StringRes text: Int, duration: Int = Toast.LENGTH_SHORT) =
    Toast.makeText(this, text, duration).show()

/**
 * Runs [AndroidViewModel.getApplication] as [Context].
 * @author Arnau Mora
 * @since 20220926
 */
val AndroidViewModel.context: Context
    get() = getApplication()

/**
 * Opens a share dialog with the text specified.
 * @author Arnau Mora
 * @since 20221003
 * @param text The text to share.
 */
@UiThread
fun Context.share(text: String) {
    val sendIntent: Intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, text)
        type = "text/plain"
    }

    val shareIntent = Intent.createChooser(sendIntent, null)
    startActivity(shareIntent)
}

/**
 * Checks if Google Play Services are available.
 * @author Arnau Mora
 * @since 20221027
 * @return `true` if they are available, `false` otherwise.
 */
fun Activity.areGooglePlayServicesAvailable(): Boolean {
    val apiAvailability = GoogleApiAvailability.getInstance()
    val status = apiAvailability.isGooglePlayServicesAvailable(this)
    if (status == ConnectionResult.SUCCESS) {
        if (apiAvailability.isUserResolvableError(status))
            apiAvailability.getErrorDialog(this, status, 2404)?.show()
        return false
    }
    return true
}
