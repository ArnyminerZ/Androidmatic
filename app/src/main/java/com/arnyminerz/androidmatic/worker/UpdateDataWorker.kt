package com.arnyminerz.androidmatic.worker

import android.content.Context
import android.os.Bundle
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.arnyminerz.androidmatic.widget.weather_widget.WeatherWidget
import com.arnyminerz.androidmatic.widget.weather_widget.WeatherWidgetData
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import org.json.JSONObject
import timber.log.Timber

class UpdateDataWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        GlanceAppWidgetManager(applicationContext)
            .getGlanceIds(WeatherWidget::class.java)
            .forEach { glanceId ->
                Timber.i("Updating widget $glanceId...")
                updateAppWidgetState(applicationContext, glanceId) { preferences ->
                    val prefKey = WeatherWidget.preferencesKey(glanceId)
                    val data = preferences[prefKey]?.let {
                        WeatherWidgetData.fromJson(JSONObject(it))
                    }!!
                    val weather = data.station.fetchWeather(applicationContext)
                    preferences[prefKey] = data.copy(weather = weather).toJson().toString()
                    Firebase.analytics.logEvent(
                        "widget_update",
                        Bundle().apply {
                            putString("station", data.station.uid)
                        },
                    )
                }
            }

        Timber.i("Ran weather update")

        return Result.success()
    }
}