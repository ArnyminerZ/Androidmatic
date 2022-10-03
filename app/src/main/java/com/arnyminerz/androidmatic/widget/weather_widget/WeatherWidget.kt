package com.arnyminerz.androidmatic.widget.weather_widget

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalGlanceId
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.arnyminerz.androidmatic.R
import com.arnyminerz.androidmatic.activity.MainActivity
import com.arnyminerz.androidmatic.widget.GlanceTheme
import com.arnyminerz.androidmatic.widget.TintImage
import org.json.JSONObject
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Locale

class WeatherWidget : GlanceAppWidget() {
    companion object {
        fun preferencesKey(glanceId: GlanceId) = stringPreferencesKey("widget-$glanceId")
    }

    private val backgroundColor: ColorProvider
        @Composable
        get() = androidx.glance.appwidget.unit.ColorProvider(
            day = GlanceTheme.lightColors.background.getColor(LocalContext.current),
            night = GlanceTheme.darkColors.background.getColor(LocalContext.current),
        )

    private val foregroundColor: ColorProvider
        @Composable
        get() = androidx.glance.appwidget.unit.ColorProvider(
            day = GlanceTheme.lightColors.onBackground.getColor(LocalContext.current),
            night = GlanceTheme.darkColors.onBackground.getColor(LocalContext.current),
        )

    private val accentColor: ColorProvider
        @Composable
        get() = androidx.glance.appwidget.unit.ColorProvider(
            day = GlanceTheme.lightColors.primary.getColor(LocalContext.current),
            night = GlanceTheme.darkColors.primary.getColor(LocalContext.current),
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

    override val sizeMode: SizeMode = SizeMode.Exact

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
        val size = LocalSize.current

        val prefs = currentState<Preferences>()
        val data = prefs[preferencesKey(glanceId)]?.let {
            WeatherWidgetData.fromJson(JSONObject(it))
        }!!

        // val station = data.station
        val weather = data.weather

        Row(
            modifier = GlanceModifier
                .fillMaxSize()
                .appWidgetBackground()
                .background(backgroundColor)
                .cornerRadius(24.dp)
                .clickable(actionStartActivity(MainActivity::class.java)),
        ) {
            Column(
                modifier = GlanceModifier
                    .fillMaxHeight()
                    .padding(start = 8.dp, top = 8.dp),
            ) {
                TintImage(
                    resource = when (weather.literalState) {
                        "sun" -> R.drawable.ic_weather_sunny
                        "moon" -> R.drawable.ic_weather_moon
                        "fog", "hazesun", "hazemoon" -> R.drawable.ic_weather_foggy
                        "rain", "mist" -> R.drawable.ic_weather_rainy
                        else -> R.drawable.ic_weather_sunny
                    },
                    tintColor = accentColor,
                    contentDescription = context.getString(R.string.widget_weather_image_content_description),
                    modifier = GlanceModifier
                        .size(56.dp)
                        .clickable(actionRunCallback<RefreshAction>()),
                )

                Spacer(
                    modifier = GlanceModifier
                        .defaultWeight(),
                )

                Text(
                    SimpleDateFormat("HH:mm", Locale.getDefault()).format(weather.timestamp),
                    modifier = GlanceModifier
                        .padding(start = 4.dp, bottom = 4.dp),
                    style = TextStyle(
                        color = foregroundColor,
                        textAlign = TextAlign.Start,
                        fontSize = 12.sp,
                    ),
                )
            }
            Column(
                modifier = GlanceModifier
                    .defaultWeight()
                    .fillMaxHeight()
                    .padding(8.dp),
            ) {
                Text(
                    weather.stationName,
                    style = TextStyle(
                        color = foregroundColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                    ),
                    modifier = GlanceModifier
                        .fillMaxWidth(),
                )
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth(),
                ) {
                    Column(
                        modifier = GlanceModifier
                            .defaultWeight(),
                    ) {
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
                    }
                    if (size.width > 225.dp)
                        Column(
                            modifier = GlanceModifier
                                .defaultWeight(),
                        ) {
                            WeatherRow(
                                "${weather.humidityValues.value}%",
                                R.string.image_desc_humidity,
                                R.drawable.ic_humidity_48dp,
                            )
                            val windDirection =
                                context.resources.getStringArray(R.array.wind_directions)
                            val windDirectionIndex = weather.windDirectionLocalizedIndex()
                            WeatherRow(
                                "${weather.windSpeedValues.value}km/h (${windDirection[windDirectionIndex]})",
                                R.string.image_desc_wind,
                                R.drawable.ic_wind_48dp,
                            )
                        }
                }
            }
        }
    }
}
