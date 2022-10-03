package com.arnyminerz.androidmatic.worker

import android.content.Context
import androidx.annotation.WorkerThread
import androidx.datastore.preferences.core.edit
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.await
import com.arnyminerz.androidmatic.storage.Keys
import com.arnyminerz.androidmatic.storage.dataStore
import com.arnyminerz.androidmatic.widget.weather_widget.WeatherWidget
import com.arnyminerz.androidmatic.widget.weather_widget.updateWeatherWidget
import timber.log.Timber
import java.util.concurrent.TimeUnit

class UpdateDataWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {
    companion object {
        fun get(context: Context) =
            WorkManager
                .getInstance(context)
                .getWorkInfosByTagLiveData(UpdateDataOptions.WorkerTag)

        @WorkerThread
        suspend fun scheduleIfNotRunning(context: Context) {
            WorkManager.getInstance(context)
                .takeIf { it.getWorkInfosByTag(UpdateDataOptions.WorkerTag).await().isEmpty() }
                ?.also { Timber.i("Scheduling weather update worker...") }
                ?.enqueue(
                    PeriodicWorkRequestBuilder<UpdateDataWorker>(
                        UpdateDataOptions.RepeatInterval,
                        TimeUnit.MINUTES,
                        UpdateDataOptions.FlexTimeInterval,
                        TimeUnit.MINUTES
                    )
                        .setConstraints(
                            Constraints.Builder()
                                .setRequiredNetworkType(NetworkType.CONNECTED)
                                .build()
                        )
                        .addTag(UpdateDataOptions.WorkerTag)
                        .build()
                )
                ?: run {
                    Timber.i("Won't schedule worker since already running.")
                }
        }
    }

    override suspend fun doWork(): Result {
        GlanceAppWidgetManager(applicationContext)
            .getGlanceIds(WeatherWidget::class.java)
            .forEach { updateWeatherWidget(applicationContext, it) }

        applicationContext.dataStore.edit {
            it[Keys.LAST_WORKER_RUN] = System.currentTimeMillis()
        }

        Timber.i("Ran weather update")

        return Result.success()
    }
}

/**
 * Stores some options about how the [UpdateDataWorker] should be scheduled.
 * @author Arnau Mora
 * @since 20221003
 */
object UpdateDataOptions {
    /**
     * Defines how often (in minutes) the worker shall be executed.
     * @author Arnau Mora
     * @since 20221003
     */
    const val RepeatInterval: Long = 15

    /**
     * Defines the time flex interval for the scheduled worker.
     * @author Arnau Mora
     * @since 20221003
     */
    const val FlexTimeInterval: Long = 5

    /**
     * Defines the tag to use for identifying the worker in the WorkManager.
     * @author Arnau Mora
     * @since 20221003
     */
    const val WorkerTag = "weather_updater"
}
