package com.arnyminerz.androidmatic.widget.weather_widget

import android.content.Context
import android.os.Bundle
import androidx.annotation.WorkerThread
import androidx.glance.GlanceId
import androidx.glance.appwidget.state.updateAppWidgetState
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import org.json.JSONObject
import timber.log.Timber

@WorkerThread
suspend fun updateWeatherWidget(context: Context, glanceId: GlanceId) {
    Timber.i("Updating widget $glanceId...")
    updateAppWidgetState(context, glanceId) { preferences ->
        val prefKey = WeatherWidget.preferencesKey(glanceId)
        val data = preferences[prefKey]?.let {
            WeatherWidgetData.fromJson(JSONObject(it))
        }!!
        val weather = data.station.fetchWeather(context)
        preferences[prefKey] = data.copy(weather = weather).toJson().toString()

        Firebase.analytics.logEvent(
            "widget_update",
            Bundle().apply {
                putString("station", data.station.uid)
            },
        )
    }
    WeatherWidget().update(context, glanceId)
}
