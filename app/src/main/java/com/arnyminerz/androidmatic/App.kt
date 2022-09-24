package com.arnyminerz.androidmatic

import android.app.Application
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.await
import com.arnyminerz.androidmatic.utils.doAsync
import com.arnyminerz.androidmatic.worker.UpdateDataWorker
import timber.log.Timber
import java.util.concurrent.TimeUnit

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        Timber.plant(Timber.DebugTree())

        doAsync {
            WorkManager.getInstance(this@App)
                .takeIf { it.getWorkInfosByTag("weather_updater").await().isEmpty() }
                ?.also { Timber.i("Scheduling weather update worker...") }
                ?.enqueue(
                    PeriodicWorkRequestBuilder<UpdateDataWorker>(
                        15,
                        TimeUnit.MINUTES,
                        5,
                        TimeUnit.MINUTES
                    )
                        .setConstraints(
                            Constraints.Builder()
                                .setRequiredNetworkType(NetworkType.CONNECTED)
                                .build()
                        )
                        .addTag("weather_updater")
                        .build()
                )
                ?: run {
                    Timber.i("Won't schedule worker since already running.")
                }
        }
    }
}