package com.arnyminerz.androidmatic.worker

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.arnyminerz.androidmatic.widget.weather_widget.WeatherWidget
import com.arnyminerz.androidmatic.widget.weather_widget.updateWeatherWidget
import timber.log.Timber

class UpdateDataWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        GlanceAppWidgetManager(applicationContext)
            .getGlanceIds(WeatherWidget::class.java)
            .forEach { updateWeatherWidget(applicationContext, it) }

        Timber.i("Ran weather update")

        return Result.success()
    }
}