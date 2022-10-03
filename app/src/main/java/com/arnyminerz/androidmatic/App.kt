package com.arnyminerz.androidmatic

import android.app.Application
import com.arnyminerz.androidmatic.data.providers.MeteoclimaticProvider
import com.arnyminerz.androidmatic.data.providers.WeewxTemplateProvider
import com.arnyminerz.androidmatic.data.providers.model.WeatherProvider
import com.arnyminerz.androidmatic.utils.doAsync
import com.arnyminerz.androidmatic.worker.UpdateDataWorker.Companion.scheduleIfNotRunning
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.DynamicColorsOptions
import timber.log.Timber

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        Timber.plant(Timber.DebugTree())

        Timber.i("Adding to providers registry: ${MeteoclimaticProvider::class}")
        WeatherProvider.register(MeteoclimaticProvider::class)
        Timber.i("Adding to providers registry: ${WeewxTemplateProvider::class}")
        WeatherProvider.register(WeewxTemplateProvider::class)

        Timber.i("Applying dynamic colors")
        DynamicColors.applyToActivitiesIfAvailable(
            this,
            DynamicColorsOptions.Builder()
                .setThemeOverlay(R.style.AndroidmaticTheme)
                .build(),
        )

        doAsync { scheduleIfNotRunning(applicationContext) }
    }
}