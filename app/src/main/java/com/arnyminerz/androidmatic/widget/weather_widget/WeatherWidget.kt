package com.arnyminerz.androidmatic.widget.weather_widget

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalGlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.arnyminerz.androidmatic.R
import com.arnyminerz.androidmatic.data.Station
import com.arnyminerz.androidmatic.data.WeatherStateSample
import com.arnyminerz.androidmatic.storage.dataStore
import com.arnyminerz.androidmatic.utils.shortTime
import com.arnyminerz.androidmatic.widget.TintImage
import com.arnyminerz.androidmatic.widget.provideColorFromResource
import org.json.JSONObject
import timber.log.Timber
import java.util.Date

class WeatherWidget : GlanceAppWidget() {
    companion object {
        fun preferencesKey(glanceId: GlanceId) = stringPreferencesKey("widget-$glanceId")
    }

    private val backgroundColor: ColorProvider
        @Composable
        get() = provideColorFromResource(
            R.color.md_theme_light_background,
            R.color.md_theme_dark_background
        )

    private val foregroundColor: ColorProvider
        @Composable
        get() = provideColorFromResource(
            R.color.md_theme_light_onBackground,
            R.color.md_theme_dark_onBackground
        )

    private val accentColor: ColorProvider
        @Composable
        get() = provideColorFromResource(
            R.color.md_theme_light_tertiary,
            R.color.md_theme_dark_tertiary
        )

    @Composable
    private fun WeatherRow(text: String, @StringRes description: Int, @DrawableRes icon: Int) {
        val context = LocalContext.current

        Row(
            modifier = GlanceModifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TintImage(
                resource = icon,
                tintColor = accentColor,
                contentDescription = context.getString(description),
                modifier = GlanceModifier
                    .size(24.dp)
                    .padding(end = 4.dp),
            )
            Text(
                text,
                style = TextStyle(
                    color = foregroundColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                ),
            )
        }
    }

    override suspend fun onDelete(context: Context, glanceId: GlanceId) {
        updateAppWidgetState(context, glanceId) { preferences ->
            Timber.i("Removing data for widget $glanceId...")
            preferences.remove(preferencesKey(glanceId))
        }
    }

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val glanceId = LocalGlanceId.current

        val prefs = currentState<Preferences>()
        val data = prefs[preferencesKey(glanceId)]?.let {
            WeatherWidgetData.fromJson(JSONObject(it))
        }!!

        val station = data.station
        val weather = data.weather

        Row(
            modifier = GlanceModifier
                .fillMaxSize()
                .appWidgetBackground()
                .background(backgroundColor)
                .cornerRadius(24.dp),
        ) {
            TintImage(
                resource = R.drawable.ic_weather_sunny,
                tintColor = accentColor,
                contentDescription = context.getString(R.string.widget_weather_image_content_description),
                modifier = GlanceModifier
                    .size(56.dp)
                    .padding(start = 8.dp, top = 8.dp),
            )
            Column(
                modifier = GlanceModifier
                    .defaultWeight()
                    .fillMaxHeight()
                    .padding(8.dp),
            ) {
                Text(
                    station.name,
                    style = TextStyle(
                        color = foregroundColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                    ),
                    modifier = GlanceModifier
                        .fillMaxWidth(),
                )
                WeatherRow(
                    "${weather.temperatureValues.value}ºC",
                    R.string.image_desc_temperature,
                    R.drawable.ic_thermometer_48dp,
                )
                WeatherRow(
                    "${weather.rain}mm",
                    R.string.image_desc_rain,
                    R.drawable.ic_rainy_48dp,
                )

                val diff = Date().time - weather.timestamp.time
                Text(
                    diff.shortTime(context),
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .padding(end = 4.dp),
                    style = TextStyle(
                        color = foregroundColor,
                        textAlign = TextAlign.End,
                        fontSize = 12.sp,
                    ),
                )
            }
        }
    }
}
