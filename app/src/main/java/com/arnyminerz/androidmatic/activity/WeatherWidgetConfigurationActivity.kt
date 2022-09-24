package com.arnyminerz.androidmatic.activity

import android.appwidget.AppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.arnyminerz.androidmatic.data.Station
import com.arnyminerz.androidmatic.utils.doAsync
import com.arnyminerz.androidmatic.widget.weather_widget.WeatherWidget
import com.arnyminerz.androidmatic.widget.weather_widget.WeatherWidgetData
import timber.log.Timber

class WeatherWidgetConfigurationActivity : AddStationActivity(
    single = true,
    onClosed = {
        setResult(RESULT_CANCELED)
        finish()
    },
) {
    override fun onSelectStation(station: Station) {
        Timber.i("Selected $station.")

        val context = this

        val appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        val glanceAppWidgetManager = GlanceAppWidgetManager(this)
        doAsync {
            val glanceId = glanceAppWidgetManager.getGlanceIdBy(appWidgetId)

            val weather = station.fetchWeather(context)

            updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                prefs.toMutablePreferences().apply {
                    this[WeatherWidget.preferencesKey(glanceId)] =
                        WeatherWidgetData(station, weather).toString()
                }
            }

            WeatherWidget().update(context, glanceId)
        }.invokeOnCompletion {
            setResult(RESULT_OK)
            finish()
        }
    }
}