package com.arnyminerz.androidmatic.widget

import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmapOrNull
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.layout.ContentScale
import androidx.glance.unit.ColorProvider
import com.arnyminerz.androidmatic.R
import com.arnyminerz.androidmatic.utils.toColor

/**
 * Provides an image component whose contents can be tinted.
 * @author Arnau Mora
 * @since 20220924
 *
 */
@Composable
fun TintImage(
    @DrawableRes resource: Int,
    tintColor: ColorProvider,
    contentDescription: String,
    modifier: GlanceModifier = GlanceModifier,
    contentScale: ContentScale = ContentScale.Fit,
) {
    val context = LocalContext.current
    Image(
        provider = ImageProvider(
            ContextCompat.getDrawable(context, resource)
                ?.apply { setTint(tintColor.getColor(context).toArgb()) }
                ?.toBitmapOrNull()!!
        ),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
    )
}

/**
 * Creates a [ColorProvider] with the given colors for light and dark theme, fetching them from
 * the [LocalContext]'s resources.
 * @author Arnau Mora
 * @since 20220924
 * @param day The color to use in light mode.
 * @param night The color to use in dark mode.
 */
@Composable
fun provideColorFromResource(@ColorRes day: Int, @ColorRes night: Int): ColorProvider {
    val context = LocalContext.current
    return androidx.glance.appwidget.unit.ColorProvider(
        day = ContextCompat.getColor(context, day).toColor(),
        night = ContextCompat.getColor(context, night).toColor(),
    )
}
